package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

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
 * The questions of one chapter - or, when no chapter is selected,
 * every unresolved question in the course. Students check here before asking.
 */
public class ChapterViewController {

    private final CourseService courseService;
    private final QuestionService questionService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Button askButton;
    @FXML private Button deleteChapterButton;
    @FXML private Label breadcrumbLabel;
    @FXML private Label headingLabel;
    @FXML private Label subheadingLabel;
    @FXML private ListView<Question> questionList;

    private Course course;
    private Integer chapterNumber; // null = "Unresolved"

    public ChapterViewController(CourseService courseService, QuestionService questionService,
                                 ViewContext viewContext, Navigator navigator) {
        this.courseService = courseService;
        this.questionService = questionService;
        this.viewContext = viewContext;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        course = courseService.findCourse(viewContext.getSelectedCourseCode());
        chapterNumber = viewContext.getSelectedChapterNumber();
        Ui.show(askButton, false);
        Ui.show(deleteChapterButton, false);

        if (course == null) {
            headingLabel.setText("This course no longer exists.");
            return;
        }

        List<Question> questions;
        if (chapterNumber == null) {
            breadcrumbLabel.setText(course.getCode() + "   \u203a   Unresolved");
            headingLabel.setText("Unresolved questions");
            questions = questionService.getUnresolvedQuestions(course.getCode());
            subheadingLabel.setText(Ui.count(questions.size(), "question")
                    + " still waiting for an accepted answer, oldest first.");
        } else {
            Chapter chapter = course.findChapter(chapterNumber);
            if (chapter == null) {
                headingLabel.setText("This chapter no longer exists.");
                return;
            }
            breadcrumbLabel.setText(course.getCode() + "   \u203a   Chapter " + chapterNumber);
            headingLabel.setText("Chapter " + chapterNumber + ": " + chapter.getTitle());
            questions = questionService.getQuestions(course.getCode(), chapterNumber);
            subheadingLabel.setText(Ui.count(questions.size(), "question")
                    + ". Look through them before asking - yours may already be answered.");
            Ui.show(deleteChapterButton, courseService.canManageCourses());
        }
        Ui.show(askButton, questionService.canAsk());

        questionList.setCellFactory(list -> new QuestionCell());
        questionList.setPlaceholder(new Label("No questions here yet."));
        questionList.getItems().setAll(questions);
        questionList.getSelectionModel().selectedItemProperty().addListener((observable, oldQ, question) -> {
            if (question != null) {
                viewContext.selectQuestion(question.getId());
                navigator.goTo(Screen.QUESTION_DETAIL);
            }
        });
    }

    @FXML
    private void askQuestion() {
        navigator.goTo(Screen.ASK_QUESTION);
    }

    @FXML
    private void deleteChapter() {
        if (!Dialogs.confirm("Delete chapter " + chapterNumber + " of " + course.getCode()
                + " with all of its questions?\nThis cannot be undone.")) {
            return;
        }
        courseService.deleteChapter(course.getCode(), chapterNumber);
        viewContext.selectChapter(null);
        navigator.goTo(Screen.COURSE_VIEW);
    }

    @FXML
    private void back() {
        navigator.goTo(Screen.COURSE_VIEW);
    }
}
