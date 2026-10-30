package dev.hostapp.backend;

import dev.hostapp.backend.dto.serverrequest.CreateServerRequest;
import dev.hostapp.backend.exceptions.EmailDeliveryException;
import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.model.ServerRequest.RequestStatus;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.service.AuthService;
import dev.hostapp.backend.service.EmailVerificationService;
import dev.hostapp.backend.service.ServerRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:email-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.mail.host=smtp.example.org",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.email.from=host@example.org",
        "app.email.verification-enabled=false"
})
class EmailFlowTest {
    @Autowired org.springframework.core.env.Environment env;
    @Autowired AuthService auth;
    @Autowired EmailVerificationService verification;
    @Autowired UserRepository users;
    @Autowired ServerRequestService requests;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender sender;

    private String address;

    @BeforeEach
    void prepare() {
        reset(sender);
        address = UUID.randomUUID() + "@example.org";
    }

    private User register() {
        return auth.registerUser("Имя", "Фамилия", address, "password");
    }

    private String deliveredCode() {
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, atLeastOnce()).send(message.capture());
        var matcher = Pattern.compile("Код подтверждения: ([0-9]{6})").matcher(message.getValue().getText());
        assertTrue(matcher.find());
        assertEquals(address, message.getValue().getTo()[0]);
        return matcher.group(1);
    }

    private java.net.http.HttpResponse<String> post(String path, String body) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                "http://127.0.0.1:" + env.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
        return java.net.http.HttpClient.newHttpClient().send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void publicHttpEndpointsRequireCodeBeforeIssuingSession() throws Exception {
        String credentials = "{\"email\":\"" + address + "\",\"password\":\"password\"}";
        var registration = post("/auth/register", "{\"name\":\"Name\",\"surname\":\"Surname\","
                + "\"email\":\"" + address + "\",\"password\":\"password\"}");
        assertEquals(201, registration.statusCode());
        assertTrue(registration.body().contains("\"verificationRequired\":true"));
        assertFalse(registration.body().contains("emailCode"));
        assertEquals(401, post("/auth/login", credentials).statusCode());
        assertEquals(400, post("/auth/resend-email-code", credentials).statusCode());
        String code = deliveredCode();
        var confirmation = post("/auth/verify-email", "{\"email\":\"" + address + "\",\"code\":\"" + code + "\"}");
        assertEquals(200, confirmation.statusCode());
        assertTrue(confirmation.body().contains("\"verificationRequired\":false"));
        var login = post("/auth/login", credentials);
        assertEquals(200, login.statusCode());
        assertTrue(login.headers().firstValue("Set-Cookie").orElseThrow().contains("session="));
    }

    @Test
    void smtpAutomaticallyRequiresVerificationAndCodeIsSingleUse() {
        User pending = register();
        String code = deliveredCode();
        assertFalse(pending.isEmailVerified());
        assertNotEquals(code, users.findByEmail(address).orElseThrow().getEmailCodeHash());
        assertThrows(UnauthorizedException.class, () -> auth.loginUser(address, "password", "ip", "agent"));
        assertTrue(verification.verify(address, code).isEmailVerified());
        assertNotNull(auth.loginUser(address, "password", "ip", "agent"));
        assertNull(users.findByEmail(address).orElseThrow().getEmailCodeHash());
        assertThrows(IllegalArgumentException.class, () -> verification.verify(address, code));
    }

    @Test
    void failedAttemptsAreCommittedAndFiveAttemptsBlockEvenTheCorrectCode() {
        register();
        String code = deliveredCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(IllegalArgumentException.class, () -> verification.verify(address, wrong));
            assertEquals(attempt, users.findByEmail(address).orElseThrow().getEmailCodeAttempts());
        }
        assertThrows(IllegalArgumentException.class, () -> verification.verify(address, code));
        assertFalse(users.findByEmail(address).orElseThrow().isEmailVerified());
    }

    @Test
    void expiredCodeCannotBeUsed() {
        register();
        String code = deliveredCode();
        User user = users.findByEmail(address).orElseThrow();
        user.issueEmailCode(user.getEmailCodeHash(), Instant.now().minusSeconds(601));
        users.save(user);
        assertThrows(IllegalArgumentException.class, () -> verification.verify(address, code));
    }

    @Test
    void resumingRegistrationDoesNotChangeCredentialsOrSendMoreMail() {
        register();
        assertFalse(register().isEmailVerified());
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
        assertThrows(RuntimeException.class, () -> auth.registerUser("Other", "Name", address, "other-password"));
        assertThrows(IllegalArgumentException.class, () -> verification.resend(address, "password"));
        assertThrows(UnauthorizedException.class, () -> verification.resend(address, "wrong-password"));
    }

    @Test
    void resendReplacesCodeAndFailedDeliveryPreservesPreviousCode() {
        register();
        User user = users.findByEmail(address).orElseThrow();
        String originalHash = user.getEmailCodeHash();
        user.issueEmailCode(originalHash, Instant.now().minusSeconds(61));
        users.save(user);
        doThrow(new MailSendException("unavailable")).when(sender).send(any(SimpleMailMessage.class));
        assertThrows(EmailDeliveryException.class, () -> verification.resend(address, "password"));
        assertEquals(originalHash, users.findByEmail(address).orElseThrow().getEmailCodeHash());
        reset(sender);
        verification.resend(address, "password");
        assertNotEquals(originalHash, users.findByEmail(address).orElseThrow().getEmailCodeHash());
        assertTrue(verification.verify(address, deliveredCode()).isEmailVerified());
    }

    @Test
    void registrationDeliveryFailureRollsBackAccount() {
        doThrow(new MailSendException("unavailable")).when(sender).send(any(SimpleMailMessage.class));
        assertThrows(EmailDeliveryException.class, this::register);
        assertFalse(users.findByEmail(address).isPresent());
    }

    @Test
    void statusEmailIsSentAfterCommitAndFailureDoesNotUndoStatus() {
        register();
        User owner = verification.verify(address, deliveredCode());
        var request = requests.create(owner, new CreateServerRequest(2, 4, 20, "Debian"));
        reset(sender);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            requests.updateStatus(request.id(), RequestStatus.COMPLETED);
            verifyNoInteractions(sender);
        });
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals(address, message.getValue().getTo()[0]);
        assertTrue(message.getValue().getText().contains("CREATED → COMPLETED"));
        assertTrue(message.getValue().getText().contains(request.id().toString()));
        var second = requests.create(owner, new CreateServerRequest(2, 4, 20, "Debian"));
        doThrow(new MailSendException("unavailable")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> requests.updateStatus(second.id(), RequestStatus.REJECTED));
        assertEquals(RequestStatus.REJECTED, requests.get(second.id(), owner, false).status());
    }

    @Test
    void rolledBackStatusChangeAndUnverifiedOwnerSendNoNotification() {
        User owner = register();
        var request = requests.create(owner, new CreateServerRequest(2, 4, 20, "Debian"));
        reset(sender);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            requests.updateStatus(request.id(), RequestStatus.REJECTED);
            status.setRollbackOnly();
        });
        assertEquals(RequestStatus.CREATED, requests.get(request.id(), owner, false).status());
        requests.updateStatus(request.id(), RequestStatus.REJECTED);
        verifyNoInteractions(sender);
    }
}
