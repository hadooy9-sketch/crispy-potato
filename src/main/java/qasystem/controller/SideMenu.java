package qasystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import qasystem.util.Shortcut;
import qasystem.util.ShortcutHints;

/**
 * The "three lines" menu button in the top bar, and the settings panel that slides in
 * from the right when it is clicked. The panel opens INSIDE the main window, on top of
 * the current screen, the same way the Add Course form does.
 *
 * App calls attachTo() for every screen it shows; screens without a top bar are skipped.
 */
public final class SideMenu {

    private static final double PANEL_WIDTH = 370;
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

    /** The "?" key: open the menu straight on its "Keyboard shortcuts" page. */
    public void openShortcuts(Scene scene) {
        open(scene, true);
    }

    private void open(Scene scene) {
        open(scene, false);
    }

    private void open(Scene scene, boolean startOnShortcuts) {
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

        // Esc closes the menu. (closeLater is filled in below, once "close" exists.)
        Runnable[] closeLater = new Runnable[1];
        EventHandler<KeyEvent> escape = event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                closeLater[0].run();
            }
        };

        // Takes the menu away right now and gives the window its screen back.
        Runnable removeNow = () -> {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, escape);
            if (scene.getRoot() == layers) { // only if nobody has switched screens meanwhile
                layers.getChildren().clear();
                scene.setRoot(screen);
            }
        };
        // Closing without going anywhere: slide the panel out first, then remove it.
        Runnable close = () -> {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, escape); // a second Esc does nothing
            slide(panel, dim, false, removeNow);
        };
        closeLater[0] = close;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, escape);
        dim.setOnMouseClicked(event -> close.run()); // click outside the panel to close it

        // Going to another screen: remove the menu at once, THEN switch screens.
        // (Before, the slide-out finished after the new screen was shown and put the OLD
        // screen back on top. That was the "flash" after Log out, and the old screen
        // no longer worked because nobody was logged in.)
        Consumer<Runnable> thenGo = action -> {
            removeNow.run();
            action.run();
        };

        if (startOnShortcuts) {
            showShortcuts(panel, scene, close, thenGo);
        } else {
            showMainMenu(panel, scene, close, thenGo);
        }
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
            go.getChildren().add(item("Home", Shortcut.HOME,
                    () -> thenGo.accept(() -> navigator.goTo(Screen.homeFor(role)))));
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

        VBox help = section("Help");
        help.getChildren().add(item("Keyboard shortcuts", Shortcut.SHOW_SHORTCUTS,
                () -> showShortcuts(panel, scene, close, thenGo)));

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

        panel.getChildren().setAll(header, go, preferences, help, account, spacer, about);
    }

    /**
     * Every shortcut, grouped by who it is for. Built from the Shortcut list,
     * so a new shortcut shows up here without changing this method.
     */
    private void showShortcuts(VBox panel, Scene scene, Runnable close, Consumer<Runnable> thenGo) {
        Button back = new Button("\u2190  Menu");
        back.getStyleClass().add("side-back");
        back.setOnAction(event -> showMainMenu(panel, scene, close, thenGo));

        Label title = new Label("Keyboard shortcuts");
        title.getStyleClass().add("dialog-title");
        Label hint = new Label("Letter keys don't work while you are typing in a box. "
                + "Press Esc to leave the box first.");
        hint.getStyleClass().add("side-hint");
        hint.setWrapText(true);
        hint.setMinHeight(Region.USE_PREF_SIZE);

        Role role = userService.getCurrentRole();
        VBox groups = new VBox(18);
        for (Shortcut.Group group : Shortcut.Group.values()) {
            boolean mine = role != null && group.name().equals(role.name());
            VBox section = section(group.displayName() + (mine ? "  \u00b7  your role" : ""));
            for (Shortcut shortcut : Shortcut.values()) {
                if (shortcut.group() == group) {
                    section.getChildren().add(shortcutRow(shortcut));
                }
            }
            groups.getChildren().add(section);
        }

        ScrollPane scroll = new ScrollPane(groups);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().setAll(back, title, hint, scroll);
    }

    /** One line of the shortcuts page:  [D] / [Enter]   Open / one-line explanation. */
    private static HBox shortcutRow(Shortcut shortcut) {
        HBox keys = ShortcutHints.keyBadges(shortcut.keys());
        keys.setMinWidth(116);
        keys.setPrefWidth(116);

        Label name = new Label(shortcut.title());
        name.getStyleClass().add("shortcut-row-title");
        Label text = new Label(shortcut.description());
        text.getStyleClass().add("shortcut-row-text");
        text.setWrapText(true);
        text.setMinHeight(Region.USE_PREF_SIZE);

        VBox words = new VBox(2, name, text);
        words.setMinWidth(0);
        HBox.setHgrow(words, Priority.ALWAYS);

        HBox row = new HBox(12, keys, words);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("shortcut-row");
        return row;
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

    /**
     * A menu item with its shortcut on the right, like a Word menu:  Home ........ [H]
     * Hovering shows the Word-style tooltip too.
     */
    private static Button item(String text, Shortcut shortcut, Runnable action) {
        Button button = item(text, action);
        Label label = new Label(text);
        label.getStyleClass().add("side-item-text");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(8, label, gap, ShortcutHints.keyBadges(shortcut.keys()));
        row.setAlignment(Pos.CENTER_LEFT);
        // As wide as the button minus its padding (12 left + 12 right), so the key sits at the right edge.
        row.prefWidthProperty().bind(button.widthProperty().subtract(24));
        button.setGraphic(row);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(ShortcutHints.tooltip(shortcut.title(), shortcut.keys(), shortcut.description()));
        return button;
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
