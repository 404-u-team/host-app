package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.util.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController extends BaseController {
    @FXML private VBox registrationFields;
    @FXML private VBox verificationFields;
    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField codeField;
    private String pendingEmail;
    private String pendingPassword;

    @FXML
    private void register() {
        String name = nameField.getText();
        String surname = surnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        runTask(() -> app.getAuthService().register(name, surname, email, password), result -> {
            passwordField.clear();
            if (result.verificationRequired()) {
                pendingEmail = result.email();
                pendingPassword = password;
                registrationFields.setVisible(false);
                registrationFields.setManaged(false);
                verificationFields.setVisible(true);
                verificationFields.setManaged(true);
                codeField.requestFocus();
                Dialogs.info(app.getStage(), "Подтверждение email",
                        "Введите код из письма на " + pendingEmail + ". Код действует 10 минут."
                        + " Если письмо не пришло, запросите новый код.");
            } else {
                registered();
            }
        });
    }

    @FXML
    private void verifyEmail() {
        String code = codeField.getText();
        runTask(() -> app.getAuthService().verifyEmail(pendingEmail, code), result -> registered());
    }

    @FXML
    private void resendCode() {
        runTask(() -> {
            app.getAuthService().resendEmailCode(pendingEmail, pendingPassword);
            return null;
        }, result -> {
            codeField.clear();
            Dialogs.info(app.getStage(), "Подтверждение email", "Новый код отправлен. Предыдущий код больше не действует.");
        });
    }

    private void registered() {
        pendingPassword = null;
        app.showLogin();
        Dialogs.info(app.getStage(), "Регистрация", "Аккаунт создан. Войдите в систему.");
    }

    @FXML
    private void back() {
        pendingPassword = null;
        app.showLogin();
    }
}
