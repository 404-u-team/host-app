package dev.hostapp.backend.security;

import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final JwtService jwt;

    public AuthInterceptor(AuthService authService, JwtService jwt) {
        this.authService = authService;
        this.jwt = jwt;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        String authorization = request.getHeader("Authorization");
        boolean bearer = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
        String token = bearer ? authorization.substring(7) : getToken(request);
        if (token == null) {
            throw new UnauthorizedException("Сначала войдите в систему");
        }
        User user = bearer ? jwt.resolve(token) : authService.getUserByToken(token);

        if (user == null) {
            throw new UnauthorizedException("Сессия недействительна, войдите снова");
        }

        if (user.mustChangePassword()) {
            throw new ForbiddenException("Сначала смените временный пароль через /api/auth/change-password");
        }
        if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD") && !user.canEdit()) {
            throw new ForbiddenException("Нет прав на изменение данных");
        }
        request.setAttribute("user", user);

        return true;
    }

    private String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
