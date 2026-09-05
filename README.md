# Ecommerce Microservices Platform

A full-stack e-commerce application built as a set of independent Spring Boot microservices, coordinated through service discovery, centralized configuration, and an API gateway, with an Angular frontend and an event-driven checkout flow backed by Kafka.

---

## Architecture Overview

```
                                   ┌────────────────────┐
                                   │   Angular Frontend │
                                   └──────────┬─────────┘
                                              │
                                   ┌──────────▼──────────┐
                                   │     API Gateway     │  (:8080)
                                   └──────────┬──────────┘
              ┌─────────────┬─────────────┬──-┴───────┬─────────────┬─────────────┐
              ▼             ▼             ▼          ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Identity │  │ Catalog  │  │   Cart   │ │  Order   │ │ Payment  │ │  Review  │
        │ Service  │  │ Service  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │
        │  :8081   │  │  :8082   │  │  :8083   │ │  :8084   │ │  :8085   │ │  :8086   │
        └──────────┘  └──────────┘  └──────────┘ └────┬─────┘ └────┬─────┘ └──────────┘
                                                      │   Kafka    │
                                                      └────────────┘

        ┌──────────────────┐       ┌──────────────────┐
        │  Discovery Server│◄──────┤  Config Server   │
        │   (Eureka, :8761)│       │      (:8888)     │
        └──────────────────┘       └──────────────────┘
```

Every business service registers itself with **Discovery Server** and pulls its configuration from **Config Server** at startup. External clients only ever talk to the **API Gateway**, which handles routing, JWT validation, and rate limiting before forwarding requests to the appropriate downstream service.

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `discovery-server` | 8761 | Eureka service registry — every other service registers here and discovers peers by name |
| `config-server` | 8888 | Centralized configuration for all services, backed by a shared config repository |
| `api-gateway` | 8080 | Single entry point for all client traffic — routing, JWT validation, CORS, rate limiting |
| `identity-service` | 8081 | User registration, authentication, JWT issuance and refresh |
| `catalog-service` | 8082 | Products, categories, and tags — search, filtering, pagination |
| `cart-service` | 8083 | Per-user shopping cart, with resilient calls to Catalog Service |
| `order-service` | 8084 | Checkout orchestration, order history, and the checkout saga |
| `payment-service` | 8085 | Consumes order events and processes (simulated) payments |
| `review-service` | 8086 | Product ratings and reviews |

---

## Tech Stack

**Backend**
- Java 21, Spring Boot, Spring Cloud (Eureka, Config Server, Gateway)
- Spring Data JPA + PostgreSQL (one database per service)
- Spring Kafka — asynchronous, event-driven checkout saga between Order and Payment services
- Spring Cache + Caffeine — in-memory caching for catalog data
- Resilience4j — circuit breakers for cross-service calls
- Bucket4j — API rate limiting at the gateway
- Micrometer Tracing + Zipkin — distributed request tracing across services
- JWT (jjwt) — stateless authentication and authorization
- MapStruct, Lombok
- JUnit 5, Mockito, Testcontainers

**Frontend**
- Angular (standalone components, signal-based state management)
- TypeScript

**Infrastructure**
- Docker & Docker Compose — local orchestration of all services, PostgreSQL, Kafka, and Zipkin
- Terraform — AWS infrastructure as code (VPC, ECS Fargate, RDS, MSK, ALB, Route53/ACM, S3 + CloudFront, CI/CD via GitHub Actions). Provisioning scripts are included but **have not yet been deployed to a live AWS environment**.

---

## Project Structure

```
ecommerce-microservices/
├── discovery-server/
├── config-server/
├── identity-service/
├── catalog-service/
├── cart-service/
├── order-service/
├── payment-service/
├── review-service/
├── api-gateway/
├── ecommerce-frontend/
├── docker-compose.yml
└── infra/                 # Terraform configuration for AWS deployment
```

