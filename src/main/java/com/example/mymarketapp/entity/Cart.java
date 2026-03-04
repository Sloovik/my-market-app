package com.example.mymarketapp.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("carts")
public class Cart {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;
}