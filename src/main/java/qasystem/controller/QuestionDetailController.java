package qasystem.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import qasystem.model.Answer;
import qasystem.model.Question;
import qasystem.model.Review;
import qasystem.model.Verdict;
import qasystem.service.QuestionService;
import qasystem.service.ReviewService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One question with all its answers and their reviews.
 * What is shown (answer form, Accept buttons, review forms, Delete) is decided by asking
 * the services' can...() methods - the rules are never copied here.
 * All colors come from app.css style classes, not inline styles.
 */
public class QuestionDetailController {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final QuestionService questionService;
    private final ReviewService reviewService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Button deleteButton;
    @FXML private Label breadcrumbLabel;
    @FXML private Label statusLabel;
    @FXML private Label askerAvatar;
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label bodyLabel;
    @FXML private Label messageLabel;
    @FXML private Label answersHeader;
    @FXML private VBox answersBox;
    @FXML private Label resolvedNote;
    @FXML private VBox answerForm;
    @FXML private TextArea answerArea;
    @FXML private Label answerMessageLabel;

    public QuestionDetailController(QuestionService questionService, ReviewService reviewService,
                                    ViewContext viewContext, Navigator navigator) {
        this.questionService = questionService;
        this.reviewService = reviewService;
        this.viewContext = viewContext;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        Ui.collapseWhenEmpty(messageLabel);
        refresh();
    }

    // =====================================================================
    // Display
    // =====================================================================

    private Question currentQuestion() {
        Long id = viewContext.getSelectedQuestionId();
        return id == null ? null : questionService.findQuestion(id);
    }

    private void refresh() {
        Question question = currentQuestion();
        if (question == null) {
            titleLabel.setText("This question no longer exists.");
            for (javafx.scene.Node node : List.of(statusLabel, askerAvatar, metaLabel, bodyLabel, answersHeader,
                    answersBox, resolvedNote, answerForm, deleteButton)) {
                Ui.show(node, false);
            }
            return;
        }

        breadcrumbLabel.setText(question.getCourseCode() + "   \u203a   Chapter " + question.getChapterNumber());
        titleLabel.setText(question.getTitle());
        askerAvatar.setText(initial(question.getAuthor()));
        metaLabel.setText(question.getAuthor() + " asked"
                + "   \u00b7   " + question.getCreatedAt().format(DATE_TIME_FORMAT));

        statusLabel.setText(question.isResolved() ? "RESOLVED" : "OPEN");
        statusLabel.getStyleClass().removeAll("badge-open", "badge-resolved");
        statusLabel.getStyleClass().add(question.isResolved() ? "badge-resolved" : "badge-open");

        bodyLabel.setText(question.getBody());
        Ui.show(bodyLabel, !question.getBody().isEmpty());

        showAnswers(question);

        // Ask the services - never copy their rules into the UI.
        Ui.show(answerForm, questionService.canAnswer(question));
        Ui.show(resolvedNote, question.isResolved());
        Ui.show(deleteButton, questionService.canDelete());
    }

    private void showAnswers(Question question) {
        answersBox.getChildren().clear();

        // Accepted answer pinned to the top (a display choice, so it lives in the view).
        List<Answer> answers = new ArrayList<>(question.getAnswers());
        Answer accepted = question.getAcceptedAnswer();
        if (accepted != null) {
            answers.remove(accepted);
            answers.add(0, accepted);
        }

        answersHeader.setText(Ui.count(answers.size(), "Answer"));
        if (answers.isEmpty()) {
            answersBox.getChildren().add(Cards.emptyMessage("No answers yet. Be the first to propose one."));
            return;
        }

        boolean canResolve = questionService.canResolve(question);
        for (Answer answer : answers) {
            answersBox.getChildren().add(buildAnswerCard(question, answer, answer == accepted, canResolve));
        }
    }

