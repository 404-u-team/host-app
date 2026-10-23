package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.util.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {

    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void register() {
        String name = nameField.getText();
        String surname = surnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        runTask(() -> {
            app.getAuthService().register(name, surname, email, password);
            return null;
        }, result -> {
            passwordField.clear();
            app.showLogin();
            Dialogs.info(app.getStage(), "Регистрация", "Аккаунт создан. Войдите в систему.");
        });
    }

    @FXML
    private void back() {
        app.showLogin();
    }
}
