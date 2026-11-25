package dev.hostapp.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void login() {
        String email = emailField.getText();
        String password = passwordField.getText();
        runTask(() -> app.getAuthService().login(email, password), user -> {
            passwordField.clear();
            app.showMain(user);
        });
    }

    @FXML
    private void register() {
        app.showRegister();
    }
}
