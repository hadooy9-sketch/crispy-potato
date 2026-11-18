package qasystem.controller;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import qasystem.model.Question;

import java.time.format.DateTimeFormatter;

/**
 * How a question looks in any list: a status badge and title, then details underneath.
 * Shared by the chapter page, the "similar questions" list, and the reviewer home (DRY).
 * Subclasses can change the second line by overriding metaText().
 */
class QuestionCell extends ListCell<Question> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    private final Label badge = new Label();
    private final Label title = new Label();
    private final Label meta = new Label();
    private final VBox card;

    QuestionCell() {
        badge.getStyleClass().add("badge");
        title.getStyleClass().add("question-title");
        meta.getStyleClass().add("question-meta");
        badge.setMinWidth(Region.USE_PREF_SIZE);  // never squeeze the badge into "..."
        title.setWrapText(true);                 // long titles wrap instead of being cut off
        title.setMinHeight(Region.USE_PREF_SIZE);

        HBox top = new HBox(10, badge, title);
        top.setAlignment(Pos.CENTER_LEFT);
        card = new VBox(6, top, meta);
        card.getStyleClass().add("question-card");

        getStyleClass().add("question-cell");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Question question, boolean empty) {
        super.updateItem(question, empty);
        setText(null);
        if (empty || question == null) {
            setGraphic(null); // reused cell: clear it
            return;
        }
        // Make the card as wide as the list (minus room for the scroll bar).
        if (getListView() != null && !card.prefWidthProperty().isBound()) {
            card.prefWidthProperty().bind(getListView().widthProperty().subtract(30));
        }

        badge.setText(question.isResolved() ? "RESOLVED" : "OPEN");
        badge.getStyleClass().removeAll("badge-open", "badge-resolved");
        badge.getStyleClass().add(question.isResolved() ? "badge-resolved" : "badge-open");
        title.setText(question.getTitle());
        meta.setText(metaText(question));
        setGraphic(card);
    }

    /** The line under the title. */
    protected String metaText(Question question) {
        return "Ch. " + question.getChapterNumber()
                + "   \u00b7   asked by " + question.getAuthor()
                + " \u00b7 " + Ui.count(question.getAnswers().size(), "answer")
                + " \u00b7 " + question.getCreatedAt().format(DATE_FORMAT);
    }
}
