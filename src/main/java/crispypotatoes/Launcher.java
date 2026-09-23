package crispypotatoes;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("It works!");
        Scene window = new Scene(label, 400, 200);
        stage.setScene(window);
        stage.setTitle("JavaFX Application");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}