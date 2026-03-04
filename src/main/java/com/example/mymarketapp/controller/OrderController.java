package com.example.mymarketapp.controller;

import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.model.OrderItemDto;
import com.example.mymarketapp.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long getCurrentUserId(WebSession session) {
        Long userId = session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.getAttributes().put("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public Mono<String> orders(Model model) {
        return orderService.getAllOrders()
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
        return orderService.getOrder(id)
                .flatMap(this::mapToOrderDto)
                .map(orderDto -> {
                    model.addAttribute("order", orderDto);
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }

    @PostMapping("/buy")
    public Mono<String> buy(WebSession session) {
        Long userId = getCurrentUserId(session);
        return orderService.createOrder(userId)
                .map(order -> "redirect:/orders/" + order.getId() + "?newOrder=true");
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