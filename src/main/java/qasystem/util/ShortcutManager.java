package qasystem.util;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What every keyboard shortcut DOES. (The list of shortcuts is in Shortcut;
 * how they are shown is in ShortcutHints.)
 *
 * Most keys just press a button on the current screen, found by its style class.
 * That way each screen keeps its OWN logic (where Back goes, what Ask opens) and
 * nothing is copied here. Keys that are not a button (Home, Next review, the help page)
 * are passed to App through the Actions interface.
 *
 * Shortcuts are ignored when:
 *  - the user is typing in a text box (Esc leaves the text box first),
 *  - a form or the side menu is open on top of the screen,
 *  - Ctrl, Alt or the Windows/Command key is held.
 */
public final class ShortcutManager {

    /** The shortcuts that are not "press a button on this screen". App implements them. */
    public interface Actions {
        /** @return false if there is no home to go to (nobody logged in yet) */
        boolean goHome();

        /** @return false if the user is not a reviewer or nothing is waiting */
        boolean openNextReview();

        /** @return false if the menu can't be opened (nobody logged in yet) */
        boolean showShortcuts();
    }

    private final Scene scene;
    private final Supplier<Parent> currentScreen;
    private final Actions actions;

    /** Keys being held down right now. Holding a key must not repeat most actions. */
    private final Set<KeyCode> keysDown = EnumSet.noneOf(KeyCode.class);

    private ShortcutManager(Scene scene, Supplier<Parent> currentScreen, Actions actions) {
        this.scene = scene;
        this.currentScreen = currentScreen;
        this.actions = actions;
    }

    /**
     * Starts listening on the whole window.
     *
     * @param currentScreen gives the screen App is showing. If the Scene's root is something
     *                      else, a form or the side menu is covering the screen.
     */
    public static void install(Scene scene, Supplier<Parent> currentScreen, Actions actions) {
        ShortcutManager manager = new ShortcutManager(scene, currentScreen, actions);
        // Event FILTERS run before the focused control sees the key, so no list or
        // button can swallow a shortcut first.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, manager::onKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> manager.keysDown.remove(event.getCode()));
        scene.addEventFilter(KeyEvent.KEY_TYPED, manager::onKeyTyped);
        // If the window loses focus (e.g. a "Please confirm" box opens), key releases go there.
        // Forget which keys were down, or the next press would look like a held key.
        scene.windowProperty().addListener((observable, oldWindow, window) -> {
            if (window != null) {
                window.focusedProperty().addListener((o, wasFocused, focused) -> {
                    if (!focused) {
                        manager.keysDown.clear();
                    }
                });
            }
        });
    }

    // =====================================================================
    // Key handling
    // =====================================================================

    private void onKeyPressed(KeyEvent event) {
        boolean repeat = !keysDown.add(event.getCode()); // true while the key is held down
        if (event.isControlDown() || event.isMetaDown() || event.isAltDown()) {
            return; // leave Ctrl+C, Alt+F4, ... alone
        }

        Parent screen = currentScreen.get();
        if (screen == null || scene.getRoot() != screen) {
            return; // a form or the side menu is open; they handle their own keys (Esc closes them)
        }

        if (isTyping(scene.getFocusOwner())) {
            if (event.getCode() == KeyCode.ESCAPE) {
                screen.requestFocus(); // leave the text box, so letter shortcuts work again
                event.consume();
            }
            return; // otherwise the key types a letter as usual
        }

        boolean handled = switch (event.getCode()) {
            // Moving the highlight may repeat while the key is held, like scrolling.
            case W -> moveHighlight(screen, -1);
            case S -> moveHighlight(screen, +1);
            default -> !repeat && singlePress(screen, event.getCode());
        };
        if (handled) {
            event.consume(); // used up: nobody else reacts to this key
        }
    }

    /** "?" is Shift + / on most keyboards, so it is read from the typed character instead. */
    private void onKeyTyped(KeyEvent event) {
        if (!"?".equals(event.getCharacter())) {
            return;
        }
        Parent screen = currentScreen.get();
        if (screen != null && scene.getRoot() == screen && !isTyping(scene.getFocusOwner())
                && actions.showShortcuts()) {
            event.consume();
        }
    }

    /** Keys that do something once per press. Returns true if the key was used. */
    private boolean singlePress(Parent screen, KeyCode code) {
        switch (code) {
            case Q:
                return pressButton(screen, Shortcut.BACK);
            case E:
                return pressButton(screen, Shortcut.ASK);
            case N:
                return pressButton(screen, Shortcut.ADD_NEW);
            case H:
                return actions.goHome();
            case R:
                return actions.openNextReview();
            case D:
            case ENTER:
                return openHighlighted();
            default:
                int number = digit(code);
                return number >= 1 && pressChapterCard(screen, number);
        }
    }

    // =====================================================================
    // Pressing buttons
    // =====================================================================

    /** Presses the first usable button on the screen that is marked for this shortcut. */
    private static boolean pressButton(Parent screen, Shortcut shortcut) {
        for (Node node : screen.lookupAll("." + shortcut.targetClass())) {
            if (node instanceof Button button && isUsable(button)) {
                button.fire(); // exactly like a mouse click
                return true;
            }
        }
        return false;
    }

    private static boolean pressChapterCard(Parent screen, int chapterNumber) {
        Node node = screen.lookup("#" + ShortcutHints.chapterCardId(chapterNumber));
        if (node instanceof Button card && isUsable(card)) {
            card.fire();
            return true;
        }
        return false;
    }

    // =====================================================================
    // W / S / D: moving a highlight through cards and question rows
    // =====================================================================

    /** One thing W/S can highlight: either a card, or one row of a question list. */
    private record Item(Button card, ListView<?> list, int row) {
    }

