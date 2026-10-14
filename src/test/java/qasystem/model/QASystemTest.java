package qasystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QASystem, the class that holds all application state
 * (users, invitations, courses, questions, and who is logged in).
 */
class QASystemTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private QASystem qa;

    // A brand-new, empty system before each test.
    @BeforeEach
    void setUp() {
        qa = new QASystem();
    }

    private Question question(long id, String course, int chapter) {
        return new Question(id, course, chapter, "Title " + id, "", "alice", NOW);
    }

    // ---------- Users ----------

    // An empty system reports that it has no users (used for first-time setup).
    @Test
    void newSystemHasNoUsers() {
        assertTrue(qa.hasNoUsers());
    }

    // After adding a user, it can be found by username.
    @Test
    void addUserThenFindIt() {
        User u = new User("alice", "pw");
        assertTrue(qa.addUser(u));
        assertSame(u, qa.findUser("alice"));
        assertFalse(qa.hasNoUsers());
    }

    // Usernames must be unique: a second "alice" is refused and the first is kept.
    @Test
    void addUserRejectsDuplicateUsername() {
        User first = new User("alice", "pw1");
        qa.addUser(first);
        assertFalse(qa.addUser(new User("alice", "pw2")));
        assertSame(first, qa.findUser("alice"));
    }

    // Looking up a username that does not exist returns null.
    @Test
    void findUserReturnsNullWhenMissing() {
        assertNull(qa.findUser("ghost"));
    }

    // ---------- Invitations ----------

    // An added invitation can be found by its code.
    @Test
    void addInvitationThenFindIt() {
        Invitation inv = new Invitation("CODE1", Set.of(Role.STUDENT), NOW);
        assertTrue(qa.addInvitation(inv));
        assertSame(inv, qa.findInvitation("CODE1"));
    }

    // Two invitations with the same code are not allowed.
    @Test
    void addInvitationRejectsDuplicateCode() {
        qa.addInvitation(new Invitation("CODE1", Set.of(Role.STUDENT), NOW));
        assertFalse(qa.addInvitation(new Invitation("CODE1", Set.of(Role.ADMIN), NOW)));
    }

    // Unknown codes return null.
    @Test
    void findInvitationReturnsNullWhenMissing() {
        assertNull(qa.findInvitation("NOPE"));
    }

    // ---------- Courses ----------

    // An added course can be found by its code.
    @Test
    void addCourseThenFindIt() {
        Course c = new Course("MAT 117", "Calc");
        assertTrue(qa.addCourse(c));
        assertSame(c, qa.findCourse("MAT 117"));
    }

    // The same course code cannot be added twice.
    @Test
    void addCourseRejectsDuplicateCode() {
        qa.addCourse(new Course("MAT 117", "Calc"));
        assertFalse(qa.addCourse(new Course("mat117", "Other")));
    }

    // Courses come back in the order they were added (LinkedHashMap).
    @Test
    void getCoursesKeepsInsertionOrder() {
        Course b = new Course("BIO 100", "Bio");
        Course a = new Course("ART 100", "Art");
        qa.addCourse(b);
        qa.addCourse(a);
        assertEquals(List.of(b, a), qa.getCourses());
    }

    // The course list is read-only.
    @Test
    void getCoursesIsReadOnly() {
        List<Course> courses = qa.getCourses();
        assertThrows(UnsupportedOperationException.class,
                () -> courses.add(new Course("X 1", "X")));
    }

    // Removing a course that does not exist returns false.
    @Test
    void removeCourseReturnsFalseWhenMissing() {
        assertFalse(qa.removeCourse("NOPE 1"));
    }

    // Removing a course must also remove its questions and every user's follow of it,
    // but must leave other courses' questions alone. This is the "cascade" rule.
    @Test
    void removeCourseCascadesToQuestionsAndFollows() {
        qa.addCourse(new Course("MAT 117", "Calc"));
        qa.addCourse(new Course("CSE 360", "SE"));
        qa.addQuestion(question(1, "MAT 117", 1));
        qa.addQuestion(question(2, "CSE 360", 1));
        User u = new User("alice", "pw");
        u.followCourse("MAT 117");
        u.followCourse("CSE 360");
        qa.addUser(u);

        assertTrue(qa.removeCourse("MAT 117"));

        assertNull(qa.findCourse("MAT 117"));
        assertNull(qa.findQuestion(1));
        assertNotNull(qa.findQuestion(2));
        assertFalse(u.isFollowing("MAT 117"));
        assertTrue(u.isFollowing("CSE 360"));
    }

    // ---------- Chapters ----------

    // Removing a chapter deletes only that chapter's questions.
    @Test
    void removeChapterCascadesToItsQuestionsOnly() {
        Course c = new Course("MAT 117", "Calc");
        c.addChapter(new Chapter(1, "A"));
        c.addChapter(new Chapter(2, "B"));
        qa.addCourse(c);
        qa.addQuestion(question(1, "MAT 117", 1));
        qa.addQuestion(question(2, "MAT 117", 2));

        assertTrue(qa.removeChapter("MAT 117", 1));

        assertNull(c.findChapter(1));
        assertNull(qa.findQuestion(1));
        assertNotNull(qa.findQuestion(2));
    }

    // Removing a chapter of a course that doesn't exist returns false.
    @Test
    void removeChapterReturnsFalseForMissingCourse() {
        assertFalse(qa.removeChapter("NOPE 1", 1));
    }

    // Removing a chapter number that doesn't exist returns false.
    @Test
    void removeChapterReturnsFalseForMissingChapter() {
        qa.addCourse(new Course("MAT 117", "Calc"));
        assertFalse(qa.removeChapter("MAT 117", 9));
    }

    // ---------- Questions ----------

    // IDs start at 1 and go up by one; they are never reused.
    @Test
    void generateQuestionIdCountsUpFromOne() {
        assertEquals(1, qa.generateQuestionId());
        assertEquals(2, qa.generateQuestionId());
        assertEquals(3, qa.generateQuestionId());
    }

    // Answer IDs have their own independent counter.
    @Test
    void generateAnswerIdIsIndependentOfQuestionIds() {
        qa.generateQuestionId();
        qa.generateQuestionId();
        assertEquals(1, qa.generateAnswerId());
        assertEquals(2, qa.generateAnswerId());
    }

    // An added question can be found by id.
    @Test
    void addQuestionThenFindIt() {
        Question q = question(1, "MAT 117", 1);
        qa.addQuestion(q);
        assertSame(q, qa.findQuestion(1));
    }

    // Unknown question ids return null.
    @Test
    void findQuestionReturnsNullWhenMissing() {
        assertNull(qa.findQuestion(99));
    }

    // Questions come back in creation order, as a read-only list.
    @Test
    void getQuestionsKeepsOrderAndIsReadOnly() {
        Question q1 = question(1, "MAT 117", 1);
        Question q2 = question(2, "MAT 117", 1);
        qa.addQuestion(q1);
        qa.addQuestion(q2);
        List<Question> list = qa.getQuestions();
        assertEquals(List.of(q1, q2), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(q1));
    }

    // removeQuestion returns true when something was removed, false otherwise.
    @Test
    void removeQuestionReportsWhetherItExisted() {
        qa.addQuestion(question(1, "MAT 117", 1));
        assertTrue(qa.removeQuestion(1));
        assertNull(qa.findQuestion(1));
        assertFalse(qa.removeQuestion(1));
    }

    // ---------- Session ----------

    // Nobody is logged in at the start.
    @Test
    void newSystemHasNoSession() {
        assertNull(qa.getCurrentUser());
        assertNull(qa.getCurrentRole());
    }

    // Setting a user and role stores both.
    @Test
    void setCurrentUserAndRole() {
        User u = new User("alice", "pw");
        qa.setCurrentUser(u);
        qa.setCurrentRole(Role.STUDENT);
        assertSame(u, qa.getCurrentUser());
        assertEquals(Role.STUDENT, qa.getCurrentRole());
    }

    // Switching to a new user must reset the role, so the old user's role never carries over.
    @Test
    void setCurrentUserResetsRole() {
        qa.setCurrentUser(new User("admin", "pw"));
        qa.setCurrentRole(Role.ADMIN);
        qa.setCurrentUser(new User("bob", "pw"));
        assertNull(qa.getCurrentRole());
    }

    // Logging out clears both the user and the role.
    @Test
    void clearCurrentUserClearsBoth() {
        qa.setCurrentUser(new User("alice", "pw"));
        qa.setCurrentRole(Role.STUDENT);
        qa.clearCurrentUser();
        assertNull(qa.getCurrentUser());
        assertNull(qa.getCurrentRole());
    }
}
