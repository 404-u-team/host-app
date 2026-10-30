package dev.hostapp.backend;

import dev.hostapp.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class EmailConfigurationTest {
    private EmailService create(MockEnvironment env) {
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mail", new JavaMailSenderImpl());
        return new EmailService(env, factory.getBeanProvider(JavaMailSender.class));
    }

    @Test
    void withoutSmtpOnlyRegexIsRequired() {
        EmailService email = create(new MockEnvironment());
        assertFalse(email.isVerificationRequired());
        assertFalse(email.isNotificationsEnabled());
        assertTrue(EmailService.isEmailAddress("user@example.org"));
        assertFalse(EmailService.isEmailAddress("user@invalid"));
        assertFalse(EmailService.isEmailAddress("user@example.org\r\nBcc: other@example.org"));
    }

    @Test
    void enabledFlagOrPartialSettingsRequireCompleteSmtp() {
        assertThrows(IllegalStateException.class,
                () -> create(new MockEnvironment().withProperty("app.email.verification-enabled", "true")));
        assertThrows(IllegalStateException.class,
                () -> create(new MockEnvironment().withProperty("spring.mail.host", "smtp.example.org")));
    }

    @Test
    void sslAndStarttlsAreExclusiveAndAuthenticationCanBeDisabled() {
        var env = new MockEnvironment()
                .withProperty("spring.mail.host", "localhost")
                .withProperty("app.email.from", "host@example.org")
                .withProperty("spring.mail.properties.mail.smtp.auth", "false");
        assertTrue(create(env).isVerificationRequired());
        assertTrue(create(env).isNotificationsEnabled());
        env.withProperty("spring.mail.properties.mail.smtp.ssl.enable", "true");
        assertThrows(IllegalStateException.class, () -> create(env));
        env.withProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");
        assertTrue(create(env).isVerificationRequired());
        env.withProperty("app.email.notifications-enabled", "false");
        assertFalse(create(env).isNotificationsEnabled());
    }
}
