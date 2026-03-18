package com.example.paymentservice.service;

import com.example.paymentservice.model.PaymentResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${payment.initial.balance:200000}")
    private long initialBalance;

    private final AtomicLong balance = new AtomicLong();

    public PaymentServiceImpl() {
    }

    @PostConstruct
    public void init() {
        balance.set(initialBalance);
        log.info("Payment service initialized with balance: {}", initialBalance);
    }

    @Override
    public Mono<Long> getBalance() {
        long currentBalance = balance.get();
        log.debug("Getting balance: {}", currentBalance);
        return Mono.just(currentBalance);
    }

    @Override
    public Mono<PaymentResult> processPayment(Long orderId, Long amount, String description) {
        log.info("Processing payment: orderId={}, amount={}, description={}", orderId, amount, description);

        return Mono.fromCallable(() -> {
            long currentBalance = balance.get();

            if (currentBalance < amount) {
                log.warn("Insufficient funds: balance={}, requested={}", currentBalance, amount);
                return PaymentResult.failure(
                        currentBalance,
                        "INSUFFICIENT_FUNDS",
                        "Недостаточно средств на балансе"
                );
            }

            long newBalance = balance.addAndGet(-amount);
            String transactionId = "txn-" + UUID.randomUUID().toString();

            log.info("Payment successful: orderId={}, transactionId={}, remainingBalance={}",
                    orderId, transactionId, newBalance);

            return PaymentResult.success(
                    newBalance,
                    transactionId,
                    "Платеж выполнен успешно"
            );
        });
    }

    @Override
    public Mono<Void> resetBalance() {
        log.info("Resetting balance to initial value: {}", initialBalance);
        balance.set(initialBalance);
        return Mono.empty();
    }
}