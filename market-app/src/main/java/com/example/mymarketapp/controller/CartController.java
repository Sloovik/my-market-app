package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.repository.UserRepository;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import com.example.mymarketapp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    private Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .ofType(UserDetails.class)
                .flatMap(userDetails ->
                        userRepository.findByUsername(userDetails.getUsername())
                                .map(user -> user.getId())
                );
    }

    @GetMapping
    public Mono<String> cart(Model model,
                             @RequestParam(required = false) String error) {
        return getCurrentUserId()
                .flatMap(userId ->
                        Mono.zip(
                                        itemService.getCartItems(userId).collectList(),
                                        cartService.getTotal(userId),
                                        paymentService.getBalance().defaultIfEmpty(0L)
                                )
                                .map(tuple -> {
                                    model.addAttribute("items", tuple.getT1());
                                    model.addAttribute("total", tuple.getT2());

                                    Long balance = tuple.getT3();
                                    Long totalCart = tuple.getT2();

                                    boolean canCheckout = balance > 0 && balance >= totalCart;
                                    model.addAttribute("canCheckout", canCheckout);
                                    model.addAttribute("balance", balance);

                                    if (balance == 0) {
                                        model.addAttribute("paymentMessage",
                                                "Сервис платежей недоступен. Попробуйте позже.");
                                    } else if (balance < totalCart) {
                                        model.addAttribute("paymentMessage",
                                                "Недостаточно средств на счёте. Баланс: " + balance + " ₽");
                                    }

                                    if (error != null) {
                                        switch (error) {
                                            case "insufficient_funds" ->
                                                    model.addAttribute("errorMessage",
                                                            "Недостаточно средств на счёте");
                                            case "payment_failed" ->
                                                    model.addAttribute("errorMessage",
                                                            "Ошибка при оплате заказа");
                                            default ->
                                                    model.addAttribute("errorMessage",
                                                            "Произошла ошибка при оформлении заказа");
                                        }
                                    }

                                    return "cart";
                                })
                );
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateFromCart(ServerWebExchange exchange, Model model) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String idStr = formData.getFirst("id");
                    String action = formData.getFirst("action");
                    if (idStr == null || action == null) {
                        return Mono.error(new IllegalArgumentException("Missing id or action"));
                    }
                    Long id = Long.valueOf(idStr);

                    return getCurrentUserId()
                            .flatMap(userId ->
                                    cartService.updateCart(userId, id, ActionDto.valueOf(action))
                                            .then(Mono.just("redirect:/cart"))
                            );
                });
    }
}