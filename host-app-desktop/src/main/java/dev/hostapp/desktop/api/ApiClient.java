package dev.hostapp.desktop.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class ApiClient {

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public void clearSession() {
        cookies.getCookieStore().removeAll();
    }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        return mapper.readValue(send("GET", path, null), type);
    }

    public <T> List<T> getList(String path, Class<T> type) throws IOException, InterruptedException {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        return mapper.readValue(send("GET", path, null), listType);
    }

    public <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        return mapper.readValue(send("POST", path, body), type);
    }

    public <T> T put(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        return mapper.readValue(send("PUT", path, body), type);
    }

    public <T> T patch(String path, Class<T> type) throws IOException, InterruptedException {
        return mapper.readValue(send("PATCH", path, null), type);
    }

    public void delete(String path) throws IOException, InterruptedException {
        send("DELETE", path, null);
    }

    public byte[] getBytes(String path) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = httpClient.send(request("GET", path, null),
                HttpResponse.BodyHandlers.ofByteArray());
        checkResponse(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        return response.body();
    }

    private String send(String method, String path, Object body) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request(method, path, body),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponse(response.statusCode(), response.body());
        return response.body();
    }

    private HttpRequest request(String method, String path, Object body) throws JsonProcessingException {
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            publisher = HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        }
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "host-app-desktop/1.0")
                .method(method, publisher)
                .build();
    }

    private void checkResponse(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new ApiException(statusCode, errorMessage(statusCode, body));
        }
    }

    private String errorMessage(int statusCode, String body) {
        String fallback = switch (statusCode) {
            case 401 -> "Неверные данные входа или сессия истекла. Войдите снова.";
            case 403 -> "Недостаточно прав для этой операции.";
            case 404 -> "Запись не найдена. Обновите список.";
            case 409 -> "Данные уже существуют или операция недоступна.";
            default -> "Не удалось выполнить запрос. Код HTTP: " + statusCode;
        };
        if (statusCode >= 500) {
            return "Ошибка backend. Попробуйте ещё раз позже.";
        }
        try {
            JsonNode json = mapper.readTree(body);
            if (json != null && json.hasNonNull("message")) {
                String message = json.get("message").asText();
                if (message.equals("Incorrect email or password")) {
                    return "Неверный логин или пароль.";
                }
                if (message.equals("User already exists")) {
                    return "Аккаунт с таким логином уже существует.";
                }
                if (!message.isBlank()) {
                    return message;
                }
            }
            return fallback;
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }
}
