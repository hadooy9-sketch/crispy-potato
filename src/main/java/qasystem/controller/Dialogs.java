package qasystem.controller;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * Dialog helpers shared by controllers (DRY), styled with app.css.
 * Package-private: only controllers use it.
 */
final class Dialogs {

    private static final URL STYLESHEET = Dialogs.class.getResource("/qasystem/app.css");

    private Dialogs() {
        // utility class - never instantiated
    }

    /** Shows an OK/Cancel dialog. Returns true only if OK was pressed. */
    static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.setTitle("Please confirm");
        style(alert.getDialogPane());
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("primary-button");
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    /**
     * A small form in a dialog (e.g. "Add a Course").
     * onSubmit calls a service. If the service rejects the input, the message is shown
     * and the dialog STAYS OPEN so the user can fix it. Returns true once it succeeded.
     */
    static boolean form(String title, Node content, String submitText, Runnable onSubmit) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        ButtonType submit = new ButtonType(submitText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        Ui.collapseWhenEmpty(error);

        VBox body = new VBox(12, heading, content, error);
        body.setPrefWidth(440);
        dialog.getDialogPane().setContent(body);
        style(dialog.getDialogPane());

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submit);
        submitButton.getStyleClass().add("primary-button");
        // An event filter runs BEFORE the dialog closes; consume() cancels the close.
        submitButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                onSubmit.run();
            } catch (IllegalArgumentException | IllegalStateException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });

        return dialog.showAndWait().filter(button -> button == submit).isPresent();
    }

    private static void style(DialogPane pane) {
        if (STYLESHEET != null) {
            pane.getStylesheets().add(STYLESHEET.toExternalForm());
        }
        pane.getStyleClass().add("app-dialog");
    }
}