    /** First letter of a name, for the round avatar. */
    private static String initial(String name) {
        return name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

    /**
     * One answer, drawn like a chat reply: a round avatar with the author's first letter,
     * then a speech bubble (its top-left corner is square, pointing at the avatar).
     */
    private HBox buildAnswerCard(Question question, Answer answer, boolean isAccepted, boolean canResolve) {
        Label meta = new Label((isAccepted ? "\u2713  ACCEPTED ANSWER   \u00b7   " : "")
                + answer.getAuthor() + "   \u00b7   " + answer.getCreatedAt().format(DATE_TIME_FORMAT));
        meta.getStyleClass().add(isAccepted ? "answer-meta-accepted" : "answer-meta");

        Label text = new Label(answer.getText());
        text.getStyleClass().add("answer-text");
        text.setWrapText(true);

        VBox card = new VBox(8, meta, text);
        card.getStyleClass().add("answer-card");
        if (isAccepted) {
            card.getStyleClass().add("answer-card-accepted");
        }
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        Label avatar = new Label(initial(answer.getAuthor()));
        avatar.getStyleClass().addAll("avatar", "avatar-small");
        if (isAccepted) {
            avatar.getStyleClass().add("avatar-accepted");
        }
        HBox row = new HBox(12, avatar, card);
        row.setAlignment(Pos.TOP_LEFT);

        if (canResolve) {
            Button acceptButton = new Button("Accept this answer");
            acceptButton.getStyleClass().add("success-button");
            acceptButton.setOnAction(event -> acceptAnswer(question, answer));
            card.getChildren().add(acceptButton);
        }

        for (Review review : answer.getReviews()) {
            card.getChildren().add(buildReviewBox(review));
        }
        if (reviewService.canReview(answer)) {
            card.getChildren().add(buildReviewForm(question, answer));
        }
        return row;
    }

    // =====================================================================
    // Reviews
    // =====================================================================

    private VBox buildReviewBox(Review review) {
        Label header = new Label("Review by " + review.getReviewer() + ":  "
                + review.getVerdict().displayName()
                + (review.isEdited() ? "   (edited)" : ""));
        header.getStyleClass().addAll("review-header", verdictClass(review.getVerdict()) + "-text");

        Label comment = new Label(review.getComment());
        comment.setWrapText(true);

        VBox box = new VBox(3, header, comment);
        box.getStyleClass().addAll("review-box", verdictClass(review.getVerdict()));
        return box;
    }

    private static String verdictClass(Verdict verdict) {
        switch (verdict) {
            case CORRECT:
                return "review-correct";
            case PARTIALLY_CORRECT:
                return "review-partial";
            case INCORRECT:
                return "review-incorrect";
        }
        return "review-box";
    }

    private VBox buildReviewForm(Question question, Answer answer) {
        Review mine = reviewService.findMyReview(answer);

        ComboBox<Verdict> verdictChoice = new ComboBox<>();
        verdictChoice.getItems().setAll(Verdict.values());
        verdictChoice.setPromptText("Verdict");
        // One converter covers both the drop-down rows and the closed box.
        verdictChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Verdict verdict) {
                return verdict == null ? "" : verdict.displayName();
            }

            @Override
            public Verdict fromString(String text) {
                return null; // only used by editable ComboBoxes
            }
        });

        TextField commentField = new TextField();
        commentField.setPromptText("Explain your verdict");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button submitButton = new Button(mine == null ? "Submit Review" : "Update Review");
        submitButton.getStyleClass().add("primary-button");
        Label reviewMessage = new Label();
        reviewMessage.getStyleClass().add("error-label");
        Ui.collapseWhenEmpty(reviewMessage);

        if (mine != null) { // prefill with the existing review
            verdictChoice.setValue(mine.getVerdict());
            commentField.setText(mine.getComment());
        }

        submitButton.setOnAction(event -> {
            try {
                reviewService.submitReview(question.getId(), answer.getId(),
                        verdictChoice.getValue(), commentField.getText());
                refresh();
            } catch (IllegalArgumentException | IllegalStateException e) {
                reviewMessage.setText(e.getMessage());
            }
        });

        Label title = new Label(mine == null ? "Review this answer" : "Your review");
        title.getStyleClass().add("review-form-title");
        HBox row = new HBox(8, verdictChoice, commentField, submitButton);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox form = new VBox(6, title, row, reviewMessage);
        form.getStyleClass().add("review-form");
        return form;
    }

    // =====================================================================
    // Actions
    // =====================================================================

    @FXML
    private void submitAnswer() {
        Question question = currentQuestion();
        if (question == null) {
            return;
        }
        try {
            questionService.proposeAnswer(question.getId(), answerArea.getText());
            answerArea.clear();
            answerMessageLabel.setText("");
            refresh();
        } catch (IllegalArgumentException | IllegalStateException e) {
            answerMessageLabel.setText(e.getMessage());
        }
    }

    private void acceptAnswer(Question question, Answer answer) {
        if (!Dialogs.confirm("Mark this answer as the one that solved your question?\n"
                + "This cannot be changed later.")) {
            return;
        }
        try {
            questionService.resolveQuestion(question.getId(), answer.getId());
            messageLabel.setText("");
            refresh();
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void deleteQuestion() {
        Question question = currentQuestion();
        if (question == null) {
            return;
        }
        if (!Dialogs.confirm("Delete this question and all of its answers?\nThis cannot be undone.")) {
            return;
        }
        try {
            questionService.deleteQuestion(question.getId());
            navigator.goTo(Screen.CHAPTER_VIEW);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void back() {
        navigator.goTo(Screen.CHAPTER_VIEW);
    }
}
