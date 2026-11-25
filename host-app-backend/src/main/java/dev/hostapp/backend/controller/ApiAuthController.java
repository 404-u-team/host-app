package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.AccountDtos.*;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.SessionService;
import dev.hostapp.backend.security.RequestAuthentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    private final SessionService sessions;

    private final boolean secureCookie;

    public ApiAuthController(SessionService sessions, @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.sessions = sessions;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody Login input, HttpServletRequest request) {
        return tokenResponse(sessions.login(input, request.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody Refresh input) {
        return tokenResponse(sessions.refresh(input.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody Refresh input) {
        sessions.logout(input.refreshToken());
        var cookie = ResponseCookie.from(RequestAuthentication.DOCS_COOKIE, "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(0).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @SecurityRequirement(name = "bearer")
    @GetMapping("/me")
    public UserView me(@RequestAttribute("user") User user) { return UserView.from(user); }

    @SecurityRequirement(name = "bearer")
    @PostMapping("/change-password")
    public ResponseEntity<TokenPair> changePassword(@RequestAttribute("user") User user,
                                                    @Valid @RequestBody PasswordChange input) {
        return tokenResponse(sessions.changePassword(user, input));
    }

    private ResponseEntity<TokenPair> tokenResponse(TokenPair pair) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pair);
    }
}
