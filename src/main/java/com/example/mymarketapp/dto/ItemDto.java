package com.example.mymarketapp.dto;

public record ItemDto(long id, String title, String description, String imgPath, long price, int count) {}