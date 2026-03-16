package com.example.mymarketapp.config;

import com.example.mymarketapp.client.api.PaymentApi;
import com.example.mymarketapp.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient paymentApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient apiClient) {
        return new PaymentApi(apiClient);
    }
}