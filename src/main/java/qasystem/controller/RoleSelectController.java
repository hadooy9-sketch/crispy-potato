package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import qasystem.model.Role;
import qasystem.model.User;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;

/**
 * Shown to users with more than one role: one card per role they have.
 */
public class RoleSelectController {

    private final UserService userService;
    private final Navigator navigator;

    @FXML private Label welcomeLabel;
    @FXML private FlowPane roleBox;

    public RoleSelectController(UserService userService, Navigator navigator) {
        this.userService = userService;
        this.navigator = navigator;
    }

    /** Called by FXMLLoader AFTER the @FXML fields are filled in. */
    @FXML
    private void initialize() {
        User user = userService.getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getUsername());

        for (Role role : user.getRoles()) {
            roleBox.getChildren().add(
                    Cards.card(role.displayName(), describe(role), "Continue \u2192", () -> chooseRole(role)));
        }
    }

    /** Screen text only - what each role does. */
    private static String describe(Role role) {
        switch (role) {
            case ADMIN:
                return "Manage courses, chapters, questions, and invitations";
            case STUDENT:
                return "Ask questions and propose answers";
            case REVIEWER:
                return "Review answers so the whole class benefits";
        }
        return "";
    }

    private void chooseRole(Role role) {
        userService.selectRole(role);
        navigator.goTo(Screen.homeFor(role));
    }

    @FXML
    private void logout() {
        userService.logout();
        navigator.goTo(Screen.LOGIN);
    }
}
