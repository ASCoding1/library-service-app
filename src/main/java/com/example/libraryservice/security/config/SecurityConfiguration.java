package com.example.libraryservice.security.config;

import com.example.libraryservice.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/authenticate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/sub/").hasAuthority("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/sub/").hasAuthority("CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/sub/").hasAuthority("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/v1/books/").hasAuthority("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/v1/books/").hasAnyAuthority("EMPLOYEE", "CUSTOMER")
                .requestMatchers("/api/v1/rentals/").hasAuthority("EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/unsubscribe").hasAuthority("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/v1/users/subscribe").hasAuthority("CUSTOMER")
                .anyRequest().authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}
