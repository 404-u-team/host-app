package dev.hostapp.cli.api;

import dev.hostapp.cli.dto.auth.LoginRequest;
import dev.hostapp.cli.dto.auth.LoginResponse;
import dev.hostapp.cli.dto.auth.RegisterRequest;
import dev.hostapp.cli.dto.auth.RegisterResponse;

import java.io.IOException;

public class AuthApi extends BaseApi {

    public AuthApi(ApiClient client) {
        super(client);
    }

    public RegisterResponse register(
        String name,
        String surname,
        String email,
        String password
    ) throws IOException, InterruptedException {

        RegisterRequest request = new RegisterRequest(
            name,
            surname,
            email,
            password
        );

        return client.post(
            "/auth/register",
            request,
            RegisterResponse.class
        );
    }

    public LoginResponse login(
        String email,
        String password
    ) throws IOException, InterruptedException {

        LoginRequest request = new LoginRequest(
            email,
            password
        );

        return client.post(
            "/auth/login",
            request,
            LoginResponse.class
        );
    }
}
