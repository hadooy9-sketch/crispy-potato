package crispypotatoes;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;

public class Login extends Application {
    @Override
    public void start(Stage stage) {

        Label titleLabel = new Label("Student Q&A Login");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setAlignment(Pos.CENTER);
        // add style
        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        //add style

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Label messageLabel = new Label();
        
        Button loginButton = new Button("Login");
        loginButton.setOnMouseEntered (e -> loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"));
        loginButton.setOnMouseExited (e -> loginButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black;"));

        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setPadding(new Insets(30));
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        //add style

        gridPane.add(titleLabel, 0, 0, 2, 1);
        gridPane.add(usernameLabel, 0, 1);
        gridPane.add(usernameField, 1, 1);
        gridPane.add(passwordLabel, 0, 2);
        gridPane.add(passwordField, 1, 2);
        gridPane.add(loginButton, 1, 3);
        gridPane.add(messageLabel, 1, 4);

        // Set up event handling for login button
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (username.equals("admin") && password.equals("password")) {
                messageLabel.setText("Login successful!");
                messageLabel.setFont(new Font("Arial", 14));
            } else {
                messageLabel.setText("Invalid credentials. Please try again.");
                messageLabel.setFont(new Font("Arial", 14));
            }
        });

        // Create scene and set stage
        Scene scene = new Scene(gridPane, 400, 250);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}