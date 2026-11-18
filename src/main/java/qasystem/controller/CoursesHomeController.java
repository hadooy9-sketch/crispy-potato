package qasystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import qasystem.model.Course;
import qasystem.model.Role;
import qasystem.model.User;
import qasystem.service.CourseService;
import qasystem.service.QuestionService;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

import java.util.List;

/**
 * The home screen for BOTH admins and students: a grid of course cards.
 * Same screen, different permissions - admins also get a "+" card and an
 * Invitations button; students also get "My Courses". The services decide which.
 */
public class CoursesHomeController {

    private final UserService userService;
    private final CourseService courseService;
    private final QuestionService questionService;
    private final ViewContext viewContext;
    private final Navigator navigator;

    @FXML private Label userLabel;
    @FXML private Button invitationsButton;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;
    @FXML private TextField searchField;
    @FXML private VBox myCoursesSection;
    @FXML private FlowPane myCoursesPane;
    @FXML private FlowPane allCoursesPane;

    public CoursesHomeController(UserService userService, CourseService courseService,
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
        User user = userService.getCurrentUser();
        Role role = userService.getCurrentRole();
        userLabel.setText(user.getUsername() + "  \u00b7  " + role.displayName());

        boolean admin = courseService.canManageCourses();
        Ui.show(invitationsButton, admin);
        Ui.show(myCoursesSection, courseService.canFollow());

        heroTitle.setText("Welcome back, " + user.getUsername());
        heroSubtitle.setText(admin
                ? "Add courses and chapters, or open a course to moderate its questions."
                : "Pick a course, open a chapter, and check whether your question was already answered.");

        searchField.textProperty().addListener((observable, oldText, newText) -> refreshAllCourses());
        refreshMyCourses();
        refreshAllCourses();
    }

    // =====================================================================
    // Cards
    // =====================================================================

    private void refreshMyCourses() {
        if (!courseService.canFollow()) {
            return;
        }
        myCoursesPane.getChildren().clear();
        List<Course> followed = courseService.getFollowedCourses();
        if (followed.isEmpty()) {
            myCoursesPane.getChildren().add(Cards.emptyMessage(
                    "You are not following any courses yet. Open a course below and press \"+ Follow\"."));
            return;
        }
        for (Course course : followed) {
            myCoursesPane.getChildren().add(courseCard(course));
        }
    }

    private void refreshAllCourses() {
        allCoursesPane.getChildren().clear();
        if (courseService.canManageCourses()) {
            allCoursesPane.getChildren().add(Cards.addCard("Add Course", this::addCourse));
        }

        List<Course> courses = courseService.searchCourses(searchField.getText());
        for (Course course : courses) {
            allCoursesPane.getChildren().add(courseCard(course));
        }
        if (courses.isEmpty() && !courseService.canManageCourses()) {
            allCoursesPane.getChildren().add(Cards.emptyMessage(
                    searchField.getText().isBlank() ? "No courses yet." : "No courses match your search."));
        }
    }

    private javafx.scene.control.Button courseCard(Course course) {
        String stats = Ui.count(course.getChapters().size(), "chapter")
                + "  \u00b7  " + Ui.count(questionService.countQuestions(course.getCode()), "question")
                + "  \u00b7  " + questionService.countUnresolved(course.getCode()) + " open";
        return Cards.card(course.getCode(), course.getTitle(), stats, () -> openCourse(course));
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private void addCourse() {
        TextField codeField = new TextField();
        codeField.setPromptText("MAT 117");
        TextField titleField = new TextField();
        titleField.setPromptText("Calculus I");

        VBox form = new VBox(8,
                fieldLabel("Course code"), codeField,
                fieldLabel("Course name"), titleField);

        boolean added = Dialogs.form("Add a Course", form, "Add Course",
                () -> courseService.createCourse(codeField.getText(), titleField.getText()));
        if (added) {
            refreshAllCourses();
        }
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void openCourse(Course course) {
        viewContext.selectCourse(course.getCode()); // also resets chapter and question
        navigator.goTo(Screen.COURSE_VIEW);
    }

    @FXML
    private void openInvitations() {
        navigator.goTo(Screen.INVITATIONS);
    }

    @FXML
    private void logout() {
        userService.logout();
        navigator.goTo(Screen.LOGIN);
    }
}
