package com.example.mymarketapp.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedItem implements Serializable {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
}