package com.example.mymarketapp.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("items")
public class Item {

    @Id
    private Long id;

    private String title;
    private String description;
    private String imgPath;
    private long price;
}