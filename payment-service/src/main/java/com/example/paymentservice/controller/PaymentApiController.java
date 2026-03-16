package com.example.paymentservice.controller;

import com.example.paymentservice.api.ApiApi;
import com.example.paymentservice.model.BalanceResponse;
import com.example.paymentservice.model.PaymentRequest;
import com.example.paymentservice.model.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentApiController implements ApiApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        log.info("GET /api/balance - Getting balance");

        return paymentService.getBalance()
                .map(balance -> {
                    log.info("Balance retrieved: {}", balance);
                    BalanceResponse response = new BalanceResponse();
                    response.setBalance(balance);
                    response.setCurrency("RUB");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Error getting balance", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> makePayment(
            Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange) {

        log.info("POST /api/payment - Processing payment");

        return paymentRequest
                .flatMap(request -> {
                    log.info("Payment request: orderId={}, amount={}",
                            request.getOrderId(), request.getAmount());

                    return paymentService.processPayment(
                                    request.getOrderId(),
                                    request.getAmount(),
                                    request.getDescription()
                            )
                            .map(result -> {
                                PaymentResponse response = new PaymentResponse();
                                response.setSuccess(result.isSuccess());
                                response.setRemainingBalance(result.getRemainingBalance());
                                response.setTransactionId(result.getTransactionId());
                                response.setMessage(result.getMessage());

                                log.info("Payment processed: success={}, transactionId={}",
                                        result.isSuccess(), result.getTransactionId());

                                return ResponseEntity.ok(response);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error processing payment", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}