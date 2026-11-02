package qasystem.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import qasystem.data.Database;
import qasystem.model.QASystem;
import qasystem.model.Role;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared setup for every service test: a fresh system with its own in-memory
 * database before EACH test, so tests never depend on each other.
 * Helpers go through the real public API.
 *
 * Remember: objects returned by services are snapshots. After changing data,
 * read it again (e.g. questionService.findQuestion(id)) to see the change.
 */
abstract class ServiceTestBase {

    static final String PASSWORD = "Secret#123"; // meets every PasswordPolicy rule
    static final String ADMIN = "admin";

    protected MutableClock clock;
    protected Database database;
    protected QASystem qaSystem;
    protected UserService userService;
    protected InvitationService invitationService;
    protected CourseService courseService;
    protected QuestionService questionService;
    protected ReviewService reviewService;

    @BeforeEach
    void setUpSystem() {
        clock = new MutableClock(LocalDateTime.of(2026, 9, 1, 9, 0));
        database = Database.inMemory(); // a brand-new, empty database for every test
        qaSystem = new QASystem(database);
        userService = new UserService(qaSystem, clock);
        invitationService = new InvitationService(qaSystem, clock);
        courseService = new CourseService(qaSystem);
        questionService = new QuestionService(qaSystem, clock);
        reviewService = new ReviewService(qaSystem, clock);

        userService.registerFirstUser(ADMIN, PASSWORD); // every test starts with an admin
    }

    @AfterEach
    void closeDatabase() {
        database.close(); // an in-memory database disappears when closed
    }

    // ---------- Helpers ----------

    /** Logs in (logging out whoever was there) and makes sure we act in the given role. */
    protected void loginAs(String username, Role role) {
        userService.logout();
        assertTrue(userService.login(username, PASSWORD), "login failed for " + username);
        if (userService.needsRoleSelection()) {
            userService.selectRole(role);
        }
        assertEquals(role, userService.getCurrentRole());
    }

    /** An invite code made by the admin. Leaves nobody logged in. */
    protected String inviteCode(int validDays, Role... roles) {
        loginAs(ADMIN, Role.ADMIN);
        String code = invitationService.createInvitation(Set.of(roles), validDays).getCode();
        userService.logout();
        return code;
    }

    /** Sign-up without a code = student. */
    protected void createStudent(String username) {
        userService.logout();
        userService.registerUser(username, PASSWORD, "");
    }

    /** Sign-up with an invite code for these roles. */
    protected void createUser(String username, Role... roles) {
        String code = inviteCode(7, roles);
        userService.registerUser(username, PASSWORD, code);
    }

    /** Admin creates a course with chapters titled "Chapter n". Leaves nobody logged in. */
    protected void createCourse(String code, String title, int... chapterNumbers) {
        loginAs(ADMIN, Role.ADMIN);
        courseService.createCourse(code, title);
        for (int number : chapterNumbers) {
            courseService.addChapter(code, number, "Chapter " + number);
        }
        userService.logout();
    }
}
