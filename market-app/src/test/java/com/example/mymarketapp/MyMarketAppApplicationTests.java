package com.example.mymarketapp;

import com.example.mymarketapp.client.invoker.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class MyMarketAppApplicationTests {

    @MockBean
    private ApiClient apiClient;

    @Test
    void contextLoads() {
    }

}
