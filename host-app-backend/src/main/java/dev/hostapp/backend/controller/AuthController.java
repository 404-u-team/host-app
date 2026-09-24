package dev.hostapp.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.hostapp.backend.dto.auth.RegisterRequest;

@RestController
@RequestMapping ("/auth")
public class AuthController {

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest req) {
        
    }
}