Each backend service follows the same internal layering: `controller` → `service` → `repository` → `entity`, with `dto`, `exception`, `config`, and `security` packages alongside.

---

## Prerequisites

- JDK 21
- Maven
- Node.js + Angular CLI
- Docker and Docker Compose
- PostgreSQL client (optional, for inspecting local databases)

---

## Getting Started — Local Development

### 1. Start shared infrastructure

```bash
docker compose up -d postgres kafka zipkin
```

### 2. Start the backend services, in order

Startup order matters on a cold start — later services expect Discovery Server and Config Server to already be reachable:

```
1. discovery-server
2. config-server
3. identity-service
4. catalog-service
5. cart-service
6. order-service
7. payment-service
8. review-service
9. api-gateway
```

Run each with:

```bash
cd <service-directory>
mvn spring-boot:run
```

Confirm every service has registered by visiting the Eureka dashboard: **http://localhost:8761**

### 3. Start the frontend

```bash
cd ecommerce-frontend
npm install
ng serve
```

The application is then available at **http://localhost:4200**, talking to the backend through the gateway at **http://localhost:8080**.

---

## Running Everything with Docker Compose

```bash
docker compose build
docker compose up
```

This builds and starts all nine backend services, PostgreSQL, Kafka, and Zipkin as a single stack, using the same host ports as local development.

---

## Key Architectural Features

- **Service discovery** — services locate each other by logical name via Eureka, never by hardcoded address.
- **Centralized configuration** — non-sensitive configuration lives in Config Server, shared across services and environments.
- **API Gateway** — the only externally-reachable entry point; owns routing, JWT validation, CORS, and rate limiting.
- **Stateless JWT authentication** — access and refresh tokens, with role-based authorization for admin-only operations.
- **Event-driven checkout saga** — Order Service publishes an event on checkout; Payment Service consumes it asynchronously via Kafka and publishes the outcome, which Order Service consumes to finalize or cancel the order. Includes idempotent consumption and dead-letter handling for failed messages.
- **Resilience** — circuit breakers around cross-service HTTP calls (e.g., Cart Service → Catalog Service), with fail-fast behavior on writes and graceful degradation on reads.
- **Optimistic locking** — concurrent stock updates on the product catalog are protected against lost updates, with automatic retry on conflict.
- **Distributed tracing** — every request is traceable end-to-end across services via a shared trace ID, visualized in Zipkin.
- **Database per service** — each service owns its own PostgreSQL database; no service queries another's data store directly.

---

## Testing

Each backend service includes a layered test suite:
- Unit tests for service-layer business logic (Mockito, no Spring context)
- Repository tests against a real PostgreSQL instance (Testcontainers)
- Controller tests for the web layer (`@WebMvcTest`)
- Integration tests exercising real infrastructure end to end, including the Kafka-based saga

Run a service's tests with:

```bash
mvn test
```

Postman collections used for manual API testing during development are available on request / in the `postman/` directory if included in this repository.

---

## Deployment

Terraform configuration for a production AWS deployment is included under `infra/`, covering:

- VPC with public/private subnet segmentation across multiple availability zones
- ECS Fargate for all nine services, with per-service IAM roles and Secrets Manager-backed credentials
- RDS PostgreSQL (Multi-AZ) and Amazon MSK for Kafka
- Application Load Balancer, Route53, and ACM for public HTTPS ingress
- S3 + CloudFront for frontend hosting
- GitHub Actions CI/CD pipelines for automated build, test, and rolling deployment

**This infrastructure has been designed and documented but not yet applied to a live AWS account.** Treat it as a deployment blueprint rather than a running environment.

---

## Monitoring & Observability

- **Zipkin** (`http://localhost:9411`) — distributed trace visualization across all services for a given request.
- **Spring Boot Actuator** — health and info endpoints exposed on every service under `/actuator`.

---

## License

This project was built for educational and portfolio purposes.
