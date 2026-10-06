package com.kinal.app.biblioteca.config;

import com.kinal.app.biblioteca.security.JwtAccessDeniedHandler;
import com.kinal.app.biblioteca.security.JwtAuthenticationEntryPoint;
import com.kinal.app.biblioteca.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints publicos de autenticacion
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 2. Endpoints de Libros
                        .requestMatchers(HttpMethod.POST, "/api/v1/libros", "/api/v1/libros/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/libros/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/libros/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/libros", "/api/v1/libros/**").authenticated()

                        // 3. Endpoints de Prestamos
                        .requestMatchers(HttpMethod.POST, "/api/v1/prestamos").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/prestamos/*/devolucion").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/atrasados").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/mis-prestamos").hasRole("LECTOR")

                        // Cualquier otra peticion debe ser autenticada
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
