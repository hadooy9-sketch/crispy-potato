package qasystem.controller;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import qasystem.util.Animations;

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
     * A small form shown INSIDE the main window (e.g. "Add a Course"), on top of the current screen.
     * The screen behind is dimmed and can't be clicked until the form closes.
     * onSubmit calls a service. If the service rejects the input, the message is shown
     * and the form STAYS OPEN so the user can fix it. Once it succeeds, the form closes
     * and onSuccess runs (for example, to refresh the list of courses).
     *
     * @param owner any node on the current screen; used to find the window
     */
    static void form(Node owner, String title, Node content, String submitText,
                     Runnable onSubmit, Runnable onSuccess) {
        Scene scene = owner.getScene();
        Parent screen = scene.getRoot();

        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        Ui.collapseWhenEmpty(error);

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);   // the Esc key presses it
        Button submitButton = new Button(submitText);
        submitButton.setDefaultButton(true);  // the Enter key presses it
        submitButton.getStyleClass().add("primary-button");
        HBox buttons = new HBox(10, cancelButton, submitButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(12, heading, content, error, buttons);
        panel.getStyleClass().add("overlay-panel");
        panel.setMaxWidth(480);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        // A see-through layer over the whole screen. It also catches clicks,
        // so the screen behind can't be used while the form is open.
        Region dim = new Region();
        dim.getStyleClass().add("overlay-dim");

        // Put the current screen, the dim layer and the panel on top of each other.
        // The screen has to stop being the window's root before it can go inside the StackPane.
        StackPane layers = new StackPane();
        scene.setRoot(layers);
        layers.getChildren().addAll(screen, dim, panel);
        Animations.fadeIn(dim);
        Animations.fadeIn(panel);

        // Closing: fade the form away, then give the window its normal screen back.
        Runnable close = () -> Animations.fadeOut(panel, () -> {
            if (scene.getRoot() == layers) { // only if nobody has switched screens meanwhile
                layers.getChildren().clear();
                scene.setRoot(screen);
            }
        });

        cancelButton.setOnAction(event -> close.run());
        submitButton.setOnAction(event -> {
            try {
                onSubmit.run();
            } catch (IllegalArgumentException | IllegalStateException e) {
                error.setText(e.getMessage());
                return; // keep the form open
            }
            close.run();
            onSuccess.run();
        });

        // Put the typing cursor in the first text box, so the user can start typing right away.
        Node firstField = content.lookup(".text-field");
        if (firstField != null) {
            Platform.runLater(firstField::requestFocus);
        }
    }

    private static void style(DialogPane pane) {
        if (STYLESHEET != null) {
            pane.getStylesheets().add(STYLESHEET.toExternalForm());
        }
        pane.getStyleClass().add("app-dialog");
    }
}
