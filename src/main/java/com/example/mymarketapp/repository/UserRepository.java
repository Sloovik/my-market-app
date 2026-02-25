package com.example.mymarketapp.repository;

import com.example.mymarketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}