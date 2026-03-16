package com.example.mymarketapp.integration;

import com.example.mymarketapp.client.api.PaymentApi;
import com.example.mymarketapp.client.model.BalanceResponse;
import com.example.mymarketapp.client.model.PaymentRequest;
import com.example.mymarketapp.client.model.PaymentResponse;
import com.example.mymarketapp.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private PaymentApi paymentApi;

    @Test
    void shouldGetBalance() {
        BalanceResponse mockResponse = new BalanceResponse();
        mockResponse.setBalance(100000L);
        mockResponse.setCurrency("RUB");

        when(paymentApi.getBalance()).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(paymentService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualTo(100000L))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenPaymentServiceUnavailable() {
        when(paymentApi.getBalance()).thenReturn(Mono.error(new RuntimeException("Service down")));

        StepVerifier.create(paymentService.getBalance())
                .verifyComplete();
    }

    @Test
    void shouldMakeSuccessfulPayment() {
        PaymentResponse mockResponse = new PaymentResponse();
        mockResponse.setSuccess(true);
        mockResponse.setRemainingBalance(85000L);
        mockResponse.setTransactionId("txn_123");
        mockResponse.setMessage("Payment successful");

        when(paymentApi.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        StepVerifier.create(paymentService.makePayment(1L, 15000L))
                .assertNext(response -> {
                    assertThat(response.getSuccess()).isTrue();
                    assertThat(response.getRemainingBalance()).isEqualTo(85000L);
                    assertThat(response.getTransactionId()).isEqualTo("txn_123");
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleInsufficientFunds() {
        PaymentResponse mockResponse = new PaymentResponse();
        mockResponse.setSuccess(false);
        mockResponse.setRemainingBalance(50000L);
        mockResponse.setMessage("Insufficient funds");

        when(paymentApi.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        StepVerifier.create(paymentService.makePayment(1L, 100000L))
                .assertNext(response -> {
                    assertThat(response.getSuccess()).isFalse();
                    assertThat(response.getMessage()).contains("Insufficient funds");
                })
                .verifyComplete();
    }

    @Test
    void shouldCheckEnoughBalance_True() {
        // Given
        BalanceResponse mockResponse = new BalanceResponse();
        mockResponse.setBalance(100000L);

        when(paymentApi.getBalance()).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(paymentService.hasEnoughBalance(50000L))
                .assertNext(hasBalance -> assertThat(hasBalance).isTrue())
                .verifyComplete();
    }

    @Test
    void shouldCheckEnoughBalance_False() {
        BalanceResponse mockResponse = new BalanceResponse();
        mockResponse.setBalance(30000L);

        when(paymentApi.getBalance()).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(paymentService.hasEnoughBalance(50000L))
                .assertNext(hasBalance -> assertThat(hasBalance).isFalse())
                .verifyComplete();
    }

    @Test
    void shouldReturnFalse_WhenPaymentServiceUnavailable() {
        when(paymentApi.getBalance()).thenReturn(Mono.error(new RuntimeException("Unavailable")));

        StepVerifier.create(paymentService.hasEnoughBalance(10000L))
                .assertNext(hasBalance -> assertThat(hasBalance).isFalse())
                .verifyComplete();
    }
}
