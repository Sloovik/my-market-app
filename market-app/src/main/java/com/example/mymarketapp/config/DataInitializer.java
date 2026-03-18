package com.example.mymarketapp.config;

import com.example.mymarketapp.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private final ItemService itemService;

    @Override
    public void run(ApplicationArguments args) {
        itemService.initData().block();
    }
}