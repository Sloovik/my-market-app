package com.example.mymarketapp.controller;

import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.model.OrderItemDto;
import com.example.mymarketapp.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    private Long getCurrentUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.setAttribute("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public String orders(Model model) {
        List<Map<String, Object>> orders = orderService.getAllOrders().stream()
                .map(this::mapToOrderDto)
                .toList();
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/{id}")
    public String order(@PathVariable Long id,
                        @RequestParam(defaultValue = "false") boolean newOrder,
                        Model model) {
        Map<String, Object> orderDto = mapToOrderDto(orderService.getOrder(id));
        model.addAttribute("order", orderDto);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpSession session) {
        Long userId = getCurrentUserId(session);
        Order order = orderService.createOrder(userId);
        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }

    private Map<String, Object> mapToOrderDto(Order order) {
        return Map.of(
                "id", order.getId(),
                "items", order.getItems().stream()
                        .map(oi -> new OrderItemDto(oi.getItemId(), oi.getTitle(), oi.getPrice(), oi.getCount()))
                        .toList(),
                "totalSum", order.getTotalSum()
        );
    }
}