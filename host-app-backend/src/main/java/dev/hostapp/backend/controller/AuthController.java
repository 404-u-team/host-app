package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.auth.LoginRequest;
import dev.hostapp.backend.dto.auth.LoginResponse;
import dev.hostapp.backend.dto.auth.RegisterRequest;
import dev.hostapp.backend.dto.auth.RegisterResponse;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.service.AuthService;
import dev.hostapp.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
        @RequestBody RegisterRequest req
    ) {
        User user = authService.registerUser(
            req.name(),
            req.surname(),
            req.email(),
            req.password()
        );

        RegisterResponse response = new RegisterResponse(
            user.getId(),
            user.getName(),
            user.getSurname(),
            user.getEmail()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest req,
            HttpServletRequest request
        ) {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String token = authService.loginUser(
                req.email(),
                req.password(),
                ip,
                userAgent
            );

            User user = userService.getByEmail(req.email());

            ResponseCookie cookie = ResponseCookie
                .from("session", token)
                .httpOnly(true)
                //.secure(true) // TODO: re-enable
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

            LoginResponse response = new LoginResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail()
            );

            return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
        }

}
