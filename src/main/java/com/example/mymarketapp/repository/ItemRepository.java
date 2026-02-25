package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT i FROM Item i WHERE i.title LIKE %:search% OR i.description LIKE %:search%")
    Page<Item> findBySearch(@Param("search") String search, Pageable pageable);
}