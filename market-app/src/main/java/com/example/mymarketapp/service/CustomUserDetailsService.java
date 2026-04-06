package com.example.mymarketapp.service;

import com.example.mymarketapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    log.debug("Loading user: {}", username);

                    boolean isEnabled = user.getEnabled() != null && user.getEnabled();

                    return User.builder()
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .disabled(!isEnabled)
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority(user.getRole())
                            ))
                            .build();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", username);
                    return Mono.empty();
                }));
    }
}