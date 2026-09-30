package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.model.Question;
import qasystem.service.CourseService;
import qasystem.service.QuestionService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

import java.util.List;

/**
 * Ask a question. Shows similar existing questions live while typing (spec),
 * so students check before asking.
 */
public class AskQuestionController {

    private final CourseService courseService;
    private final QuestionService questionService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Label courseLabel;
    @FXML private ComboBox<Chapter> chapterChoice;
    @FXML private TextField titleField;
    @FXML private TextArea bodyArea;
    @FXML private Label relatedHeader;
    @FXML private ListView<Question> relatedList;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;

    private Course course;

    public AskQuestionController(CourseService courseService, QuestionService questionService,
                                 ViewContext viewContext, Navigator navigator) {
        this.courseService = courseService;
        this.questionService = questionService;
        this.viewContext = viewContext;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        Ui.collapseWhenEmpty(messageLabel);
        course = courseService.findCourse(viewContext.getSelectedCourseCode());
        if (course == null) {
            courseLabel.setText("This course no longer exists.");
            submitButton.setDisable(true);
            return;
        }
        courseLabel.setText(course.getCode() + "   \u203a   " + course.getTitle() + "   \u203a   Ask a question");

        // ComboBox needs BOTH: rows in the drop-down, and the closed box itself.
        chapterChoice.setCellFactory(list -> new ChapterCell());
        chapterChoice.setButtonCell(new ChapterCell());
        chapterChoice.getItems().setAll(course.getChapters());
        preselectChapter();
        if (chapterChoice.getItems().isEmpty()) {
            messageLabel.setText("This course has no chapters yet, so questions cannot be asked.");
            submitButton.setDisable(true);
        }

        relatedList.setCellFactory(list -> new QuestionCell());
        relatedList.setPlaceholder(new Label("No similar questions found."));
        relatedList.getSelectionModel().selectedItemProperty().addListener((observable, oldQ, question) -> {
            if (question != null) {
                openQuestion(question);
            }
        });

        // Live search: re-run on every change to the title or details.
        titleField.textProperty().addListener((observable, oldText, newText) -> refreshRelated());
        bodyArea.textProperty().addListener((observable, oldText, newText) -> refreshRelated());
        refreshRelated();
    }

    /** Start on the chapter tab the student came from, else the first chapter. */
    private void preselectChapter() {
        Integer wanted = viewContext.getSelectedChapterNumber();
        for (Chapter chapter : chapterChoice.getItems()) {
            if (wanted != null && chapter.getNumber() == wanted) {
                chapterChoice.getSelectionModel().select(chapter);
                return;
            }
        }
        chapterChoice.getSelectionModel().selectFirst();
    }

    private void refreshRelated() {
        String title = titleField.getText();
        String body = bodyArea.getText();
        if (title.isBlank() && body.isBlank()) {
            relatedHeader.setText("Start typing - similar questions appear here as you type.");
            relatedList.getItems().clear();
            return;
        }
        List<Question> related = questionService.findRelatedQuestions(course.getCode(), title, body);
        relatedHeader.setText(related.isEmpty()
                ? "No similar questions yet."
                : "Similar questions (" + related.size() + ") - is yours already here?");
        relatedList.getItems().setAll(related);
    }

    @FXML
    private void submit() {
        Chapter chapter = chapterChoice.getValue();
        if (chapter == null) {
            messageLabel.setText("Choose a chapter.");
            return;
        }
        try {
            Question question = questionService.askQuestion(
                    course.getCode(), chapter.getNumber(), titleField.getText(), bodyArea.getText());
            openQuestion(question);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    private void openQuestion(Question question) {
        viewContext.selectQuestion(question.getId());
        navigator.goTo(Screen.QUESTION_DETAIL);
    }

    @FXML
    private void cancel() {
        navigator.goTo(Screen.CHAPTER_VIEW);
    }

    private static class ChapterCell extends ListCell<Chapter> {
        @Override
        protected void updateItem(Chapter chapter, boolean empty) {
            super.updateItem(chapter, empty);
            setText(empty || chapter == null ? null
                    : "Chapter " + chapter.getNumber() + ": " + chapter.getTitle());
        }
    }
}
