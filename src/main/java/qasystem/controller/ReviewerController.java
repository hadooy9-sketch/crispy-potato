package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;

import qasystem.model.Course;
import qasystem.model.Question;
import qasystem.service.CourseService;
import qasystem.service.QuestionService;
import qasystem.service.ReviewService;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

/**
 * Reviewer home: questions with answers awaiting review, and all courses as cards.
 * Reuses the course, chapter, and question screens that students use.
 */
public class ReviewerController {

    private final UserService userService;
    private final CourseService courseService;
    private final QuestionService questionService;
    private final ReviewService reviewService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Label userLabel;
    @FXML private Label heroTitle;
    @FXML private ListView<Question> awaitingList;
    @FXML private FlowPane coursesPane;

    public ReviewerController(UserService userService, CourseService courseService,
                              QuestionService questionService, ReviewService reviewService,
                              ViewContext viewContext, Navigator navigator) {
        this.userService = userService;
        this.courseService = courseService;
        this.questionService = questionService;
        this.reviewService = reviewService;
        this.viewContext = viewContext;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        String name = userService.getCurrentUser().getUsername();
        userLabel.setText(name + "  \u00b7  Reviewer");
        heroTitle.setText("Welcome back, " + name);

        // ----- Awaiting review -----
        awaitingList.setCellFactory(list -> new AwaitingCell());
        awaitingList.setPlaceholder(new Label("Nothing to review right now. Nice work!"));
        awaitingList.getItems().setAll(reviewService.getQuestionsAwaitingReview());
        // Size the list to its rows (at most 4 visible) instead of a fixed empty box.
        int rows = Math.max(1, Math.min(4, awaitingList.getItems().size()));
        awaitingList.setPrefHeight(rows * 92 + 10);
        awaitingList.getSelectionModel().selectedItemProperty().addListener((observable, oldQ, question) -> {
            if (question != null) {
                openQuestion(question);
            }
        });

        // ----- All courses -----
        for (Course course : courseService.getAllCourses()) {
            String stats = Ui.count(course.getChapters().size(), "chapter")
                    + "  \u00b7  " + Ui.count(questionService.countQuestions(course.getCode()), "question");
            coursesPane.getChildren().add(Cards.card(course.getCode(), course.getTitle(), stats, () -> {
                viewContext.selectCourse(course.getCode());
                navigator.goTo(Screen.COURSE_VIEW);
            }));
        }
        if (coursesPane.getChildren().isEmpty()) {
            coursesPane.getChildren().add(Cards.emptyMessage("No courses yet."));
        }
    }

    /**
     * Set course, chapter AND question, so the Back buttons lead
     * to the right chapter and course.
     */
    private void openQuestion(Question question) {
        viewContext.selectCourse(question.getCourseCode());
        viewContext.selectChapter(question.getChapterNumber());
        viewContext.selectQuestion(question.getId());
        navigator.goTo(Screen.QUESTION_DETAIL);
    }

    /** Same look as every question row, but the second line says what needs reviewing. */
    private class AwaitingCell extends QuestionCell {
        @Override
        protected String metaText(Question question) {
            return question.getCourseCode() + "   \u00b7   Ch. " + question.getChapterNumber()
                    + "  \u00b7  " + Ui.count(reviewService.countAnswersAwaitingReview(question), "answer")
                    + " awaiting review";
        }
    }

    @FXML
    private void logout() {
        userService.logout();
        navigator.goTo(Screen.LOGIN);
    }
}
