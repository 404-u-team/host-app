package dev.hostapp.backend;

import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.service.AdminBootstrap;
import dev.hostapp.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-bootstrap;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.mail.host=smtp.example.org",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.email.from=host@example.org",
        "app.email.verification-enabled=true",
        "app.admin.bootstrap-password=initial-admin-password"
})
@Transactional
class AdminBootstrapTest {
    @Autowired UserRepository users;
    @Autowired AuthService auth;
    @MockitoBean JavaMailSender sender;

    @Test
    void startupCreatesAdministratorWhoCanLoginWithoutEmailEvenWithSmtpEnabled() {
        User admin = users.findByEmail("ADMIN").orElseThrow();
        assertTrue(admin.isEmailVerified());
        assertNull(admin.getEmailCodeHash());
        assertNotEquals("initial-admin-password", admin.getPasswordHash());
        assertTrue(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                .matches("initial-admin-password", admin.getPasswordHash()));
        String token = auth.loginUser("ADMIN", "initial-admin-password", "ip", "agent");
        assertEquals(admin.getId(), auth.getUserByToken(token).getId());
        verifyNoInteractions(sender);
    }

    @Test
    void repeatedStartupDoesNotReplaceAdministratorOrResetPassword() {
        User original = users.findByEmail("ADMIN").orElseThrow();
        String originalHash = original.getPasswordHash();
        new AdminBootstrap(users, "replacement-password").run(null);
        User admin = users.findByEmail("ADMIN").orElseThrow();
        assertEquals(original.getId(), admin.getId());
        assertEquals(originalHash, admin.getPasswordHash());
        verifyNoInteractions(sender);
    }

    @Test
    void existingAdministratorWithDifferentCaseIsAlsoPreserved() {
        users.deleteAll();
        users.flush();
        User original = new User("Existing", "Admin", "admin", "existing-hash");
        original.requireEmailVerification();
        users.saveAndFlush(original);
        new AdminBootstrap(users, "replacement-password").run(null);
        assertEquals(1, users.count());
        User admin = users.findByEmail("admin").orElseThrow();
        assertEquals("existing-hash", admin.getPasswordHash());
        assertFalse(admin.isEmailVerified());
        verifyNoInteractions(sender);
    }

    @Test
    void blankPasswordLeavesEmptyDatabaseWithoutAnAdministrator() {
        users.deleteAll();
        users.flush();
        new AdminBootstrap(users, "").run(null);
        assertEquals(0, users.count());
        verifyNoInteractions(sender);
    }

    @Test
    void invalidPasswordLengthPreventsAccountCreation() {
        users.deleteAll();
        users.flush();
        for (String password : new String[] {"too-short", "x".repeat(256)}) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> new AdminBootstrap(users, password).run(null));
            assertFalse(error.getMessage().contains(password));
        }
        assertEquals(0, users.count());
        verifyNoInteractions(sender);
    }
}
