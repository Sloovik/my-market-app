package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final ItemService itemService;

    private Long getCurrentUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.setAttribute("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public String cart(Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        model.addAttribute("items", itemService.getCartItems(userId));
        model.addAttribute("total", cartService.getTotal(userId));
        return "cart";
    }

    @PostMapping("/items")
    public String updateFromCart(@RequestParam Long id,
                                 @RequestParam String action,
                                 Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        cartService.updateCart(userId, id, ActionDto.valueOf(action));
        return cart(model, session);
    }
}