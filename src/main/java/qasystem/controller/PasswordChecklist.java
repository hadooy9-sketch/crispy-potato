package qasystem.controller;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import qasystem.service.PasswordPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * The live "your password needs..." box under the password field.
 * Draws its lines from PasswordPolicy - the same rules UserService enforces -
 * and ticks each one off as the user types. Also shows whether the confirmation matches.
 */
final class PasswordChecklist {

    private static final String MET = "\u2713  ";   // check mark
    private static final String UNMET = "\u2022  "; // bullet

    private final List<PasswordPolicy.Rule> rules = PasswordPolicy.rules();
    private final List<Label> ruleLabels = new ArrayList<>();
    private final PasswordField passwordField;
    private final PasswordField confirmField;
    private final Label matchLabel;

    PasswordChecklist(VBox box, PasswordField passwordField, PasswordField confirmField, Label matchLabel) {
        this.passwordField = passwordField;
        this.confirmField = confirmField;
        this.matchLabel = matchLabel;
        Ui.collapseWhenEmpty(matchLabel);

        box.getStyleClass().add("password-rules");
        Label header = new Label("Your password needs:");
        header.getStyleClass().add("password-rules-header");
        box.getChildren().add(header);

        for (PasswordPolicy.Rule rule : rules) {
            Label label = new Label();
            ruleLabels.add(label);
            box.getChildren().add(label);
        }

        passwordField.textProperty().addListener((observable, oldText, newText) -> update());
        confirmField.textProperty().addListener((observable, oldText, newText) -> update());
        update();
    }

    private void update() {
        String password = passwordField.getText();
        for (int i = 0; i < rules.size(); i++) {
            PasswordPolicy.Rule rule = rules.get(i);
            mark(ruleLabels.get(i), rule.isMetBy(password), rule.getDescription());
        }

        String confirm = confirmField.getText();
        if (confirm.isEmpty()) {
            matchLabel.setText("");
        } else if (confirm.equals(password)) {
            mark(matchLabel, true, "Passwords match");
        } else {
            mark(matchLabel, false, "Passwords do not match yet");
        }
    }

    private static void mark(Label label, boolean met, String text) {
        label.setText((met ? MET : UNMET) + text);
        label.getStyleClass().removeAll("rule-met", "rule-unmet");
        label.getStyleClass().add(met ? "rule-met" : "rule-unmet");
    }
}
