package com.example.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class ItemsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void itemsPageLoads() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void itemPageLoads() throws Exception {
        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));
    }

    @Test
    void cartPageLoads() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void ordersPageLoads() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"));
    }
}
