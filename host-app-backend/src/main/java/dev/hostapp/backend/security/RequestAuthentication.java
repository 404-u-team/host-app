package dev.hostapp.backend.security;

import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestAuthentication extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AuthService legacy;

    public RequestAuthentication(JwtService jwt, AuthService legacy) {
        this.jwt = jwt;
        this.legacy = legacy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        String header = request.getHeader("Authorization");
        User user = null;
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            user = jwt.resolve(header.substring(7));
        } else if (isDocumentation(path) && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session".equals(cookie.getName())) {
                    user = legacy.getUserByToken(cookie.getValue());
                    break;
                }
            }
        }
        if (user != null) {
            request.setAttribute("user", user);
            if (user.mustChangePassword() && !path.equals("/api/auth/me") && !path.equals("/api/auth/change-password")
                    && !path.equals("/api/auth/login") && !path.equals("/api/auth/refresh")
                    && !path.equals("/api/auth/logout")) {
                dev.hostapp.backend.error.ApiError.write(response, 403, "PASSWORD_CHANGE_REQUIRED",
                        "Сначала смените временный пароль");
                return;
            }
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
        }
        chain.doFilter(request, response);
    }

    private boolean isDocumentation(String path) {
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }
}
