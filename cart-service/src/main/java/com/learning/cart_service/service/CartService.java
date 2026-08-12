package com.learning.cart_service.service;

import com.learning.cart_service.client.ProductClient;
import com.learning.cart_service.client.dto.ProductInfo;
import com.learning.cart_service.dto.request.AddItemRequest;
import com.learning.cart_service.dto.response.CartItemResponse;
import com.learning.cart_service.dto.response.CartResponse;
import com.learning.cart_service.entity.Cart;
import com.learning.cart_service.entity.CartItem;
import com.learning.cart_service.exception.ResourceNotFoundException;
import com.learning.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest request){
        Cart cart = getOrCreateCart(userId);

        /**
         * FAIL-FAST. This call is intentionally NOT wrapped in a
         * try/catch here. If Catalog Service can't confirm this product
         * exists, is active, and has enough stock, we let the exception
         * propagate straight to GlobalExceptionHandler and reject the
         * add-to-cart outright (a 404 if the product genuinely doesn't
         * exist, a 503 if Catalog Service is unreachable). Silently
         * accepting an unverifiable product would let a customer "add"
         * something that turns out to be nonexistent or out of stock —
         * a worse failure, discovered later at checkout, instead of a
         * clean one right now.
         */

        ProductInfo product = productClient.getProduct(request.getProductId());

        if(!product.isActive()){
            throw new IllegalArgumentException("This product is no longer available");
        }

        if(product.getStockQuantity() < request.getQuantity()){
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }

        CartItem existingItem = cart.findItemByProductId(request.getProductId());
        if(existingItem != null){
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setPriceSnapshot(product.getPrice()); // refresh the snapshot on any write touching this item
        } else {
            CartItem newItem = new CartItem(request.getProductId(),request.getQuantity(),product.getPrice());
            cart.addItem(newItem);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = getCartOrThrow(userId);
        CartItem item = findItemOrThrow(cart, itemId);

        if (quantity == null || quantity <= 0) {
            cart.removeItem(item);
        } else {
            // Still fail-fast: changing quantity is still a write that
            // should reconfirm current stock, same reasoning as addItem.
            ProductInfo product = productClient.getProduct(item.getProductId());
            if (product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
            item.setQuantity(quantity);
            item.setPriceSnapshot(product.getPrice());
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getCartOrThrow(userId);
        CartItem item = findItemOrThrow(cart, itemId);
        cart.removeItem(item); // orphanRemoval = true (Phase F, File 2) means this actually deletes the row on flush
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartOrThrow(userId);
        // Copy into a new list first — removeItem() below mutates
        // cart.getItems() as it goes (orphanRemoval=true), so iterating
        // the live collection directly would throw ConcurrentModificationException.
        List<CartItem> itemsToRemove = List.copyOf(cart.getItems());
        itemsToRemove.forEach(cart::removeItem);
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        // Auto-creating an empty cart on first view is a deliberate UX
        // choice — "view my cart" should never 404 just because someone
        // hasn't added anything yet.
        return cartRepository.findByUserId(userId)
                .orElseGet(()-> cartRepository.save(new Cart(userId)));
    }


    private Cart getCartOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No cart found for this user"));
    }

    private CartItem findItemOrThrow(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse).toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .cartTotal(total)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        /**
         * GRACEFUL DEGRADATION. Unlike addItem/updateItemQuantity above,
         * simply VIEWING a cart should never fail outright just because
         * Catalog Service is slow, temporarily down, or because one
         * product in an otherwise-fine cart was deleted after being
         * added. We already have everything we NEED for a usable cart —
         * productId, quantity, and the frozen priceSnapshot — straight
         * from our own database, no network call required. The
         * product's current display NAME is a nice-to-have, fetched
         * live, and we degrade to a placeholder rather than let its
         * failure take down the whole response. This is the concrete
         * payoff of distinguishing "must succeed" writes from "should
         * degrade" reads, catching what addItem deliberately does not.
         */
        String productName;
        try{
            productName = productClient.getProduct(item.getProductId()).getName();
        } catch(RuntimeException ex){
            productName = "Unavailable";
        }
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(productName)
                .quantity(item.getQuantity())
                .priceSnapshot(item.getPriceSnapshot())
                .subtotal(item.getSubtotal())
                .build();

    }
}
