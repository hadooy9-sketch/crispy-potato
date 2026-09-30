package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import qasystem.model.Invitation;
import qasystem.model.Role;
import qasystem.service.InvitationService;
import qasystem.util.Navigator;
import qasystem.util.Screen;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Admin screen for one-time invite codes (moved out of the old admin home).
 */
public class InvitationsController {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final InvitationService invitationService;
    private final Navigator navigator;

    @FXML private CheckBox adminCheck;
    @FXML private CheckBox studentCheck;
    @FXML private CheckBox reviewerCheck;
    @FXML private Spinner<Integer> daysSpinner;
    @FXML private TextField codeField;
    @FXML private Button copyButton;
    @FXML private Label detailsLabel;
    @FXML private Label messageLabel;

    public InvitationsController(InvitationService invitationService, Navigator navigator) {
        this.invitationService = invitationService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        Ui.collapseWhenEmpty(messageLabel);
        copyButton.setDisable(true);
    }

    @FXML
    private void generateInvite() {
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        if (adminCheck.isSelected()) {
            roles.add(Role.ADMIN);
        }
        if (studentCheck.isSelected()) {
            roles.add(Role.STUDENT);
        }
        if (reviewerCheck.isSelected()) {
            roles.add(Role.REVIEWER);
        }

        try {
            Invitation invitation = invitationService.createInvitation(roles, daysSpinner.getValue());
            String roleNames = invitation.getRoles().stream()
                    .map(Role::displayName)
                    .collect(Collectors.joining(", "));
            codeField.setText(invitation.getCode());
            detailsLabel.setText("Grants: " + roleNames
                    + "     Expires: " + invitation.getExpiresAt().format(DEADLINE_FORMAT));
            copyButton.setDisable(false);
            copyButton.setText("Copy");
            messageLabel.setText("");
        } catch (IllegalArgumentException | IllegalStateException e) {
            codeField.clear();
            detailsLabel.setText("");
            copyButton.setDisable(true);
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void copyCode() {
        ClipboardContent content = new ClipboardContent();
        content.putString(codeField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        copyButton.setText("Copied \u2713");
    }

    @FXML
    private void back() {
        navigator.goTo(Screen.ADMIN_HOME);
    }
}
