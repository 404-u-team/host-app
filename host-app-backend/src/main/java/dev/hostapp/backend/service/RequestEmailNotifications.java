package dev.hostapp.backend.service;

import dev.hostapp.backend.exceptions.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RequestEmailNotifications {
    private static final Logger LOG = LoggerFactory.getLogger(RequestEmailNotifications.class);
    private final EmailService email;

    public RequestEmailNotifications(EmailService email) { this.email = email; }

    @TransactionalEventListener
    public void statusChanged(RequestStatusChanged event) {
        if (!email.isNotificationsEnabled() || !event.emailVerified()
                || !EmailService.isEmailAddress(event.email()) || event.oldStatus() == event.newStatus()) {
            return;
        }
        String type = switch (event.type()) {
            case CREATE -> "Создание сервера";
            case CHANGE -> "Изменение сервера";
            case CANCEL -> "Отмена сервера";
        };
        try {
            email.send(event.email(), "Host App: изменён статус заявки",
                    "Статус вашей заявки " + event.requestId() + " изменён: "
                            + event.oldStatus() + " → " + event.newStatus() + ".\n\n"
                            + "Тип заявки: " + type + "\n"
                            + "ОС: " + event.os() + "\n"
                            + "CPU: " + event.cpuCores() + "\n"
                            + "RAM: " + event.ramGb() + " ГБ\n"
                            + "Диск: " + event.diskGb() + " ГБ"
                            + (event.serverId() == null ? "" : "\nСервер: " + event.hostname() + " (" + event.serverId() + ")"));
        } catch (EmailDeliveryException exception) {
            // The request has committed. Do not report a failed status update to the client.
            LOG.warn("Email notification failed for request {}", event.requestId());
        }
    }
}
