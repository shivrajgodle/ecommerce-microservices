FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Replaced 'dependency:go-offline' with 'dependency:resolve' — this
# resolves ONLY the project's own main + test dependencies, not the
# dependency-plugin's own unrelated reporting/site-generation tree.
# Same Docker layer-caching benefit (this layer still only
# invalidates when pom.xml changes), meaningfully smaller and more
# reliable network footprint. Added explicit retry/timeout flags too,
# rather than relying on whatever Docker Desktop injects automatically.
RUN mvn dependency:resolve -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120
COPY src ./src
RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]