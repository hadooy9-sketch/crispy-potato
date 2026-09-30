package qasystem.util;

import qasystem.model.Role;

/**
 * Every screen in the application and the FXML file that draws it.
 * Using an enum instead of strings turns typos into compile errors.
 */
public enum Screen {
    LOGIN("login.fxml"),
    FIRST_USER("first-user.fxml"),
    REGISTER("register.fxml"),
    ROLE_SELECT("role-select.fxml"),

    // Admins and students share ONE home screen; permissions decide what each sees.
    ADMIN_HOME("courses-home.fxml"),
    STUDENT_HOME("courses-home.fxml"),
    REVIEWER_HOME("reviewer-home.fxml"),

    INVITATIONS("invitations.fxml"),
    COURSE_VIEW("course-view.fxml"),
    CHAPTER_VIEW("chapter-view.fxml"),
    ASK_QUESTION("ask-question.fxml"),
    QUESTION_DETAIL("question-detail.fxml");

    private final String fxmlFile;

    Screen(String fxmlFile) {
        this.fxmlFile = fxmlFile;
    }

    public String getFxmlFile() {
        return fxmlFile;
    }

    /**
     * The home screen for a role.
     * Note: a switch STATEMENT does not force every case to be handled -
     * a new Role would compile and only fail here at runtime.
     */
    public static Screen homeFor(Role role) {
        switch (role) {
            case ADMIN:
                return ADMIN_HOME;

            case STUDENT:
                return STUDENT_HOME;

            case REVIEWER:
                return REVIEWER_HOME;
        }

        throw new IllegalArgumentException("Unexpected role: " + role);
    }
}
