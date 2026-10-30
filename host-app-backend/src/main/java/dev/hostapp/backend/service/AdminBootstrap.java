package dev.hostapp.backend.service;

import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final String password;

    public AdminBootstrap(UserRepository users,
                          @Value("${app.admin.bootstrap-password:}") String password) {
        this.users = users;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (password.isBlank() || users.existsByEmailIgnoreCase("ADMIN")) {
            return;
        }
        if (password.length() < 12 || password.length() > 255) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD: пароль должен содержать от 12 до 255 символов");
        }

        String hash = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(password);
        User admin = new User("Admin", "Administrator", "ADMIN", hash);
        admin.verifyEmail();
        users.save(admin);
    }
}
