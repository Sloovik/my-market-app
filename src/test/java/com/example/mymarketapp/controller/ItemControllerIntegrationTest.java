package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ItemControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ItemService itemService;

    @MockBean
    private CartService cartService;

    @BeforeEach
    void setUp() {
        when(cartService.updateCart(anyLong(), anyLong(), any(ActionDto.class)))
                .thenReturn(Mono.empty());
        when(cartService.getTotal(anyLong())).thenReturn(Mono.just(0L));
    }

    @Test
    void itemsPageLoads() {
        when(itemService.getPagedItems(isNull(), eq("NO"), eq(1), eq(5), eq(1L)))
                .thenReturn(Mono.just(Collections.singletonList(Collections.emptyList())));
        when(itemService.getPaging(isNull(), eq("NO"), eq(1), eq(5)))
                .thenReturn(Mono.just(new PagingDto(5, 1, false, true)));

        webTestClient.get().uri("/items").exchange().expectStatus().isOk();
    }

    @Test
    void itemPageLoads() {
        ItemDto itemDto = new ItemDto(1L, "Test", "Test desc", "/img/test.jpg", 1000L, 10);
        when(itemService.getItemDto(eq(1L), eq(1L))).thenReturn(Mono.just(itemDto));

        webTestClient.get().uri("/items/1").exchange().expectStatus().isOk();
    }

    @Test
    void updateFromListRedirects() {
        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void updateFromItemReturnsItemView() {
        ItemDto itemDto = new ItemDto(1L, "Test", "Test desc", "/img/test.jpg", 1000L, 11);
        when(itemService.getItemDto(eq(1L), eq(1L))).thenReturn(Mono.just(itemDto));

        webTestClient.post()
                .uri("/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isOk();
    }
}