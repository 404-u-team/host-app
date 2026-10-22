package dev.hostapp.desktop.util;

public class Validation {

    public static String text(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Заполните поле «" + field + "».");
        }
        String result = value.trim();
        if (result.length() > maxLength) {
            throw new IllegalArgumentException("Поле «" + field + "»: не более " + maxLength + " символов.");
        }
        return result;
    }

    public static int positiveInteger(String value, String field) {
        try {
            int number = Integer.parseInt(value.trim());
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Поле «" + field + "»: введите целое число от 1 до 2147483647.");
        }
        throw new IllegalArgumentException("Поле «" + field + "»: значение должно быть больше нуля.");
    }

    public static String email(String value) {
        String email = text(value, "Email или ADMIN", 255);
        if (!email.equalsIgnoreCase("ADMIN") && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Введите корректный email или логин ADMIN.");
        }
        return email;
    }

    public static String password(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException("Введите пароль длиной от 1 до 255 символов.");
        }
        return value;
    }
}
