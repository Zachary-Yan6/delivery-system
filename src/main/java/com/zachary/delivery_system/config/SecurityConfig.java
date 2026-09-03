package com.zachary.delivery_system.config;

import com.zachary.delivery_system.security.JwtAuthenticationFilter;
import com.zachary.delivery_system.security.RestSecurityErrorHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestSecurityErrorHandler restSecurityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restSecurityErrorHandler)
                        .accessDeniedHandler(restSecurityErrorHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/ws",
                                "/ws/**"
                        ).permitAll()

                        .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        .requestMatchers("/api/dispatcher/**")
                        .hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/deliveries/*/confirm"
                        ).hasAnyRole(
                                "CUSTOMER",
                                "DISPATCHER",
                                "ADMIN"
                        )
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/deliveries",
                                "/api/deliveries/**"
                        ).hasAnyRole("ADMIN", "DISPATCHER", "CUSTOMER", "DRIVER")
                        .requestMatchers(
                                "/api/deliveries/**",
                                "/api/drivers/**"
                        ).hasAnyRole("DISPATCHER", "ADMIN")


                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }
}
