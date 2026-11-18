package qasystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.Consumer;

import qasystem.model.Role;
import qasystem.model.User;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;

/**
 * The "three lines" menu button in the top bar, and the settings panel that slides in
 * from the right when it is clicked. The panel opens INSIDE the main window, on top of
 * the current screen, the same way the Add Course form does.
 *
 * App calls attachTo() for every screen it shows; screens without a top bar are skipped.
 */
public final class SideMenu {

    private static final double PANEL_WIDTH = 340;
    private static final Duration SLIDE_TIME = Duration.millis(260);

    /** The dark colors live in their own stylesheet, added on top of app.css when dark mode is on. */
    private static final String DARK_STYLESHEET =
            SideMenu.class.getResource("/qasystem/dark.css").toExternalForm();

    private final UserService userService;
    private final Navigator navigator;

    public SideMenu(UserService userService, Navigator navigator) {
        this.userService = userService;
        this.navigator = navigator;
    }

    /** Adds the menu button to the right end of the screen's top bar (if it has one). */
    public void attachTo(Parent screen) {
        Node bar = screen.lookup(".top-bar");
        if (!(bar instanceof HBox) || userService.getCurrentUser() == null) {
            return; // sign-in screens have no top bar and nobody is logged in yet
        }
        Button menuButton = new Button();
        menuButton.setGraphic(threeLines());
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnAction(event -> open(menuButton.getScene()));
        ((HBox) bar).getChildren().add(menuButton);
    }