    private boolean moveHighlight(Parent screen, int step) {
        List<Item> items = new ArrayList<>();
        collectItems(screen, items);
        if (items.isEmpty()) {
            return false;
        }
        int current = indexOfHighlighted(items);
        int next;
        if (current < 0) {
            next = step > 0 ? 0 : items.size() - 1; // nothing highlighted yet: start at an end
        } else {
            next = Math.max(0, Math.min(items.size() - 1, current + step));
        }
        highlight(items.get(next));
        return true;
    }

    /** Collects cards and list rows in the order they appear on screen (top to bottom). */
    private static void collectItems(Node node, List<Item> out) {
        if (!node.isVisible()) {
            return;
        }
        if (node instanceof ListView<?> list) {
            for (int row = 0; row < list.getItems().size(); row++) {
                out.add(new Item(null, list, row));
            }
            return; // don't look inside the list itself
        }
        if (node instanceof Button button) {
            if (button.getStyleClass().contains("card") && !button.isDisabled()) {
                out.add(new Item(button, null, -1));
            }
            return;
        }
        if (node instanceof ScrollPane scrollPane) {
            if (scrollPane.getContent() != null) {
                collectItems(scrollPane.getContent(), out);
            }
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectItems(child, out);
            }
        }
    }

    private int indexOfHighlighted(List<Item> items) {
        Node focused = scene.getFocusOwner();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.card() != null && item.card() == focused) {
                return i;
            }
            if (item.list() != null && item.list() == focused
                    && item.list().getFocusModel().getFocusedIndex() == item.row()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Cards: give the card keyboard focus. List rows: give the list focus and move its
     * FOCUS (not its selection - selecting a row opens the question right away).
     */
    private static void highlight(Item item) {
        if (item.card() != null) {
            item.card().requestFocus();
            scrollIntoView(item.card());
            return;
        }
        ListView<?> list = item.list();
        list.requestFocus();
        list.getFocusModel().focus(item.row());
        if (!rowIsShown(list, item.row())) {
            list.scrollTo(item.row());
        }
        scrollIntoView(list);
    }

    /** D / Enter: open what W/S highlighted. Returns false if nothing is highlighted. */
    private boolean openHighlighted() {
        Node focused = scene.getFocusOwner();
        if (focused instanceof Button button && button.getStyleClass().contains("card")) {
            button.fire();
            return true;
        }
        if (focused instanceof ListView<?> list) {
            int row = list.getFocusModel().getFocusedIndex();
            if (row >= 0) {
                list.getSelectionModel().select(row); // the screen opens the selected question
                return true;
            }
        }
        return false;
    }

    /** True if the row is fully visible inside the list (no need to scroll). */
    private static boolean rowIsShown(ListView<?> list, int row) {
        Bounds listBounds = list.localToScene(list.getBoundsInLocal());
        for (Node node : list.lookupAll(".list-cell")) {
            if (node instanceof IndexedCell<?> cell && cell.getIndex() == row && cell.isVisible()) {
                Bounds cellBounds = cell.localToScene(cell.getBoundsInLocal());
                return cellBounds.getMinY() >= listBounds.getMinY()
                        && cellBounds.getMaxY() <= listBounds.getMaxY();
            }
        }
        return false;
    }

    /** A page's ScrollPane does not follow the keyboard focus by itself, so scroll it here. */
    private static void scrollIntoView(Node node) {
        Parent parent = node.getParent();
        while (parent != null && !(parent instanceof ScrollPane)) {
            parent = parent.getParent();
        }
        if (!(parent instanceof ScrollPane scrollPane) || scrollPane.getContent() == null) {
            return;
        }
        Node content = scrollPane.getContent();
        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewHeight = scrollPane.getViewportBounds().getHeight();
        if (contentHeight <= viewHeight) {
            return; // everything fits, nothing to scroll
        }
        Bounds box = content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        double top = scrollPane.getVvalue() * (contentHeight - viewHeight);
        double margin = 24;
        double newTop;
        if (box.getMinY() - margin < top) {
            newTop = box.getMinY() - margin;                    // above the view: scroll up
        } else if (box.getMaxY() + margin > top + viewHeight) {
            newTop = box.getMaxY() + margin - viewHeight;       // below the view: scroll down
        } else {
            return;                                              // already visible
        }
        scrollPane.setVvalue(Math.max(0, Math.min(1, newTop / (contentHeight - viewHeight))));
    }

    // =====================================================================
    // Small checks
    // =====================================================================

    /** True if the focused control is one the user types into. */
    private static boolean isTyping(Node focused) {
        if (focused instanceof TextInputControl) {
            return true; // TextField, PasswordField, TextArea (also a Spinner's text box)
        }
        return focused instanceof ComboBoxBase<?> comboBox && comboBox.isEditable();
    }

    /** Visible (with all of its parents), enabled, and on the screen. */
    private static boolean isUsable(Button button) {
        if (button.isDisabled() || button.getScene() == null) {
            return false;
        }
        for (Node node = button; node != null; node = node.getParent()) {
            if (!node.isVisible()) {
                return false; // e.g. the Ask button inside a hidden banner
            }
        }
        return true;
    }

    /** 1-9 from the number row or the number pad; 0 for any other key. */
    private static int digit(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 1;
            case DIGIT2, NUMPAD2 -> 2;
            case DIGIT3, NUMPAD3 -> 3;
            case DIGIT4, NUMPAD4 -> 4;
            case DIGIT5, NUMPAD5 -> 5;
            case DIGIT6, NUMPAD6 -> 6;
            case DIGIT7, NUMPAD7 -> 7;
            case DIGIT8, NUMPAD8 -> 8;
            case DIGIT9, NUMPAD9 -> 9;
            default -> 0;
        };
    }
}
