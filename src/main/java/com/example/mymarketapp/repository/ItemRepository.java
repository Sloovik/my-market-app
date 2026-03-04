package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    @Query("""
        SELECT * FROM items 
        WHERE title LIKE CONCAT('%', :search, '%') 
           OR description LIKE CONCAT('%', :search, '%')
        LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
        """)
    Flux<Item> findBySearch(String search, Pageable pageable);

    @Query("""
        SELECT COUNT(*) FROM items 
        WHERE title LIKE CONCAT('%', :search, '%') 
           OR description LIKE CONCAT('%', :search, '%')
        """)
    Mono<Long> countBySearch(String search);
}