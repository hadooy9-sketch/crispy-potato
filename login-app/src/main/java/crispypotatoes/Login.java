package crispypotatoes;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class Login extends Application {
    @Override
    public void start(Stage stage) {

        StackPane Pane = new StackPane();
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4b6cb7")), new Stop(1, Color.web("#182848")));
        Pane.setBackground(new Background(new BackgroundFill(gradient, null, null)));
        Pane.setPrefWidth(400);
        Pane.setPrefHeight(250);

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label titleLabel = new Label("Student Q&A Login");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-text-align: center; -fx-text-fill: white; -fx-font-weight: bold;");

        box.getChildren().addAll(titleLabel, box);
        box.getChildren().addAll(Pane);

        ///////////////
        
        Label welcomeLabel = new Label("Welcome to the Student Q&A System");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        welcomeLabel.setTextFill(Color.web("#888888"));

        Label instructionLabel = new Label("Please enter your username and password to log in.");
        instructionLabel.setFont(new Font("Arial", 14));   
        instructionLabel.setTextFill(Color.web("#888888"));

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle
            ("-fx-padding: 10; -fx-background-radius: 6;"+"-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-border-width: 1;");
        //add style

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Label messageLabel = new Label();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle
            ("-fx-padding: 10; -fx-background-radius: 6;"+"-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-border-width: 1;");
        //add style

        ///////////////

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

//Run Command: mvn javafx:run