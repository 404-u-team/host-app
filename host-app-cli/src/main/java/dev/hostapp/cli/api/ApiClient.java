package dev.hostapp.cli.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();

        CookieManager cookieManager = new CookieManager(
            null,
            CookiePolicy.ACCEPT_ALL
        );

        this.httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .build();
    }

    public <T> T post(
        String path,
        Object body,
        Class<T> responseType
    ) throws IOException, InterruptedException {

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("User-Agent", "host-app-cli/1.0")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 400) {
            throw new RuntimeException(
                "Server returned HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body()
            );
        }

        return objectMapper.readValue(
            response.body(),
            responseType
        );
    }
}