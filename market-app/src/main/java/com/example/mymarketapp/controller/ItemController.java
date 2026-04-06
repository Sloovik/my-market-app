package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.repository.UserRepository;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;
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
                .switchIfEmpty(Mono.empty());
    }

    @GetMapping
    public Mono<String> items(@RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "NO") String sort,
                              @RequestParam(defaultValue = "1") int pageNumber,
                              @RequestParam(defaultValue = "5") int pageSize,
                              Model model) {

        return getCurrentUserId()
                .defaultIfEmpty(-1L)
                .flatMap(userId -> {
                    Mono<List<List<ItemDto>>> itemsMono =
                            itemService.getPagedItems(search, sort, pageNumber, pageSize, userId);
                    Mono<PagingDto> pagingMono =
                            itemService.getPaging(search, sort, pageNumber, pageSize);

                    Mono<Long> totalCartMono = userId > 0
                            ? cartService.getTotal(userId)
                            : Mono.just(0L);

                    return Mono.zip(itemsMono, pagingMono, totalCartMono)
                            .map(tuple -> {
                                model.addAttribute("items", tuple.getT1());
                                model.addAttribute("search", search);
                                model.addAttribute("sort", sort);
                                model.addAttribute("paging", tuple.getT2());
                                model.addAttribute("totalCart", tuple.getT3());
                                return "items";
                            });
                });
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromList(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String idStr = formData.getFirst("id");
                    String action = formData.getFirst("action");

                    if (idStr == null || action == null) {
                        return Mono.error(new IllegalArgumentException("Missing id or action parameters"));
                    }

                    Long id = Long.valueOf(idStr);

                    return getCurrentUserId()
                            .switchIfEmpty(Mono.error(
                                    new SecurityException("Authentication required")
                            ))
                            .flatMap(userId -> {
                                StringBuilder params = new StringBuilder();
                                formData.forEach((name, values) -> {
                                    if (!("id".equals(name) || "action".equals(name)) && !values.isEmpty()) {
                                        if (params.length() > 0) params.append("&");
                                        params.append(name).append("=").append(values.get(0));
                                    }
                                });

                                return cartService.updateCart(userId, id, ActionDto.valueOf(action))
                                        .thenReturn("redirect:/items?" + params);
                            });
                });
    }

    @GetMapping("/{id}")
    public Mono<String> item(@PathVariable Long id, Model model) {
        return getCurrentUserId()
                .defaultIfEmpty(-1L)
                .flatMap(userId -> {
                    Mono<Long> totalCartMono = userId > 0
                            ? cartService.getTotal(userId)
                            : Mono.just(0L);

                    return Mono.zip(
                                    itemService.getItemDto(id, userId),
                                    totalCartMono
                            )
                            .map(tuple -> {
                                model.addAttribute("item", tuple.getT1());
                                model.addAttribute("totalCart", tuple.getT2());
                                return "item";
                            });
                });
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromItem(ServerWebExchange exchange,
                                       @PathVariable Long id,
                                       Model model) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    if (action == null) {
                        return Mono.error(new IllegalArgumentException("Missing action parameter"));
                    }

                    return getCurrentUserId()
                            .switchIfEmpty(Mono.error(
                                    new SecurityException("Authentication required")
                            ))
                            .flatMap(userId ->
                                    cartService.updateCart(userId, id, ActionDto.valueOf(action))
                                            .then(Mono.zip(
                                                    itemService.getItemDto(id, userId),
                                                    cartService.getTotal(userId)
                                            ))
                                            .map(tuple -> {
                                                model.addAttribute("item", tuple.getT1());
                                                model.addAttribute("totalCart", tuple.getT2());
                                                return "item";
                                            })
                            );
                });
    }
}