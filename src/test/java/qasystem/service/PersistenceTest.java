package qasystem.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.data.Database;
import qasystem.model.Course;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Role;
import qasystem.model.User;
import qasystem.model.Verdict;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reason the database exists: data must survive closing the app,
 * and two apps using the same database must see each other's changes.
 * Uses a real database FILE in a temporary folder.
 */
@DisplayName("Persistence: data survives restarts and is shared")
class PersistenceTest {

    private static final String PASSWORD = "Secret#123";

    private Path folder;
    private Path file;
    private final Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 9, 1, 9, 0).atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

    @BeforeEach
    void createFolder() throws IOException {
        folder = Files.createTempDirectory("qasystem-test");
        file = folder.resolve("qasystem");
    }

    @AfterEach
    void deleteFolder() throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    @DisplayName("Everything is still there after closing and reopening the database")
    void dataSurvivesRestart() {
        long questionId;
        // ----- First run of the app -----
        try (Database db = Database.openFile(file)) {
            QASystem qa = new QASystem(db);
            UserService users = new UserService(qa, clock);
            CourseService courses = new CourseService(qa);
            QuestionService questions = new QuestionService(qa, clock);
            InvitationService invitations = new InvitationService(qa, clock);
            ReviewService reviews = new ReviewService(qa, clock);

            users.registerFirstUser("admin", PASSWORD);
            users.login("admin", PASSWORD);
            courses.createCourse("MAT 117", "Calculus I");
            courses.addChapter("MAT 117", 1, "Limits");
            String code = invitations.createInvitation(Set.of(Role.REVIEWER), 7).getCode();
            users.logout();

            users.registerUser("alice", PASSWORD, "");
            users.registerUser("rita", PASSWORD, code);
            users.login("alice", PASSWORD);
            courses.followCourse("MAT 117");
            Question q = questions.askQuestion("MAT 117", 1, "What is a limit?", "");
            questionId = q.getId();
            long answerId = questions.proposeAnswer(questionId, "The value f(x) approaches.").getId();
            questions.resolveQuestion(questionId, answerId);
            users.logout();

            users.login("rita", PASSWORD);
            reviews.submitReview(questionId, answerId, Verdict.CORRECT, "Right.");
        }

        // ----- Second run: brand-new objects, same file -----
        try (Database db = Database.openFile(file)) {
            QASystem qa = new QASystem(db);
            UserService users = new UserService(qa, clock);
            CourseService courses = new CourseService(qa);
            QuestionService questions = new QuestionService(qa, clock);

            assertFalse(users.needsFirstUser());
            assertTrue(users.login("alice", PASSWORD));
            assertTrue(courses.isFollowing("MAT 117"));

            Course course = courses.findCourse("MAT 117");
            assertNotNull(course);
            assertEquals(1, course.getChapters().size());

            Question q = questions.findQuestion(questionId);
            assertEquals("What is a limit?", q.getTitle());
            assertTrue(q.isResolved());
            assertEquals(1, q.getAnswers().size());
            assertEquals(Verdict.CORRECT, q.getAnswers().get(0).getReviews().get(0).getVerdict());

            users.logout();
            assertTrue(users.login("rita", PASSWORD));
            assertEquals(Role.REVIEWER, users.getCurrentRole());
        }
    }

    @Test
    @DisplayName("Passwords are stored as salted hashes, never as plain text")
    void passwordsAreHashed() {
        try (Database db = Database.openFile(file)) {
            QASystem qa = new QASystem(db);
            new UserService(qa, clock).registerFirstUser("admin", PASSWORD);
            String stored = db.query(c -> {
                try (var rs = c.createStatement().executeQuery("SELECT password_hash FROM users")) {
                    rs.next();
                    return rs.getString(1);
                }
            });
            assertFalse(stored.contains(PASSWORD));
            assertTrue(stored.startsWith("pbkdf2-sha256$"));

            User admin = qa.findUser("admin");
            assertTrue(admin.checkPassword(PASSWORD));
            assertFalse(admin.checkPassword("Wrong#123"));
        }
    }

    @Test
    @DisplayName("Two apps on the same database see each other's changes (the online-ready design)")
    void twoAppsShareData() {
        try (Database dbA = Database.openFile(file); Database dbB = Database.openFile(file)) {
            QASystem appA = new QASystem(dbA);
            QASystem appB = new QASystem(dbB);
            UserService usersA = new UserService(appA, clock);
            usersA.registerFirstUser("admin", PASSWORD);
            usersA.login("admin", PASSWORD);
            new CourseService(appA).createCourse("CSE 360", "Software Engineering");

            // App B never cached anything, so it sees A's course right away.
            assertNotNull(new CourseService(appB).findCourse("CSE 360"));
            // ...but sessions are separate: logging in on A does not log in B.
            assertEquals(null, appB.getCurrentUser());
        }
    }
}
