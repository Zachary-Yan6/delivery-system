package com.zachary.delivery_system.security;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.service.AppUserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserService appUserService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // token you extract from the frontend
        String token = authorizationHeader.substring(7);

        try {
            String username = jwtService.extractUsername(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUser user = appUserService.lambdaQuery()
                        .eq(AppUser::getUsername, username)
                        .one();

                boolean valid = user != null && jwtService.isTokenValid(token, username);

                if (valid) {

                    var authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole())
                    );

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    // principal
                                    user,
                                    // credential
                                    null,
                                    // authorities
                                    authorities
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                }
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid or expired token: protected routes will return 401.
            System.out.println("JWT rejected: " + ignored.getMessage());

        }

        filterChain.doFilter(request, response);
    }
}