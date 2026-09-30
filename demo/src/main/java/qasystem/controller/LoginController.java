package qasystem.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;


public class LoginController {
    
     @FXML private TextField usernameField;
     @FXML private PasswordField passwordField;
     @FXML private TextField passwordTextField;
     @FXML private Button showPasswordButton;

     @FXML
    public void initialize() {
        // Keep text in sync between PasswordField and TextField
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleShowPasswordPressed(MouseEvent event) {
        // Show plain text, hide masked password
        passwordTextField.setVisible(true);
        passwordTextField.setManaged(true);

        passwordField.setVisible(false);
        passwordField.setManaged(false);
    }

    @FXML
    private void handleShowPasswordReleased(MouseEvent event) {
        // Hide plain text, restore masked password
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        passwordField.setVisible(true);
        passwordField.setManaged(true);
    }

    @FXML
    private void login() {
    String username = usernameField.getText();
    String password = passwordField.getText();

    userService.login(username,password);
    }

}
