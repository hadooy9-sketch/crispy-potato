package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.service.CourseService;
import qasystem.service.QuestionService;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ShortcutHints;
import qasystem.util.ViewContext;

/**
 * One course: an "ask" banner (ask a question, or see the open ones), then one card per chapter.
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
    @FXML private HBox askBanner;
    @FXML private Label askTitleLabel;
    @FXML private Label askSubtitleLabel;
    @FXML private Button openQuestionsButton;
    @FXML private Button askButton;

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
            Ui.show(askBanner, false);
            return;
        }
        courseCodeLabel.setText(course.getCode());
        courseTitleLabel.setText(course.getTitle());

        Ui.show(followButton, courseService.canFollow());
        Ui.show(deleteCourseButton, courseService.canManageCourses());
        updateFollowButton();
        refreshAskBanner();
        refreshCards();
    }

    /**
     * The banner at the top. It used to be a yellow "Unresolved" card, which was confusing
     * next to the chapters. It still leads to the same list (the spec asks for
     * "a list of all unresolved questions"), it just says what it is in plain words.
     */
    private void refreshAskBanner() {
        int open = questionService.countUnresolved(course.getCode());
        askTitleLabel.setText("Have a question about " + course.getCode() + "?");
        boolean canAsk = questionService.canAsk() && !course.getChapters().isEmpty();
        Ui.show(askButton, canAsk);
        if (open == 0) {
            askSubtitleLabel.setText(canAsk
                    ? "Nothing is waiting for an answer right now. Ask the first one!"
                    : "Nothing is waiting for an answer right now.");
            openQuestionsButton.setText("See open questions");
        } else {
            askSubtitleLabel.setText(Ui.count(open, "question")
                    + (open == 1 ? " is" : " are") + " still waiting for an answer. Check them first, yours may be there.");
            openQuestionsButton.setText("See open questions (" + open + ")");
        }
    }

    private void refreshCards() {
        // Re-read the course: the copy we hold is a snapshot and may be out of date
        // (e.g. right after adding a chapter, or if someone else changed it).
        course = courseService.findCourse(course.getCode());
        chaptersPane.getChildren().clear();
        if (course == null) {
            return;
        }

        for (Chapter chapter : course.getChapters()) {
            int number = chapter.getNumber();
            String stats = Ui.count(questionService.countQuestions(course.getCode(), number), "question")
                    + "  \u00b7  " + questionService.countUnresolved(course.getCode(), number) + " open";
            Button card = Cards.card("Chapter " + number, chapter.getTitle(), stats, () -> openChapter(number));
            if (number >= 1 && number <= 9) {
                ShortcutHints.markChapterCard(card, number); // the number keys 1-9 open it
            }
            chaptersPane.getChildren().add(card);
        }

        if (courseService.canManageCourses()) {
            chaptersPane.getChildren().add(Cards.addCard("Add Chapter", this::addChapter));
        } else if (course.getChapters().isEmpty()) {
            chaptersPane.getChildren().add(Cards.emptyMessage("No chapters yet. Questions can be asked once an admin adds one."));
        }
    }

    // =====================================================================
    // Actions
    // =====================================================================

    /** null = the list of open (unresolved) questions. */
    private void openChapter(Integer chapterNumber) {
        viewContext.selectChapter(chapterNumber);
        navigator.goTo(Screen.CHAPTER_VIEW);
    }

    @FXML
    private void openQuestions() {
        openChapter(null);
    }

    @FXML
    private void askQuestion() {
        viewContext.selectChapter(null); // the form starts on the first chapter; the student can pick another
        navigator.goTo(Screen.ASK_QUESTION);
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
        titleField.setPromptText("e.g. Derivatives");

        Label numberLabel = new Label("Chapter number");
        numberLabel.getStyleClass().add("field-label");
        Label titleLabel = new Label("Chapter title");
        titleLabel.getStyleClass().add("field-label");
        VBox form = new VBox(8, numberLabel, numberSpinner, titleLabel, titleField);

        // Opens inside this window; refreshCards runs after the chapter is added.
        Dialogs.form(chaptersPane, "Add a Chapter to " + course.getCode(), form, "Add Chapter",
                () -> courseService.addChapter(course.getCode(), numberSpinner.getValue(), titleField.getText()),
                () -> { refreshCards(); refreshAskBanner(); });
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
