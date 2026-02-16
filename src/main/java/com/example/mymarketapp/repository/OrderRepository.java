package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}