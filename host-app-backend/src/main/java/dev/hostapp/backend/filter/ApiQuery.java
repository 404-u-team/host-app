package dev.hostapp.backend.filter;

import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class ApiQuery {
    private ApiQuery() {}

    public static PageRequest page(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page должен быть >= 0, size от 1 до 100");
        }
        return PageRequest.of(page, size, sort);
    }

    public static Sort sort(String field, String direction, Map<String, String> fields) {
        String property = fields.get(field);
        if (property == null) {
            throw new IllegalArgumentException("Недопустимое поле сортировки. Доступны " + fields.keySet());
        }
        return Sort.by(Sort.Direction.fromString(direction), property).and(Sort.by("id"));
    }

    public static String contains(String input) {
        return "%" + input.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
