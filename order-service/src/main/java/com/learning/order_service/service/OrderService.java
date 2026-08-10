package com.learning.order_service.service;

import com.learning.order_service.client.CartServiceClient;
import com.learning.order_service.client.CatalogServiceClient;
import com.learning.order_service.client.dto.CartInfo;
import com.learning.order_service.client.dto.CartItemInfo;
import com.learning.order_service.client.dto.ProductInfo;
import com.learning.order_service.client.dto.request.BulkStockDecrementRequest;
import com.learning.order_service.client.dto.request.CheckoutRequest;
import com.learning.order_service.client.dto.request.ShippingAddressRequest;
import com.learning.order_service.client.dto.request.StockDecrementItem;
import com.learning.order_service.client.dto.response.OrderResponse;
import com.learning.order_service.entity.Order;
import com.learning.order_service.entity.OrderItem;
import com.learning.order_service.entity.ShippingAddress;
import com.learning.order_service.event.OrderCreatedEvent;
import com.learning.order_service.event.OrderItemInfo;
import com.learning.order_service.exception.CartServiceUnavailableException;
import com.learning.order_service.exception.CatalogServiceUnavailableException;
import com.learning.order_service.exception.EmptyCartException;
import com.learning.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final CatalogServiceClient catalogServiceClient;
    private final CircuitBreakerFactory<?,?> circuitBreakerFactory;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request){

        // STEP 1 — fetch the cart. FAIL-FAST: checkout is meaningless
        // without knowing what's being purchased.
        CartInfo cart = circuitBreakerFactory.create("cartService").run(
                () -> cartServiceClient.getCart(userId).getData(),
                throwable -> {
                    throw new CartServiceUnavailableException("Unable to reach Cart Service: "+throwable.getMessage());
                }
        );

        if(cart.getItems() == null || cart.getItems().isEmpty()){
            throw new EmptyCartException("Cannot checkout an empty cart");
        }

        // STEP 2 — re-verify EVERY item against Catalog Service's
        // CURRENT data. This is the "trust but verify at the actual
        // purchase moment" principle flagged back in File 1 — we do NOT
        // trust Cart's priceSnapshot for the actual charge.
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(this::buildOrderItem)
                .collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream().map(OrderItem::getSubTotal).
                reduce(BigDecimal.ZERO,BigDecimal::add);

        // STEP 3 — decrement stock for ALL items in one atomic call.
        // FAIL-FAST: if this throws (insufficient stock, OR Catalog
        // Service unreachable), checkout aborts entirely — nothing has
        // been persisted yet in THIS service either, since we're still
        // inside the @Transactional method and haven't saved the Order.
        List<StockDecrementItem> decrementItems = orderItems.stream().map(oi-> {
            StockDecrementItem item = new StockDecrementItem();
            item.setProductId(oi.getProductId());
            item.setQuantity(oi.getQuantity());
            return item;
        }).toList();

        BulkStockDecrementRequest decrementRequest = new BulkStockDecrementRequest();
        decrementRequest.setItems(decrementItems);

        circuitBreakerFactory.create("catalogService").run(
                () -> catalogServiceClient.decrementStockBulk(decrementRequest), throwable -> {
                    throw new CatalogServiceUnavailableException("Unable to reserve stock: "+throwable.getMessage());
                }
        );

        // STEP 4 — persist the order. If this fails after stock was
        // already decremented, we have a genuine inconsistency (stock
        // reserved, no order to show for it) — flagged honestly below,
        // not glossed over.
        Order order = new Order(userId, toShippingAddress(request.getShippingAddress()), totalAmount);
        orderItems.forEach(order::addItem);
        Order saved = orderRepository.save(order);

        // STEP 5 — best-effort cleanup. Checkout has ALREADY succeeded
        // at this point (the order is saved) — a failure clearing the
        // cart is annoying, not catastrophic, so we log and move on
        // rather than failing the whole checkout over it. Contrast this
        // explicitly with Steps 1-3, which were all fail-fast.
        try{
            cartServiceClient.clearCart(userId);
        } catch (Exception ex){
            log.warn("Order {} created successfully, but failed to clear cart for user {}:{}",saved.getId(),userId,ex.getMessage());
        }

        // STEP 6 — publish the event that drives the payment saga.
        publishOrderCreatedEvent(saved, orderItems);

        return OrderResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .totalAmount(saved.getTotalAmount())
                .createdDate(saved.getCreatedDate())
                .build();
    }

    private OrderItem buildOrderItem(CartItemInfo cartItem) {
        ProductInfo product = circuitBreakerFactory.create("catalogService").run(
                () -> catalogServiceClient.getProductById(cartItem.getProductId()).getData(),throwable -> {
                    throw new CatalogServiceUnavailableException("Unable to verify product "+cartItem.getProductId());
                }
        );

        if(!product.isActive()){
            throw new IllegalStateException("Product '" + product.getName() + "' is no longer available");
        }

        // priceAtPurchase = product.getPrice() — CURRENT price from
        // Catalog Service right now, deliberately NOT whatever price
        // was in the cart. This is the concrete moment where "checkout
        // re-verifies, cart just displays" becomes real code.
        return new OrderItem(product.getId(),product.getName(),cartItem.getQuantity(),product.getPrice());
    }

    private ShippingAddress toShippingAddress(ShippingAddressRequest req) {
        return new ShippingAddress(req.getStreet(), req.getCity(),req.getState(),req.getZipCode(),req.getCountry());
    }

    private void publishOrderCreatedEvent(Order order, List<OrderItem> items) {
        List<OrderItemInfo> eventItems = items.stream()
                .map(oi -> new OrderItemInfo(oi.getProductId(),oi.getQuantity(),oi.getPriceAtPurchase())).toList();

        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID().toString(),
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                eventItems, Instant.now());

        /**
         * ⚠️ THE DUAL-WRITE PROBLEM — flagging this honestly rather than
         * hiding it. We just committed the Order to Postgres (Step 4)
         * and are NOW, as a SEPARATE operation, publishing to Kafka.
         * These are two independent systems with no shared transaction
         * — if this service crashed in the gap between them, we'd have
         * an order permanently stuck in PENDING with no saga ever
         * triggered to resolve it. This is a genuinely well-known
         * distributed systems problem with a well-known production
         * answer: the OUTBOX PATTERN — write the event into an "outbox"
         * table in the SAME LOCAL TRANSACTION as the Order (Postgres
         * guarantees that atomicity, since it's one database), then a
         * separate, independent process (a scheduled poller, or
         * Debezium via Change Data Capture) reads unpublished outbox
         * rows and relays them to Kafka, retrying until Kafka
         * confirms — guaranteeing the event eventually gets published
         * if and only if the order was actually committed. We're NOT
         * building the outbox pattern in this project (real scope
         * creep for a revision project), but you should be able to
         * name it and explain why it's needed the moment "how do you
         * keep a database write and a Kafka publish consistent"
         * comes up in an interview — which it will.
         */
        kafkaTemplate.send("order.created",order.getId().toString(),event)
                .whenComplete((result,ex) -> {
                    if(ex != null){
                        log.error("Failed to publish OrderCreatedEvent for order {}: {}",
                                order.getId(), ex.getMessage());
                    } else{
                        log.info("Published OrderCreatedEvent for order {}", order.getId());
                    }
                });
    }

}
