package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final CartService cartService;

    private Long getCurrentUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.setAttribute("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public String items(@RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "NO") String sort,
                        @RequestParam(defaultValue = "1") int pageNumber,
                        @RequestParam(defaultValue = "5") int pageSize,
                        Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        model.addAttribute("items", itemService.getPagedItems(search, sort, pageNumber, pageSize, userId));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", itemService.getPaging(search, sort, pageNumber, pageSize));
        model.addAttribute("totalCart", cartService.getTotal(userId));
        return "items";
    }

    @PostMapping
    public String updateFromList(@RequestParam Long id,
                                 @RequestParam String action,
                                 @RequestParam Map<String, String> allParams,
                                 HttpSession session) {
        Long userId = getCurrentUserId(session);
        cartService.updateCart(userId, id, ActionDto.valueOf(action));

        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!"id".equals(entry.getKey()) && !"action".equals(entry.getKey())) {
                if (params.length() > 0) params.append("&");
                params.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return "redirect:/items?" + params;
    }

    @GetMapping("/{id}")
    public String item(@PathVariable Long id, Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        model.addAttribute("item", itemService.getItemDto(id, userId));
        model.addAttribute("totalCart", cartService.getTotal(userId));
        return "item";
    }

    @PostMapping("/{id}")
    public String updateFromItem(@PathVariable Long id,
                                 @RequestParam String action,
                                 Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        cartService.updateCart(userId, id, ActionDto.valueOf(action));
        model.addAttribute("item", itemService.getItemDto(id, userId));
        model.addAttribute("totalCart", cartService.getTotal(userId));
        return "item";
    }
}