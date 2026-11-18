package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import qasystem.service.UserService;
import qasystem.util.Birds;
import qasystem.util.Navigator;
import qasystem.util.Screen;

/**
 * Coordinates the login screen: gathers input, asks UserService,
 * then navigates to the right screen. No business logic.
 */
public class LoginController {

    private final UserService userService;
    private final Navigator navigator;

    @FXML
    private StackPane authRoot;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    public LoginController(UserService userService, Navigator navigator) {
        this.userService = userService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        Ui.collapseWhenEmpty(messageLabel);
        Birds.addTo(authRoot); // a few small birds fly across the landscape behind the card
        PasswordReveal.addTo(passwordField); // the eye button that shows the password
    }

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter a username and password.");
            return;
        }

        if (!userService.login(username, password)) {
            messageLabel.setText("Invalid username or password.");
            passwordField.clear();
            return;
        }

        if (userService.needsRoleSelection()) {
            navigator.goTo(Screen.ROLE_SELECT);
        } else {
            navigator.goTo(Screen.homeFor(userService.getCurrentRole()));
        }
    }

    @FXML
    private void goToRegister() {
        navigator.goTo(Screen.REGISTER);
    }
}
