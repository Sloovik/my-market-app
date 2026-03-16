package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    private Long getCurrentUserId(WebSession session) {
        Long userId = session.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            session.getAttributes().put("userId", userId);
        }
        return userId;
    }

    @GetMapping
    public Mono<String> items(@RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "NO") String sort,
                              @RequestParam(defaultValue = "1") int pageNumber,
                              @RequestParam(defaultValue = "5") int pageSize,
                              Model model,
                              WebSession session) {
        Long userId = getCurrentUserId(session);

        Mono<List<List<ItemDto>>> itemsMono =
                itemService.getPagedItems(search, sort, pageNumber, pageSize, userId);
        Mono<PagingDto> pagingMono = itemService.getPaging(search, sort, pageNumber, pageSize);
        Mono<Long> totalCartMono = cartService.getTotal(userId);

        return Mono.zip(itemsMono, pagingMono, totalCartMono)
                .map(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    model.addAttribute("paging", tuple.getT2());
                    model.addAttribute("totalCart", tuple.getT3());
                    return "items";
                });
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromList(ServerWebExchange exchange, WebSession session) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String idStr = formData.getFirst("id");
                    String action = formData.getFirst("action");
                    if (idStr == null || action == null) {
                        return Mono.error(new IllegalArgumentException("Missing id or action parameters"));
                    }
                    Long id = Long.valueOf(idStr);
                    Long userId = getCurrentUserId(session);

                    StringBuilder params = new StringBuilder();
                    formData.forEach((name, values) -> {
                        if (!"id".equals(name) && !"action".equals(name) && !values.isEmpty()) {
                            if (params.length() > 0) params.append("&");
                            params.append(name).append("=").append(values.get(0));
                        }
                    });

                    return cartService.updateCart(userId, id, ActionDto.valueOf(action))
                            .thenReturn("redirect:/items?" + params);
                });
    }

    @GetMapping("/{id}")
    public Mono<String> item(@PathVariable Long id, Model model, WebSession session) {
        Long userId = getCurrentUserId(session);

        return Mono.zip(
                        itemService.getItemDto(id, userId),
                        cartService.getTotal(userId)
                )
                .map(tuple -> {
                    model.addAttribute("item", tuple.getT1());
                    model.addAttribute("totalCart", tuple.getT2());
                    return "item";
                });
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromItem(ServerWebExchange exchange, @PathVariable Long id, Model model, WebSession session) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    if (action == null) {
                        return Mono.error(new IllegalArgumentException("Missing action parameter"));
                    }
                    Long userId = getCurrentUserId(session);
                    return cartService.updateCart(userId, id, ActionDto.valueOf(action))
                            .then(Mono.zip(itemService.getItemDto(id, userId), cartService.getTotal(userId)))
                            .map(tuple -> {
                                model.addAttribute("item", tuple.getT1());
                                model.addAttribute("totalCart", tuple.getT2());
                                return "item";
                            });
                });
    }
}