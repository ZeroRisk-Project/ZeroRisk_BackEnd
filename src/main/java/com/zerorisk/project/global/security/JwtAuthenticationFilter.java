package com.zerorisk.project.global.security;

import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import com.zerorisk.project.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        extractAccessToken(request)
                .filter(jwtTokenProvider::validateToken)
                .map(jwtTokenProvider::getUserId)
                .flatMap(userRepository::findById)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::setAuthentication);

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private void setAuthentication(User user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name());
        var authentication = new UsernamePasswordAuthenticationToken(
                user.getId(), null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}