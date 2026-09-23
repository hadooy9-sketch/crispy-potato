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
        // Create the gradient background
        StackPane Pane = new StackPane();
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#963ec9")), new Stop(1, Color.web("#862f3c")));
        Pane.setBackground(new Background(new BackgroundFill(gradient, null, null)));
        Pane.setPrefWidth(200);
        Pane.setPrefHeight(200);
        // Holds branding title text
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        // Our big title
        Label titleLabel = new Label("Student Q&A Login");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-text-align: center; -fx-text-fill: black; -fx-font-weight: bold;");
        
        box.getChildren().addAll(titleLabel);
        box.getChildren().addAll(Pane);

        ///////////////
        
        // subtitle and instructions
        Label welcomeLabel = new Label("Welcome to the Student Q&A System");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        welcomeLabel.setTextFill(Color.web("#888888"));
        
        Label instructionLabel = new Label("Please enter your username and password to log in.");
        instructionLabel.setFont(new Font("Arial", 14));   
        instructionLabel.setTextFill(Color.web("#888888"));

        //username
        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle
            ("-fx-padding: 10; -fx-background-radius: 6;"+"-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-border-width: 1;");
        
        //passowrd
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Label messageLabel = new Label();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle
            ("-fx-padding: 10; -fx-background-radius: 6;"+"-fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-border-width: 1;");
        //add style

        ///////////////
        //bot check box + lock icon
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        CheckBox botcheckbox = new CheckBox("I am not a robot");
        botcheckbox.setStyle("-fx-text-fill: #b03d3d; -fx-font-size: 12px;");
        Label lock = new Label("\uD83D\uDEE1");
        lock.setFont(Font.font(16));
        HBox.setHgrow(lock, Priority.ALWAYS);
        buttonBox.getChildren().addAll(lock, botcheckbox);

        // base/mouse hover style for login button but doesnt really work for some reason
        Button loginButton = new Button("Login");
        loginButton.setOnMouseEntered (e -> loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"));
        loginButton.setOnMouseExited (e -> loginButton.setStyle("-fx-background-color: #3e15d1; -fx-text-fill: black;"));
        
        String base = "-fx-background-color: #4b6cb7; -fx-text-fill: white; -fx-background-radius: 6;";
        String mouseStyle = base.replace("#4b6cb7", "#3a5a9c");
        loginButton.setStyle(base);
        loginButton.setOnMouseEntered(e -> loginButton.setStyle(mouseStyle));
        loginButton.setOnMouseExited(e -> loginButton.setStyle(base));
        //Our feedback message
        Label messageLabel1 = new Label();
        messageLabel1.setFont(new Font("Arial", 14));
        messageLabel1.setWrapText(true);

        ///////
        //Runs when the button is clicked, checks empty boxes, or if the bot check is clicked
        Runnable attemptLogin = () -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            boolean isBotChecked = botcheckbox.isSelected();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel1.setText("Please enter both username and password.");
                messageLabel1.setTextFill(Color.RED);
            } else if (!isBotChecked) {
                messageLabel1.setText("Please confirm you are not a robot.");
                messageLabel1.setTextFill(Color.RED);
            } else {
                // Here you would normally check the credentials against a database or authentication service
                if (username.equals("user") && password.equals("pass")) {
                    messageLabel1.setText("Login successful!");
                    messageLabel1.setTextFill(Color.GREEN);
                } else {
                    messageLabel1.setText("Invalid username or password.");
                    messageLabel1.setTextFill(Color.RED);
                }
            }
        };
        //Form layout
        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.add(usernameLabel, 0, 0);
        gridPane.add(usernameField, 1, 0);
        gridPane.add(passwordLabel, 0, 1);
        gridPane.add(passwordField, 1, 1); 
        gridPane.add(loginButton, 1, 2);

        //make sure everuthing on the right is stacked nicely
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);

       loginButton.setOnAction(e -> attemptLogin.run());
       passwordField.setOnAction(e -> attemptLogin.run());
       card.getChildren().addAll(welcomeLabel, instructionLabel, gridPane, buttonBox, messageLabel1);

        //is in charge of the branding panel on the left and card(everything on the right)
        BorderPane root = new BorderPane();
        root.setLeft(Pane);

        StackPane loginPane = new StackPane(card);
        loginPane.setStyle("-fx-background-color: #d4d4d4; -fx-padding: 40;");
        root.setCenter(loginPane);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

//Run Command: mvn javafx:run