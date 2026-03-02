package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findByCartId(Long cartId);

    Mono<CartItem> findByCartIdAndItemId(Long cartId, Long itemId);

    Mono<Void> deleteByCartId(Long cartId);
}