package qasystem.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * How shortcuts are SHOWN (ShortcutManager decides what they DO):
 *  - a small key badge on the button itself, e.g.  "<- Back [Q]"
 *  - a Word-style tooltip that appears almost at once on hover:
 *        Back (Q)                      <- bold title with the key
 *        Go back to the previous page. <- one line explaining it
 */
public final class ShortcutHints {

    /** Remembers that a button already has its hints, so they are never added twice. */
    private static final String DONE_KEY = "shortcut-hints-added";

    /** Short enough to feel instant, long enough not to flicker while the mouse passes by. */
    private static final Duration SHOW_DELAY = Duration.millis(150);

    private ShortcutHints() {
        // utility class - never instantiated
    }

    /**
     * Gives every shortcut button on a screen its badge and tooltip.
     * App calls this each time it shows a screen; it finds the buttons by their style class
     * (for example "back-button"), which the FXML files set.
     */
    public static void attachAll(Parent screen) {
        for (Shortcut shortcut : Shortcut.values()) {
            if (shortcut.targetClass() == null) {
                continue;
            }
            for (Node node : screen.lookupAll("." + shortcut.targetClass())) {
                if (node instanceof Button button) {
                    mark(button, shortcut);
                }
            }
        }
    }

    /**
     * Makes a button the target of a shortcut and adds its hints.
     * Controllers call this for buttons they create in code (cards, review forms).
     */
    public static void mark(Button button, Shortcut shortcut) {
        if (shortcut.targetClass() != null && !button.getStyleClass().contains(shortcut.targetClass())) {
            button.getStyleClass().add(shortcut.targetClass());
        }
        addHints(button, shortcut.title(), shortcut.keys(), shortcut.description());
    }

    /** A chapter card on the course page: number keys 1-9 open it. */
    public static void markChapterCard(Button card, int chapterNumber) {
        card.setId(chapterCardId(chapterNumber));
        addHints(card, "Open Chapter " + chapterNumber, String.valueOf(chapterNumber),
                "Press " + chapterNumber + " on the course page to open this chapter.");
    }

    /** The id that markChapterCard gives a chapter card. ShortcutManager looks it up. */
    public static String chapterCardId(int chapterNumber) {
        return "chapter-card-" + chapterNumber;
    }

    /** Adds the Word-style tooltip, and the key badge if the button has room for one. */
    private static void addHints(Button button, String title, String keys, String description) {
        if (button.getProperties().containsKey(DONE_KEY)) {
            return;
        }
        button.getProperties().put(DONE_KEY, true);

        button.setTooltip(tooltip(title, keys, description));

        // Cards already use their graphic for their content, so only plain text buttons get a badge.
        if (button.getGraphic() == null) {
            button.setGraphic(keyBadges(keys));
            button.setContentDisplay(ContentDisplay.RIGHT); // text first, then the badge
            button.setGraphicTextGap(10);
        }
    }

    /** The tooltip: bold "Name (Key)" on top, a short explanation below - like Microsoft Word. */
    public static Tooltip tooltip(String title, String keys, String description) {
        Label heading = new Label(title + "  (" + keys + ")");
        heading.getStyleClass().add("shortcut-tip-title");

        Label text = new Label(description);
        text.getStyleClass().add("shortcut-tip-text");
        // No wrapText here: inside a tooltip a wrapping label measures itself as a tall,
        // thin column, which drew the big empty box. The descriptions are one short line anyway.

        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(new VBox(4, heading, text));
        tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tooltip.getStyleClass().add("shortcut-tip");
        tooltip.setShowDelay(SHOW_DELAY);
        tooltip.setShowDuration(Duration.INDEFINITE); // stay while the mouse is on the button
        tooltip.setHideDelay(Duration.millis(100));
        return tooltip;
    }

    /**
     * Draws keys as little keyboard keys: "D / Enter" -> [D] / [Enter].
     * Words become badges; "+", "/" and "–" stay plain text between them.
     */
    public static HBox keyBadges(String keys) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        for (String part : keys.split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            Label label = new Label(part);
            boolean joiner = part.equals("+") || part.equals("/") || part.equals("–");
            label.getStyleClass().add(joiner ? "key-joiner" : "key-badge");
            row.getChildren().add(label);
        }
        return row;
    }
}
