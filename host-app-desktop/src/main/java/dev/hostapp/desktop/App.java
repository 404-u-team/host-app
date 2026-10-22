package dev.hostapp.desktop;

import dev.hostapp.desktop.api.ApiClient;
import dev.hostapp.desktop.api.ApiException;
import dev.hostapp.desktop.api.AuthApi;
import dev.hostapp.desktop.api.ServerApi;
import dev.hostapp.desktop.api.ServerRequestApi;
import dev.hostapp.desktop.controller.LoginController;
import dev.hostapp.desktop.controller.MainController;
import dev.hostapp.desktop.controller.RegisterController;
import dev.hostapp.desktop.dto.auth.LoginResponse;
import dev.hostapp.desktop.service.AuthService;
import dev.hostapp.desktop.service.RequestService;
import dev.hostapp.desktop.service.ServerService;
import dev.hostapp.desktop.util.Dialogs;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;

public class App extends Application {

    private Stage stage;
    private LoginResponse user;
    private ApiClient client;
    private AuthService authService;
    private RequestService requestService;
    private ServerService serverService;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        client = new ApiClient(System.getProperty("hostapp.api", "http://localhost:8080"));
        authService = new AuthService(new AuthApi(client));
        requestService = new RequestService(new ServerRequestApi(client));
        serverService = new ServerService(new ServerApi(client));
        stage.setTitle("Host App");
        stage.setMinWidth(620);
        stage.setMinHeight(480);
        showLogin();
        stage.show();
    }

    public void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/dev/hostapp/desktop/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setup(this);
            showScene(root, 700, 520);
        } catch (IOException exception) {
            Dialogs.error(stage, "Не удалось открыть форму входа: " + exception.getMessage());
        }
    }

    public void showRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/dev/hostapp/desktop/register.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setup(this);
            showScene(root, 700, 580);
        } catch (IOException exception) {
            Dialogs.error(stage, "Не удалось открыть регистрацию: " + exception.getMessage());
        }
    }

    public void showMain(LoginResponse user) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/dev/hostapp/desktop/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            this.user = user;
            showScene(root, 1100, 700);
            controller.setup(this);
        } catch (IOException exception) {
            Dialogs.error(stage, "Не удалось открыть главное окно: " + exception.getMessage());
        }
    }

    private void showScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(App.class.getResource("/dev/hostapp/desktop/style.css").toExternalForm());
        stage.setScene(scene);
    }

    public void logout() {
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof Stage child && child.getOwner() == stage) {
                child.hide();
            }
        }
        client.clearSession();
        user = null;
        showLogin();
    }

    public void handleError(Throwable error) {
        if (error instanceof ApiException apiError) {
            if (apiError.getStatusCode() == 401 && user != null) {
                logout();
            }
            Dialogs.error(stage, apiError.getMessage());
        } else if (error instanceof ConnectException || error instanceof HttpTimeoutException) {
            Dialogs.error(stage, "Нет соединения с backend. Проверьте его запуск и адрес подключения.");
        } else if (error instanceof IOException) {
            Dialogs.error(stage, "Не удалось прочитать или сохранить данные. Проверьте соединение и доступ к файлу.");
        } else if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            Dialogs.error(stage, error.getMessage());
        } else if (error instanceof InterruptedException) {
            Dialogs.error(stage, "Операция была прервана. Повторите её.");
        } else {
            Dialogs.error(stage, "Не удалось выполнить операцию. Обновите данные и повторите попытку.");
        }
    }

    public Stage getStage() {
        return stage;
    }

    public LoginResponse getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user != null && user.email().equalsIgnoreCase("ADMIN");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public RequestService getRequestService() {
        return requestService;
    }

    public ServerService getServerService() {
        return serverService;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
