package com.example.mymarketapp.config;

import com.example.mymarketapp.client.api.PaymentApi;
import com.example.mymarketapp.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("!test")
public class PaymentApiOAuth2Config {

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentWebClient(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId("keycloak");

        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter(oauth2Filter)
                .build();
    }

    @Bean
    public ApiClient oauth2PaymentApiClient(WebClient paymentWebClient) {
        ApiClient apiClient = new ApiClient(paymentWebClient);
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        ReactiveOAuth2AuthorizedClientService clientService =
                new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);

        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, clientService);

        manager.setAuthorizedClientProvider(
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build()
        );

        return manager;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient paymentApiClient) {
        return new PaymentApi(paymentApiClient);
    }
}