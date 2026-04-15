package com.example.paymentservice;

import com.example.paymentservice.service.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @TestConfiguration
    static class NoSecurityConfig {
        @Bean
        public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(auth -> auth.anyExchange().permitAll())
                    .build();
        }
    }

    @Autowired
    private PaymentServiceImpl paymentService;

    @Test
    void shouldReturnInitialBalance() {
        StepVerifier.create(paymentService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualTo(100000L))
                .verifyComplete();
    }

    @Test
    void shouldProcessSuccessfulPayment() {
        paymentService.resetBalance().block();

        StepVerifier.create(paymentService.processPayment(1L, 30000L, "Test payment"))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getRemainingBalance()).isEqualTo(70000L);
                    assertThat(result.getTransactionId()).isNotNull();
                    assertThat(result.getMessage()).contains("успешно");
                })
                .verifyComplete();
    }

    @Test
    void shouldFailPaymentWithInsufficientFunds() {
        paymentService.resetBalance().block();

        StepVerifier.create(paymentService.processPayment(1L, 150000L, "Too much"))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
                    assertThat(result.getMessage()).contains("Недостаточно");
                })
                .verifyComplete();
    }

    @Test
    void shouldResetBalance() {
        paymentService.getBalance().block();
        paymentService.processPayment(1L, 30000L, "Test").block();

        StepVerifier.create(paymentService.resetBalance())
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .assertNext(balance -> assertThat(balance).isEqualTo(100000L))
                .verifyComplete();
    }
}