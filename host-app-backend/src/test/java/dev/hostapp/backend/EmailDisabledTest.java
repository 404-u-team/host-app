package dev.hostapp.backend;

import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.service.AuthService;
import dev.hostapp.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:email-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:legacy-users.sql",
        "spring.mail.host=", "spring.mail.username=", "spring.mail.password=", "app.email.from=",
        "app.email.verification-enabled=false",
        "app.admin.bootstrap-password="
})
class EmailDisabledTest {
    @Autowired AuthService auth;
    @Autowired EmailService email;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void noSmtpRequiresOnlyValidEmailAndAllowsLogin() {
        assertFalse(email.isVerificationRequired());
        var user = auth.registerUser("Name", "Surname", "new@example.org", "password");
        assertTrue(user.isEmailVerified());
        assertNotNull(auth.loginUser(user.getEmail(), "password", "ip", "agent"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.registerUser("Name", "Surname", "invalid", "password"));
    }

    @Test
    void schemaUpgradePreservesLegacyAccountsAndAdminLogin() {
        var user = users.findByEmail("ADMIN").orElseThrow();
        assertTrue(user.isEmailVerified());
        assertEquals(0, user.getEmailCodeAttempts());
        assertEquals("CREATE", jdbc.queryForObject("SELECT type FROM server_requests WHERE os = 'Debian'", String.class));
        String hash = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("password");
        jdbc.update("UPDATE users SET password_hash = ? WHERE email = 'ADMIN'", hash);
        assertNotNull(auth.loginUser("ADMIN", "password", "ip", "agent"));
    }
}
