package qasystem.controller;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Adds an eye button inside a password field that shows or hides the password.
 *
 * How it works: a normal TextField sits on top of the PasswordField and shares the same
 * text (bindBidirectional), so typing in either one updates both. The eye button just
 * switches which of the two is visible. The controller keeps using its PasswordField
 * exactly as before.
 */
final class PasswordReveal {

    // A simple eye outline, a pupil, and the slash drawn over it while the password is visible.
    private static final String EYE = "M1 8 C3.5 3.5 6.5 2 10 2 C13.5 2 16.5 3.5 19 8 "
            + "C16.5 12.5 13.5 14 10 14 C6.5 14 3.5 12.5 1 8 Z";
    private static final String PUPIL = "M10 5 A3 3 0 1 0 10.001 5 Z";
    private static final String SLASH = "M3 15 L17 1";

    private PasswordReveal() {
        // utility class - never instantiated
    }

    /** Call from a controller's initialize(), once the field is on the screen's layout. */
    static void addTo(PasswordField passwordField) {
        Pane parent = (Pane) passwordField.getParent();
        int index = parent.getChildren().indexOf(passwordField);

        TextField plainField = new TextField();
        plainField.textProperty().bindBidirectional(passwordField.textProperty());
        plainField.promptTextProperty().bind(passwordField.promptTextProperty());
        plainField.onActionProperty().bind(passwordField.onActionProperty());
        plainField.getStyleClass().add("reveal-field");
        passwordField.getStyleClass().add("reveal-field");

        SVGPath slash = svg(SLASH, "reveal-icon-slash");
        Button eye = new Button();
        eye.setGraphic(new Group(svg(EYE, "reveal-icon"), svg(PUPIL, "reveal-icon-pupil"), slash));
        eye.getStyleClass().add("reveal-button");
        eye.setFocusTraversable(false); // Tab still goes straight from the password to the next field
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);

        StackPane box = new StackPane(passwordField, plainField, eye);
        box.setMaxWidth(Double.MAX_VALUE);
        parent.getChildren().add(index, box);

        Runnable apply = () -> {
            boolean showing = plainField.isVisible();
            passwordField.setVisible(!showing);
            slash.setVisible(showing);
            eye.setTooltip(new Tooltip(showing ? "Hide password" : "Show password"));
        };
        plainField.setVisible(false);
        apply.run();

        eye.setOnAction(event -> {
            boolean wasFocused = passwordField.isFocused() || plainField.isFocused();
            plainField.setVisible(!plainField.isVisible());
            apply.run();
            TextField shown = plainField.isVisible() ? plainField : passwordField;
            if (wasFocused) {
                shown.requestFocus();
                shown.end(); // keep the cursor at the end of what was typed
            }
        });
    }

    private static SVGPath svg(String content, String styleClass) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.getStyleClass().add(styleClass);
        return path;
    }
}
