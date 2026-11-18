module qasystem  {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;   // JDBC: talking to the database

    opens qasystem to javafx.fxml;
    opens qasystem.controller to javafx.fxml;
    exports qasystem;
}