    /** Three short rounded bars stacked on top of each other: the familiar "menu" icon. */
    private static Node threeLines() {
        VBox lines = new VBox(4);
        lines.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Region line = new Region();
            line.getStyleClass().add("menu-line");
            line.setPrefSize(20, 2.5);
            line.setMaxSize(20, 2.5);
            lines.getChildren().add(line);
        }
        return lines;
    }

    // =====================================================================
    // Opening and closing the panel
    // =====================================================================

    private void open(Scene scene) {
        Parent screen = scene.getRoot();

        Region dim = new Region();
        dim.getStyleClass().add("overlay-dim");

        VBox panel = new VBox();
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(PANEL_WIDTH);
        panel.setMaxWidth(PANEL_WIDTH);
        StackPane.setAlignment(panel, Pos.CENTER_RIGHT);

        // Same trick as Dialogs.form(): stack the screen, a dim layer and the panel.
        StackPane layers = new StackPane();
        scene.setRoot(layers);
        layers.getChildren().addAll(screen, dim, panel);

        // Takes the menu away right now and gives the window its screen back.
        Runnable removeNow = () -> {
            if (scene.getRoot() == layers) { // only if nobody has switched screens meanwhile
                layers.getChildren().clear();
                scene.setRoot(screen);
            }
        };
        // Closing without going anywhere: slide the panel out first, then remove it.
        Runnable close = () -> slide(panel, dim, false, removeNow);
        dim.setOnMouseClicked(event -> close.run()); // click outside the panel to close it

        // Going to another screen: remove the menu at once, THEN switch screens.
        // (Before, the slide-out finished after the new screen was shown and put the OLD
        // screen back on top. That was the "flash" after Log out, and the old screen
        // no longer worked because nobody was logged in.)
        Consumer<Runnable> thenGo = action -> {
            removeNow.run();
            action.run();
        };

        showMainMenu(panel, scene, close, thenGo);
        slide(panel, dim, true, () -> { });
    }

    /** Slides the panel in from (or out to) the right edge while the dim layer fades. */
    private static void slide(Node panel, Node dim, boolean in, Runnable then) {
        TranslateTransition move = new TranslateTransition(SLIDE_TIME, panel);
        move.setFromX(in ? PANEL_WIDTH : 0);
        move.setToX(in ? 0 : PANEL_WIDTH);

        FadeTransition fade = new FadeTransition(SLIDE_TIME, dim);
        fade.setFromValue(in ? 0 : 1);
        fade.setToValue(in ? 1 : 0);

        ParallelTransition both = new ParallelTransition(move, fade);
        both.setInterpolator(Interpolator.EASE_BOTH);
        both.setOnFinished(event -> then.run());
        both.play();
    }

    // =====================================================================
    // What the panel shows
    // =====================================================================

    private void showMainMenu(VBox panel, Scene scene, Runnable close, Consumer<Runnable> thenGo) {
        User user = userService.getCurrentUser();
        Role role = userService.getCurrentRole();

        // ----- Who is logged in -----
        Label avatar = new Label(user.getUsername().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("avatar");
        Label name = new Label(user.getUsername());
        name.getStyleClass().add("side-name");
        Label roleLabel = new Label(role == null ? "No role picked yet" : role.displayName());
        roleLabel.getStyleClass().add("side-role");
        VBox who = new VBox(2, name, roleLabel);
        HBox header = new HBox(14, avatar, who);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("side-header");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox go = section("Go to");
        if (role != null) {
            go.getChildren().add(item("Home", () -> thenGo.accept(() -> navigator.goTo(Screen.homeFor(role)))));
        }
        if (role == Role.ADMIN) {
            go.getChildren().add(item("Invitations", () -> thenGo.accept(() -> navigator.goTo(Screen.INVITATIONS))));
        }
        if (user.getRoles().size() > 1) {
            go.getChildren().add(item("Switch role", () -> thenGo.accept(() -> navigator.goTo(Screen.ROLE_SELECT))));
        }

        VBox preferences = section("Preferences");
        ToggleButton darkMode = new ToggleButton();
        darkMode.getStyleClass().addAll("side-item", "side-toggle");
        darkMode.setMaxWidth(Double.MAX_VALUE);
        darkMode.setSelected(scene.getStylesheets().contains(DARK_STYLESHEET));
        updateDarkLabel(darkMode);
        darkMode.setOnAction(event -> {
            if (darkMode.isSelected()) {
                scene.getStylesheets().add(DARK_STYLESHEET);
                scene.setFill(Color.web("#1C1416")); // window color behind fades = dark background
            } else {
                scene.getStylesheets().remove(DARK_STYLESHEET);
                scene.setFill(Color.web("#FFD7B0"));
            }
            updateDarkLabel(darkMode);
        });
        preferences.getChildren().add(darkMode);

        VBox account = section("Account");
        account.getChildren().add(item("Log out", () -> thenGo.accept(() -> {
            userService.logout();
            navigator.goTo(Screen.LOGIN);
        })));
        Button delete = item("Delete account", () -> showDeleteConfirm(panel, scene, close, thenGo));
        delete.getStyleClass().add("side-item-danger");
        account.getChildren().add(delete);

        Label about = new Label("Q&A System  ·  CSE 360 team project");
        about.getStyleClass().add("side-about");

        panel.getChildren().setAll(header, go, preferences, account, spacer, about);
    }

    /** Deleting cannot be undone, so the panel asks first. */
    private void showDeleteConfirm(VBox panel, Scene scene, Runnable close, Consumer<Runnable> thenGo) {
        Label title = new Label("Delete your account?");
        title.getStyleClass().add("dialog-title");
        title.setWrapText(true);
        Label text = new Label("Your account will be removed and you will be logged out. "
                + "Questions and answers you posted stay. This cannot be undone.");
        text.setWrapText(true);
        text.getStyleClass().add("side-text");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        Ui.collapseWhenEmpty(error);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> showMainMenu(panel, scene, close, thenGo));
        Button confirm = new Button("Delete");
        confirm.getStyleClass().add("danger-filled-button");
        confirm.setOnAction(event -> {
            try {
                userService.deleteCurrentAccount();
            } catch (IllegalStateException e) {
                error.setText(e.getMessage()); // e.g. the only admin can't delete themselves
                return;
            }
            thenGo.accept(() -> navigator.goTo(userService.needsFirstUser() ? Screen.FIRST_USER : Screen.LOGIN));
        });
        HBox buttons = new HBox(10, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(14, title, text, error, buttons);
        box.getStyleClass().add("side-confirm");
        panel.getChildren().setAll(box);
    }

    private static void updateDarkLabel(ToggleButton toggle) {
        toggle.setText(toggle.isSelected() ? "Dark mode: On" : "Dark mode: Off");
    }

    private static VBox section(String title) {
        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("side-section-title");
        VBox box = new VBox(4, label);
        box.getStyleClass().add("side-section");
        return box;
    }

    private static Button item(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("side-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> action.run());
        return button;
    }
}
