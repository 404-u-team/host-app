package dev.hostapp.backend.security;

import dev.hostapp.backend.error.TooManyAttemptsException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LoginAttempts {
    private final Map<String, Counter> counters = new HashMap<>();
    private Instant nextCleanup = Instant.EPOCH;

    public synchronized void start(String login, String ip) {
        Instant now = Instant.now();
        if (!nextCleanup.isAfter(now)) {
            counters.values().removeIf(counter -> !counter.until.isAfter(now));
            nextCleanup = now.plusSeconds(60);
        }
        String loginKey = loginKey(login);
        String ipKey = "ip:" + ip;
        if (count(loginKey, now) >= 5 || count(ipKey, now) >= 30 || counters.size() >= 20_000) {
            throw new TooManyAttemptsException();
        }
        increment(loginKey, now);
        increment(ipKey, now);
    }

    public synchronized void success(String login, String ip) {
        counters.remove(loginKey(login));
        String ipKey = "ip:" + ip;
        Counter counter = counters.get(ipKey);
        if (counter != null && --counter.attempts == 0) {
            counters.remove(ipKey);
        }
    }

    private int count(String key, Instant now) {
        Counter counter = counters.get(key);
        return counter != null && counter.until.isAfter(now) ? counter.attempts : 0;
    }

    private void increment(String key, Instant now) {
        Counter counter = counters.get(key);
        if (counter == null || !counter.until.isAfter(now)) {
            counter = new Counter(now.plusSeconds(900));
            counters.put(key, counter);
        }
        counter.attempts++;
    }

    private String loginKey(String login) {
        return "login:" + login.toLowerCase(Locale.ROOT);
    }

    private static class Counter {
        final Instant until;
        int attempts;

        Counter(Instant until) { this.until = until; }
    }
}
