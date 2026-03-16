package com.example.paymentservice;

import com.example.paymentservice.model.BalanceResponse;
import com.example.paymentservice.model.PaymentRequest;
import com.example.paymentservice.model.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PaymentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-application-type=reactive",
                "server.port=0"
        })
@AutoConfigureWebTestClient
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetBalance() {
        webTestClient.get()
                .uri("/api/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .value(response -> {
                    assertThat(response.getBalance()).isGreaterThan(0);
                    assertThat(response.getCurrency()).isEqualTo("RUB");
                });
    }

    @Test
    void shouldMakeSuccessfulPayment() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(123L);
        request.setAmount(10000L);
        request.setDescription("Test payment");

        webTestClient.post()
                .uri("/api/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .value(response -> {
                    assertThat(response.getSuccess()).isTrue();
                    assertThat(response.getRemainingBalance()).isGreaterThanOrEqualTo(0);
                    assertThat(response.getTransactionId()).isNotNull();
                });
    }

    @Test
    void shouldRejectMissingOrderId() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(10000L);

        webTestClient.post()
                .uri("/api/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldHandleInsufficientFunds() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(999L);
        request.setAmount(999999999L);
        request.setDescription("Too much");

        webTestClient.post()
                .uri("/api/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .value(response -> {
                    assertThat(response.getSuccess()).isFalse();
                    assertThat(response.getMessage()).contains("Недостаточно средств");
                });
    }
}