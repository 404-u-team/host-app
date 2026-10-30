package dev.hostapp.backend.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import dev.hostapp.backend.exceptions.EmailDeliveryException;

@Service
public class EmailService {
    private final JavaMailSender sender;
    private final String from;
    private final boolean configured;
    private final boolean verificationRequired;
    private final boolean notificationsEnabled;

    public EmailService(Environment env, ObjectProvider<JavaMailSender> senders) {
        String host = env.getProperty("spring.mail.host", "").trim();
        from = env.getProperty("app.email.from", "").trim();
        String username = env.getProperty("spring.mail.username", "");
        String password = env.getProperty("spring.mail.password", "");
        boolean auth = env.getProperty("spring.mail.properties.mail.smtp.auth", Boolean.class, true);
        boolean ssl = env.getProperty("spring.mail.properties.mail.smtp.ssl.enable", Boolean.class, false);
        boolean tls = env.getProperty("spring.mail.properties.mail.smtp.starttls.enable", Boolean.class, true);
        configured = !host.isBlank() && isEmailAddress(from) && (!auth || (!username.isBlank() && !password.isBlank()));
        boolean anySettings = !host.isBlank() || !from.isBlank() || !username.isBlank() || !password.isBlank();
        verificationRequired = configured || env.getProperty("app.email.verification-enabled", Boolean.class, false);
        notificationsEnabled = env.getProperty("app.email.notifications-enabled", Boolean.class, true);
        sender = senders.getIfAvailable();
        if ((anySettings || verificationRequired) && (!configured || sender == null)) {
            throw new IllegalStateException("SMTP: задайте SMTP_HOST, SMTP_FROM и SMTP_USERNAME/SMTP_PASSWORD (или SMTP_AUTH=false)");
        }
        if (configured && ssl && tls) {
            throw new IllegalStateException("SMTP: выберите SSL или STARTTLS; отключите второй режим");
        }
    }

    public static boolean isEmailAddress(String email) {
        return email != null && email.length() <= 255 && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    }

    public boolean isVerificationRequired() { return verificationRequired; }
    public boolean isNotificationsEnabled() { return configured && notificationsEnabled; }

    public void send(String recipient, String subject, String body) {
        if (!configured) {
            throw new EmailDeliveryException("Отправка email не настроена");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException("Не удалось отправить письмо. Повторите попытку позже.");
        }
    }
}
