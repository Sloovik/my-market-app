package com.example.mymarketapp.model;

import lombok.Data;

@Data
public class CartItem {
    private long id;
    private String title;
    private String description;
    private String imgPath;
    private long price;
    private int count;
}