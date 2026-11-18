package qasystem.controller;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Builds the clickable cards used on the home, course, and reviewer screens.
 * Each card is a real Button (so it gets hover, focus, keyboard, and click for free)
 * with a VBox as its graphic. All looks come from app.css.
 */
final class Cards {

    static final double WIDTH = 272;
    static final double HEIGHT = 170;
    private static final double PADDING = 20;

    private Cards() {
        // utility class - never instantiated
    }

    /** A card with a big title, a subtitle, and small stats at the bottom. */
    static Button card(String title, String subtitle, String stats, Runnable onOpen) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("card-subtitle");
        subtitleLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label statsLabel = new Label(stats);
        statsLabel.getStyleClass().add("card-stats");
        statsLabel.setWrapText(true);

        // Wrapped labels must be allowed their full height, or JavaFX cuts them off with "...".
        for (Label label : new Label[] {titleLabel, subtitleLabel, statsLabel}) {
            label.setMinHeight(Region.USE_PREF_SIZE);
        }

        VBox content = new VBox(6, titleLabel, subtitleLabel, spacer, statsLabel);
        content.setAlignment(Pos.TOP_LEFT);
        return button(content, onOpen);
    }

    /** The dashed "+" card admins use to add a course or chapter. */
    static Button addCard(String text, Runnable onAdd) {
        Label plus = new Label("+");
        plus.getStyleClass().add("add-card-plus");
        Label label = new Label(text);
        label.getStyleClass().add("add-card-text");

        VBox content = new VBox(2, plus, label);
        content.setAlignment(Pos.CENTER);

        Button card = button(content, onAdd);
        card.getStyleClass().add("add-card");
        return card;
    }

    /** Muted text shown where cards would be. */
    static Label emptyMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-message");
        label.setWrapText(true);
        return label;
    }

    private static Button button(VBox content, Runnable action) {
        content.setPrefSize(WIDTH - 2 * PADDING, HEIGHT - 2 * PADDING);
        content.setMinSize(WIDTH - 2 * PADDING, HEIGHT - 2 * PADDING);

        Button card = new Button();
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setMnemonicParsing(false);
        card.setPrefSize(WIDTH, HEIGHT);
        card.setMinSize(WIDTH, HEIGHT);
        card.getStyleClass().add("card");
        card.setOnAction(event -> action.run());
        return card;
    }
}
