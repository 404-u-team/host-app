package dev.hostapp.cli.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

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
        checkResponse(response.statusCode(), response.body());

        return objectMapper.readValue(
            response.body(),
            responseType
        );
    }

    public <T> T get(String path, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("User-Agent", "host-app-cli/1.0")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
        return objectMapper.readValue(response.body(), responseType);
    }

    public <T> List<T> getList(String path, Class<T> elementType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("User-Agent", "host-app-cli/1.0")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return objectMapper.readValue(response.body(), listType);
    }

    public <T> T put(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("User-Agent", "host-app-cli/1.0")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
        return objectMapper.readValue(response.body(), responseType);
    }

    public <T> T patch(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("User-Agent", "host-app-cli/1.0")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
        return objectMapper.readValue(response.body(), responseType);
    }

    public <T> T patch(String path, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("User-Agent", "host-app-cli/1.0")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
        return objectMapper.readValue(response.body(), responseType);
    }

    public void delete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("User-Agent", "host-app-cli/1.0")
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response.statusCode(), response.body());
    }

    public byte[] getBytes(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("User-Agent", "host-app-cli/1.0")
            .GET()
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new RuntimeException(errorMessage(response.statusCode(), new String(response.body())));
        }
        return response.body();
    }

    private void checkResponse(int statusCode, String body) throws IOException {
        if (statusCode >= 400) {
            throw new RuntimeException(errorMessage(statusCode, body));
        }
    }

    private String errorMessage(int statusCode, String body) {
        try {
            var json = objectMapper.readTree(body);
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
            if (json.hasNonNull("error")) {
                return json.get("error").asText();
            }
        } catch (Exception ignored) {
        }

        if (statusCode == 401) {
            return "Сначала войдите в систему";
        }
        if (statusCode == 404) {
            return "Запись не найдена";
        }
        return "Ошибка сервера (HTTP " + statusCode + ")";
    }
}
