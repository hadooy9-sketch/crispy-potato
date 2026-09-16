module com.example {
    requires javafx.controls;
    requires javafx.fxml;

    opens qasystem to javafx.fxml;
    exports qasystem;
}
