package com.example.mymarketapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        var authManager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authManager.setPasswordEncoder(passwordEncoder);
        return authManager;
    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create("/"));
        return handler;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerLogoutSuccessHandler logoutSuccessHandler) {

        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/items", "/items/**").permitAll()
                        .pathMatchers("/login", "/logout").permitAll()
                        .pathMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                        .pathMatchers("/cart/**").authenticated()
                        .pathMatchers("/orders/**").authenticated()
                        .pathMatchers("/error").permitAll()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }
}