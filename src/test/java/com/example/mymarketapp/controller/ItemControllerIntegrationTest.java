package com.example.mymarketapp.controller;

import com.example.mymarketapp.MyMarketAppApplication;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = MyMarketAppApplication.class)
@AutoConfigureMockMvc
class ItemControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ItemService itemService;
    @MockBean private CartService cartService;

    @Test
    void itemsPageLoads() throws Exception {
        when(itemService.getPagedItems(null, "NO", 1, 5, 1L))
                .thenReturn(Collections.singletonList(Collections.emptyList()));
        when(itemService.getPaging(null, null, 1, 5))
                .thenReturn(new PagingDto(5, 1, false, true));
        when(cartService.getTotal(1L)).thenReturn(0L);

        mockMvc.perform(get("/items")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void itemPageLoads() throws Exception {
        ItemDto itemDto = new ItemDto(1L, "Test", "Test desc", "/img/test.jpg", 1000L, 10);
        when(itemService.getItemDto(1L, 1L)).thenReturn(itemDto);
        when(cartService.getTotal(1L)).thenReturn(0L);

        mockMvc.perform(get("/items/1")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));
    }

    @Test
    void itemsPageWithSearch() throws Exception {
        when(itemService.getPagedItems("phone", "NO", 1, 5, 1L))
                .thenReturn(Collections.singletonList(Collections.emptyList()));
        when(itemService.getPaging("phone", null, 1, 5))
                .thenReturn(new PagingDto(5, 1, false, true));
        when(cartService.getTotal(1L)).thenReturn(0L);

        mockMvc.perform(get("/items")
                        .param("search", "phone")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }
}
