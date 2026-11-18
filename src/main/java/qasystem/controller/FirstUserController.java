package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;

/**
 * Coordinates the first-user setup screen.
 * The form check (passwords match) lives here;
 * the account rules live in UserService / PasswordPolicy.
 */
public class FirstUserController {

    private final UserService userService;
    private final Navigator navigator;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private VBox passwordRulesBox;
    @FXML private Label matchLabel;
    @FXML private Label messageLabel;

    public FirstUserController(UserService userService, Navigator navigator) {
        this.userService = userService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        Ui.collapseWhenEmpty(messageLabel);
        new PasswordChecklist(passwordRulesBox, passwordField, confirmPasswordField, matchLabel);
    }

    @FXML
    private void createAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!password.equals(confirmPasswordField.getText())) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            userService.registerFirstUser(username, password);
            // Spec: after setup, the user must log in again.
            navigator.goTo(Screen.LOGIN);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }
}
