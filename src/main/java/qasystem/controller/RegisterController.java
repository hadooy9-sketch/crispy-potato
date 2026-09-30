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
 * Coordinates the Create Account screen.
 * Checks the form; UserService decides the account rules and roles.
 */
public class RegisterController {

    private final UserService userService;
    private final Navigator navigator;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private VBox passwordRulesBox;
    @FXML private Label matchLabel;
    @FXML private TextField inviteCodeField;
    @FXML private Label messageLabel;

    public RegisterController(UserService userService, Navigator navigator) {
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
            userService.registerUser(username, password, inviteCodeField.getText());
            navigator.goTo(Screen.LOGIN);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void backToLogin() {
        navigator.goTo(Screen.LOGIN);
    }
}
