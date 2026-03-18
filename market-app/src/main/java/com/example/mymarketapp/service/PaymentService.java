package com.example.mymarketapp.service;

import com.example.mymarketapp.client.api.PaymentApi;
import com.example.mymarketapp.client.model.PaymentRequest;
import com.example.mymarketapp.client.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentApi paymentApi;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public Mono<Long> getBalance() {
        return paymentApi.getBalance()
                .timeout(TIMEOUT)
                .map(response -> response.getBalance())
                .doOnNext(balance -> log.info("Balance retrieved: {}", balance))
                .doOnError(e -> log.error("Error getting balance from payment service", e))
                .onErrorResume(e -> {
                    log.warn("Payment service unavailable, returning empty");
                    return Mono.empty();
                });
    }

    public Mono<PaymentResponse> makePayment(Long orderId, Long amount) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(amount);
        request.setDescription("Оплата заказа №" + orderId);

        return paymentApi.makePayment(request)
                .timeout(TIMEOUT)
                .doOnNext(response ->
                        log.info("Payment completed for order {}: success={}",
                                orderId, response.getSuccess()))
                .doOnError(e -> log.error("Payment failed for order {}", orderId, e));
    }

    public Mono<Boolean> hasEnoughBalance(Long amount) {
        return getBalance()
                .map(balance -> balance >= amount)
                .defaultIfEmpty(false);
    }
}