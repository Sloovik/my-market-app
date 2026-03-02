package com.example.mymarketapp.service;

import com.example.mymarketapp.entity.CartItem;
import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.entity.OrderItem;
import com.example.mymarketapp.repository.OrderItemRepository;
import com.example.mymarketapp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;

    public Mono<Order> createOrder(Long userId) {
        if (userId == null || userId <= 0) {
            return Mono.error(new IllegalArgumentException("Valid userId required"));
        }

        Flux<CartItem> cartItemsFlux = cartService.getCart(userId);

        return cartItemsFlux
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    return cartService.getTotal(userId)
                            .flatMap(totalSum -> {
                                Order order = new Order();
                                order.setUserId(userId);
                                order.setTotalSum(totalSum);
                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> saveOrderItems(savedOrder, cartItems)
                                                .then(cartService.clearCart(userId))
                                                .thenReturn(savedOrder));
                            });
                });
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> getOrder(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + id)));
    }

    public Flux<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    private Mono<Void> saveOrderItems(Order order, java.util.List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderId(order.getId());
                    orderItem.setItemId(cartItem.getItemId());
                    orderItem.setTitle(cartItem.getTitle());
                    orderItem.setPrice(cartItem.getPrice());
                    orderItem.setCount(cartItem.getCount());
                    return orderItem;
                })
                .collectList()
                .flatMapMany(orderItemRepository::saveAll)
                .then();
    }
}