package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {}