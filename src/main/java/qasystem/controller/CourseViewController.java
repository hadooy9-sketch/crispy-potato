package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.service.CourseService;
import qasystem.service.QuestionService;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

/**
 * One course: an "Unresolved" card, then one card per chapter.
 * Students can follow the course; admins can add chapters and delete the course.
 */
public class CourseViewController {

    private final UserService userService;
    private final CourseService courseService;
    private final QuestionService questionService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Button followButton;
    @FXML private Button deleteCourseButton;
    @FXML private Label courseCodeLabel;
    @FXML private Label courseTitleLabel;
    @FXML private Label courseSubtitleLabel;
    @FXML private FlowPane chaptersPane;

    private Course course;

    public CourseViewController(UserService userService, CourseService courseService,
                                QuestionService questionService, ViewContext viewContext,
                                Navigator navigator) {
        this.userService = userService;
        this.courseService = courseService;
        this.questionService = questionService;
        this.viewContext = viewContext;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        course = courseService.findCourse(viewContext.getSelectedCourseCode());
        if (course == null) {
            courseTitleLabel.setText("This course no longer exists.");
            Ui.show(courseSubtitleLabel, false);
            Ui.show(followButton, false);
            Ui.show(deleteCourseButton, false);
            return;
        }
        courseCodeLabel.setText(course.getCode());
        courseTitleLabel.setText(course.getTitle());

        Ui.show(followButton, courseService.canFollow());
        Ui.show(deleteCourseButton, courseService.canManageCourses());
        updateFollowButton();
        refreshCards();
    }

    private void refreshCards() {
        chaptersPane.getChildren().clear();

        // Spec: "a list of all unresolved questions" - always the first card.
        Button unresolved = Cards.card("Unresolved", "Questions still waiting for an accepted answer",
                Ui.count(questionService.countUnresolved(course.getCode()), "open question"),
                () -> openChapter(null));
        unresolved.getStyleClass().add("card-highlight");
        chaptersPane.getChildren().add(unresolved);

        for (Chapter chapter : course.getChapters()) {
            int number = chapter.getNumber();
            String stats = Ui.count(questionService.countQuestions(course.getCode(), number), "question")
                    + "  \u00b7  " + questionService.countUnresolved(course.getCode(), number) + " open";
            chaptersPane.getChildren().add(
                    Cards.card("Chapter " + number, chapter.getTitle(), stats, () -> openChapter(number)));
        }

        if (courseService.canManageCourses()) {
            chaptersPane.getChildren().add(Cards.addCard("Add Chapter", this::addChapter));
        }
    }

    // =====================================================================
    // Actions
    // =====================================================================

    /** null = the "Unresolved" card. */
    private void openChapter(Integer chapterNumber) {
        viewContext.selectChapter(chapterNumber);
        navigator.goTo(Screen.CHAPTER_VIEW);
    }

    @FXML
    private void toggleFollow() {
        if (courseService.isFollowing(course.getCode())) {
            courseService.unfollowCourse(course.getCode());
        } else {
            courseService.followCourse(course.getCode());
        }
        updateFollowButton();
    }

    private void updateFollowButton() {
        boolean following = courseService.canFollow() && courseService.isFollowing(course.getCode());
        followButton.setText(following ? "\u2713  Following" : "+  Follow");
        followButton.getStyleClass().removeAll("primary-button", "secondary-button");
        followButton.getStyleClass().add(following ? "secondary-button" : "primary-button");
    }

    private void addChapter() {
        Spinner<Integer> numberSpinner = new Spinner<>(1, 99, course.nextChapterNumber());
        numberSpinner.setPrefWidth(120);
        TextField titleField = new TextField();
        titleField.setPromptText("Derivatives");

        Label numberLabel = new Label("Chapter number");
        numberLabel.getStyleClass().add("field-label");
        Label titleLabel = new Label("Chapter title");
        titleLabel.getStyleClass().add("field-label");
        VBox form = new VBox(8, numberLabel, numberSpinner, titleLabel, titleField);

        boolean added = Dialogs.form("Add a Chapter to " + course.getCode(), form, "Add Chapter",
                () -> courseService.addChapter(course.getCode(), numberSpinner.getValue(), titleField.getText()));
        if (added) {
            refreshCards();
        }
    }

    @FXML
    private void deleteCourse() {
        if (!Dialogs.confirm("Delete " + course.getCode() + " with all of its chapters and questions?\n"
                + "This cannot be undone.")) {
            return;
        }
        courseService.deleteCourse(course.getCode());
        navigator.goTo(Screen.homeFor(userService.getCurrentRole()));
    }

    @FXML
    private void back() {
        navigator.goTo(Screen.homeFor(userService.getCurrentRole()));
    }
}
