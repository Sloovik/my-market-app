package com.example.mymarketapp.service;

import com.example.mymarketapp.entity.CartItem;
import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.entity.OrderItem;
import com.example.mymarketapp.repository.OrderItemRepository;
import com.example.mymarketapp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    public Mono<Order> createOrder(Long userId) {
        if (userId == null || userId <= 0) {
            return Mono.error(new IllegalArgumentException("Valid userId required"));
        }

        Flux<CartItem> cartItemsFlux = cartService.getCart(userId);

        return cartItemsFlux
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }

                    return cartService.getTotal(userId)
                            .flatMap(totalSum -> {
                                return paymentService.hasEnoughBalance(totalSum)
                                        .flatMap(hasBalance -> {
                                            if (!hasBalance) {
                                                return Mono.error(new IllegalStateException(
                                                        "Недостаточно средств на счёте"));
                                            }

                                            Order order = new Order();
                                            order.setUserId(userId);
                                            order.setTotalSum(totalSum);

                                            return orderRepository.save(order)
                                                    .flatMap(savedOrder ->
                                                            paymentService.makePayment(
                                                                            savedOrder.getId(),
                                                                            totalSum
                                                                    )
                                                                    .flatMap(paymentResponse -> {
                                                                        if (!paymentResponse.getSuccess()) {
                                                                            return orderRepository
                                                                                    .delete(savedOrder)
                                                                                    .then(Mono.error(
                                                                                            new IllegalStateException(
                                                                                                    "Ошибка оплаты: " +
                                                                                                            paymentResponse.getMessage()
                                                                                            )
                                                                                    ));
                                                                        }

                                                                        log.info("Payment successful for order {}, " +
                                                                                        "remaining balance: {}",
                                                                                savedOrder.getId(),
                                                                                paymentResponse.getRemainingBalance());

                                                                        return saveOrderItems(savedOrder, cartItems)
                                                                                .then(cartService.clearCart(userId))
                                                                                .thenReturn(savedOrder);
                                                                    })
                                                                    .onErrorResume(e -> {
                                                                        log.error("Payment error, rolling back order {}",
                                                                                savedOrder.getId(), e);
                                                                        return orderRepository.delete(savedOrder)
                                                                                .then(Mono.error(e));
                                                                    })
                                                    );
                                        });
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