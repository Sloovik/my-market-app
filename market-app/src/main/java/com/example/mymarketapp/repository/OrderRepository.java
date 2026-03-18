package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {}