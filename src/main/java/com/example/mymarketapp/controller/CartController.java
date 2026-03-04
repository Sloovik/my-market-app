package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    private Long getCurrentUserId(WebSession session) {
        Long userId = session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.getAttributes().put("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public Mono<String> cart(Model model, WebSession session) {
        Long userId = getCurrentUserId(session);

        return Mono.zip(
                        itemService.getCartItems(userId).collectList(),
                        cartService.getTotal(userId)
                )
                .map(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("total", tuple.getT2());
                    return "cart";
                });
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromCart(ServerWebExchange exchange, Model model, WebSession session) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String idStr = formData.getFirst("id");
                    String action = formData.getFirst("action");
                    if (idStr == null || action == null) {
                        return Mono.error(new IllegalArgumentException("Missing id or action"));
                    }
                    Long id = Long.valueOf(idStr);
                    Long userId = getCurrentUserId(session);
                    return cartService.updateCart(userId, id, ActionDto.valueOf(action))
                            .then(cart(model, session));
                });
    }
}