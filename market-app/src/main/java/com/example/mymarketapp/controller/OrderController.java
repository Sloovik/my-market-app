package com.example.mymarketapp.controller;

import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.model.OrderItemDto;
import com.example.mymarketapp.repository.UserRepository;
import com.example.mymarketapp.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    private Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(Authentication::getPrincipal)
                .ofType(UserDetails.class)
                .flatMap(userDetails ->
                        userRepository.findByUsername(userDetails.getUsername())
                                .map(user -> user.getId())
                )
                .switchIfEmpty(Mono.error(
                        new SecurityException("User not authenticated")
                ));
    }

    @GetMapping
    public Mono<String> orders(Model model) {
        return getCurrentUserId()
                .flatMapMany(userId -> orderService.getAllOrdersForUser(userId))
                .flatMap(this::mapToOrderDto)
                .collectList()
                .map(orders -> {
                    model.addAttribute("orders", orders);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> order(@PathVariable Long id,
                              @RequestParam(defaultValue = "false") boolean newOrder,
                              Model model) {
        return getCurrentUserId()
                .flatMap(userId -> orderService.getOrder(id)
                        .flatMap(order -> {
                            if (!order.getUserId().equals(userId)) {
                                return Mono.error(new AccessDeniedException("Access denied"));
                            }
                            return Mono.just(order);
                        })
                )
                .flatMap(this::mapToOrderDto)
                .map(orderDto -> {
                    model.addAttribute("order", orderDto);
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }

    @PostMapping("/buy")
    public Mono<String> buy(Model model) {
        return getCurrentUserId()
                .flatMap(userId -> orderService.createOrder(userId))
                .map(order -> "redirect:/orders/" + order.getId() + "?newOrder=true")
                .onErrorResume(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Ошибка при оплате заказа";

                    model.addAttribute("error", message);

                    if (message.contains("Недостаточно средств")) {
                        return Mono.just("redirect:/cart?error=insufficient_funds");
                    }

                    return Mono.just("redirect:/cart?error=payment_failed");
                });
    }

    private Mono<Map<String, Object>> mapToOrderDto(Order order) {
        Flux<OrderItemDto> itemsFlux = orderService.getOrderItems(order.getId())
                .map(oi -> new OrderItemDto(
                        oi.getItemId(),
                        oi.getTitle(),
                        oi.getPrice(),
                        oi.getCount()
                ));

        return itemsFlux.collectList()
                .map(items -> Map.of(
                        "id", order.getId(),
                        "items", items,
                        "totalSum", order.getTotalSum()
                ));
    }
}