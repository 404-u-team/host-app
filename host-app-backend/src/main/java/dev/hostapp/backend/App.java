package dev.hostapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRequestRepository;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    CommandLineRunner seedDemoData(UserRepository userRepository, ServerRequestRepository requestRepository) {
        return args -> {
            Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            String passwordHash = encoder.encode("demo1234");
            List<User> users = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                String email = "demo" + i + "@hostapp.local";
                String surname = "User " + i;
                User user = userRepository.findByEmail(email)
                        .orElseGet(() -> userRepository.save(new User("Demo", surname, email, passwordHash)));
                users.add(user);
            }

            long demoRequestCount = users.stream().mapToLong(requestRepository::countByOwner).sum();
            String[] operatingSystems = {"Ubuntu 24.04", "Debian 12", "AlmaLinux 9", "Windows Server 2022", "Rocky Linux 9"};
            ServerRequest.RequestStatus[] statuses = ServerRequest.RequestStatus.values();

            for (int i = (int) demoRequestCount; i < 10; i++) {
                User owner = users.get(i % users.size());
                ServerRequest request = new ServerRequest(
                        owner,
                        1 + i % 8,
                        2 + i % 16,
                        20 + i * 10,
                        operatingSystems[i % operatingSystems.length]
                );
                request.setStatus(statuses[(i / 5 + i % 5) % statuses.length]);
                requestRepository.save(request);
            }
        };
    }
}
