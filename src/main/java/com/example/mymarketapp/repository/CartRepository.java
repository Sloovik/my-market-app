package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.Cart;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {

    Mono<Cart> findByUserId(Long userId);
}