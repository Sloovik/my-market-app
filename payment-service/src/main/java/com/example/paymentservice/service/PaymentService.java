package com.example.paymentservice.service;

import com.example.paymentservice.model.PaymentResult;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Mono<Long> getBalance();
    Mono<PaymentResult> processPayment(Long orderId, Long amount, String description);
    Mono<Void> resetBalance();
}