package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import qasystem.service.UserService;
import qasystem.util.Animations;
import qasystem.util.Birds;
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

    @FXML private StackPane authRoot;
    @FXML private VBox card;
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
        PasswordReveal.addTo(passwordField);        // eye buttons that show the passwords
        PasswordReveal.addTo(confirmPasswordField);
        Birds.addTo(authRoot); // a few small birds fly behind the card
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
            // First show a short welcome message, then go to the login screen.
            Animations.showMessageThen(authRoot, card, "Admin account created, welcome!",
                    () -> navigator.goTo(Screen.LOGIN));
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }
}
