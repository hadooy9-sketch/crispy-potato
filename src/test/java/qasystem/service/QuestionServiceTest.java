package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import qasystem.model.Answer;
import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Role;
import qasystem.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuestionService: asking, listing, searching, counting,
 * answering, resolving and deleting questions.
 */
class QuestionServiceTest {

    /**
     * A clock we can move forward by hand. Many methods sort by "createdAt",
     * so each question needs a different time - without waiting in real life.
     */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private QASystem qa;
    private MutableClock clock;
    private QuestionService service;
    private User alice;
    private User bob;

    // Set up: two courses (MAT 117 with chapters 1 and 2, CSE 360 with chapter 1),
    // two students (alice, bob), and alice is logged in as STUDENT.
    @BeforeEach
    void setUp() {
        qa = new QASystem();
        clock = new MutableClock();
        service = new QuestionService(qa, clock);

        Course mat = new Course("MAT 117", "Calculus I");
        mat.addChapter(new Chapter(1, "Limits"));
        mat.addChapter(new Chapter(2, "Derivatives"));
        qa.addCourse(mat);
        Course cse = new Course("CSE 360", "Software Engineering");
        cse.addChapter(new Chapter(1, "Intro"));
        qa.addCourse(cse);

        alice = new User("alice", "pw");
        alice.addRole(Role.STUDENT);
        bob = new User("bob", "pw");
        bob.addRole(Role.STUDENT);
        qa.addUser(alice);
        qa.addUser(bob);
        loginAs(alice, Role.STUDENT);
    }

    private void loginAs(User user, Role role) {
        qa.setCurrentUser(user);
        qa.setCurrentRole(role);
    }

    /** Asks a question, then moves the clock forward 1 minute so the next one is "newer". */
    private Question ask(String course, int chapter, String title, String body) {
        Question q = service.askQuestion(course, chapter, title, body);
        clock.advance(Duration.ofMinutes(1));
        return q;
    }

    // ---------- askQuestion ----------

    // Asking stores the question with a new id, the current user as author, and the clock's time.
    @Test
    void askQuestionStoresQuestion() {
        LocalDateTime expectedTime = LocalDateTime.now(clock);
        Question q = service.askQuestion("mat117", 1, "What is a limit?", "Please explain");
        assertEquals(1, q.getId());
        assertEquals("MAT 117", q.getCourseCode());
        assertEquals(1, q.getChapterNumber());
        assertEquals("alice", q.getAuthor());
        assertEquals(expectedTime, q.getCreatedAt());
        assertSame(q, service.findQuestion(1));
    }

    // Each question gets a new, increasing id.
    @Test
    void askQuestionGivesIncreasingIds() {
        Question q1 = ask("MAT 117", 1, "One", "");
        Question q2 = ask("MAT 117", 1, "Two", "");
        assertEquals(q1.getId() + 1, q2.getId());
    }

    // Only students can ask questions.
    @Test
    void askQuestionRequiresStudent() {
        qa.setCurrentRole(Role.ADMIN);
        assertThrows(IllegalStateException.class, () -> service.askQuestion("MAT 117", 1, "T", ""));
    }

    // Unknown course or chapter is rejected.
    @Test
    void askQuestionRejectsUnknownCourseOrChapter() {
        assertThrows(IllegalArgumentException.class, () -> service.askQuestion("NOPE 1", 1, "T", ""));
        assertThrows(IllegalArgumentException.class, () -> service.askQuestion("MAT 117", 9, "T", ""));
    }

    // Invalid title is rejected and nothing is stored.
    @Test
    void askQuestionRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.askQuestion("MAT 117", 1, " ", ""));
        assertTrue(qa.getQuestions().isEmpty());
    }

    // ---------- findQuestion ----------

    // Unknown ids return null.
    @Test
    void findQuestionReturnsNullWhenMissing() {
        assertNull(service.findQuestion(123));
    }

    // ---------- getQuestions(course, chapter) ----------

    // Only questions from that exact course+chapter are returned, newest first.
    @Test
    void getQuestionsFiltersAndSortsNewestFirst() {
        Question old = ask("MAT 117", 1, "Old", "");
        ask("MAT 117", 2, "Other chapter", "");
        ask("CSE 360", 1, "Other course", "");
        Question recent = ask("MAT 117", 1, "New", "");

        assertEquals(List.of(recent, old), service.getQuestions("mat117", 1));
    }

    // A chapter with no questions gives an empty list.
    @Test
    void getQuestionsEmptyWhenNone() {
        assertTrue(service.getQuestions("MAT 117", 2).isEmpty());
    }

    // ---------- getUnresolvedQuestions ----------

    // All unresolved questions across courses, OLDEST first (waiting longest at the top).
    @Test
    void getUnresolvedQuestionsOldestFirstAndSkipsResolved() {
        Question q1 = ask("MAT 117", 1, "First", "");
        Question q2 = ask("CSE 360", 1, "Second", "");
        Question q3 = ask("MAT 117", 2, "Third", "");
        Answer a = service.proposeAnswer(q2.getId(), "answer");
        service.resolveQuestion(q2.getId(), a.getId());

        assertEquals(List.of(q1, q3), service.getUnresolvedQuestions());
    }

    // The per-course version only includes that course.
    @Test
    void getUnresolvedQuestionsForOneCourse() {
        Question q1 = ask("MAT 117", 1, "First", "");
        ask("CSE 360", 1, "Second", "");
        Question q3 = ask("MAT 117", 2, "Third", "");
        assertEquals(List.of(q1, q3), service.getUnresolvedQuestions("mat 117"));
    }

    // ---------- findRelatedQuestions ----------

    // Questions sharing keywords are found; unrelated ones and other courses are not.
    @Test
    void findRelatedQuestionsMatchesSharedKeywordsInSameCourse() {
        Question related = ask("MAT 117", 1, "How to compute derivatives", "");
        ask("MAT 117", 1, "Integration by parts", "");
        ask("CSE 360", 1, "Derivative of a function", ""); // other course

        assertEquals(List.of(related),
                service.findRelatedQuestions("MAT 117", "derivative help", null));
    }

    // More shared keywords ranks higher.
    @Test
    void findRelatedQuestionsSortsByMostSharedKeywords() {
        Question oneWord = ask("MAT 117", 1, "chain problem", "");
        Question twoWords = ask("MAT 117", 1, "chain rule", "");
        assertEquals(List.of(twoWords, oneWord),
                service.findRelatedQuestions("MAT 117", "chain rule", ""));
    }

    // With equal scores, resolved questions come before unresolved ones.
    @Test
    void findRelatedQuestionsPrefersResolvedOnTie() {
        Question resolved = ask("MAT 117", 1, "limit question", "");
        ask("MAT 117", 1, "limit puzzle", ""); // newer but unresolved
        Answer a = service.proposeAnswer(resolved.getId(), "answer");
        service.resolveQuestion(resolved.getId(), a.getId());

        assertSame(resolved, service.findRelatedQuestions("MAT 117", "limit", "").get(0));
    }

    // With equal scores and same resolved state, newer questions come first.
    @Test
    void findRelatedQuestionsPrefersNewerOnTie() {
        Question older = ask("MAT 117", 1, "limit one", "");
        Question newer = ask("MAT 117", 1, "limit two", "");
        assertEquals(List.of(newer, older), service.findRelatedQuestions("MAT 117", "limit", ""));
    }

    // At most 5 results are returned.
    @Test
    void findRelatedQuestionsReturnsAtMostFive() {
        for (int i = 0; i < 8; i++) {
            ask("MAT 117", 1, "matrix question " + i, "");
        }
        assertEquals(5, service.findRelatedQuestions("MAT 117", "matrix", "").size());
    }

    // The body text is used for matching too.
    @Test
    void findRelatedQuestionsUsesBody() {
        Question q = ask("MAT 117", 1, "Help please", "something about eigenvalues");
        assertEquals(List.of(q), service.findRelatedQuestions("MAT 117", "Stuck", "eigenvalues"));
    }

    // If the new text has no keywords (only stop words), the result is empty.
    @Test
    void findRelatedQuestionsEmptyWhenNoKeywords() {
        ask("MAT 117", 1, "How is this", "");
        assertTrue(service.findRelatedQuestions("MAT 117", "how is it", "").isEmpty());
    }

    // ---------- counts ----------

    // Counting questions per course and per chapter.
    @Test
    void countQuestions() {
        ask("MAT 117", 1, "A", "");
        ask("MAT 117", 1, "B", "");
        ask("MAT 117", 2, "C", "");
        ask("CSE 360", 1, "D", "");
        assertEquals(3, service.countQuestions("mat117"));
        assertEquals(2, service.countQuestions("MAT 117", 1));
        assertEquals(1, service.countQuestions("MAT 117", 2));
        assertEquals(0, service.countQuestions("NOPE 1"));
    }

    // Counting unresolved questions per course and per chapter.
    @Test
    void countUnresolved() {
        Question q1 = ask("MAT 117", 1, "A", "");
        ask("MAT 117", 1, "B", "");
        ask("MAT 117", 2, "C", "");
        Answer a = service.proposeAnswer(q1.getId(), "answer");
        service.resolveQuestion(q1.getId(), a.getId());

        assertEquals(2, service.countUnresolved("MAT 117"));
        assertEquals(1, service.countUnresolved("MAT 117", 1));
        assertEquals(1, service.countUnresolved("MAT 117", 2));
    }

    // ---------- proposeAnswer ----------

    // A student can answer; the answer is attached with the current user as author.
    @Test
    void proposeAnswerAddsAnswer() {
        Question q = ask("MAT 117", 1, "Q", "");
        loginAs(bob, Role.STUDENT);
        Answer a = service.proposeAnswer(q.getId(), "Here is how");
        assertEquals("bob", a.getAuthor());
        assertEquals(List.of(a), q.getAnswers());
    }

    // Only students can answer.
    @Test
    void proposeAnswerRequiresStudent() {
        Question q = ask("MAT 117", 1, "Q", "");
        qa.setCurrentRole(Role.REVIEWER);
        assertThrows(IllegalStateException.class, () -> service.proposeAnswer(q.getId(), "x"));
    }

    // Can't answer a question that doesn't exist.
    @Test
    void proposeAnswerRejectsUnknownQuestion() {
        assertThrows(IllegalArgumentException.class, () -> service.proposeAnswer(42, "x"));
    }

    // Can't answer a question that is already resolved.
    @Test
    void proposeAnswerRejectsResolvedQuestion() {
        Question q = ask("MAT 117", 1, "Q", "");
        Answer a = service.proposeAnswer(q.getId(), "first");
        service.resolveQuestion(q.getId(), a.getId());
        assertThrows(IllegalArgumentException.class, () -> service.proposeAnswer(q.getId(), "late"));
    }

    // Blank answer text is rejected (validation from Answer).
    @Test
    void proposeAnswerRejectsBlankText() {
        Question q = ask("MAT 117", 1, "Q", "");
        assertThrows(IllegalArgumentException.class, () -> service.proposeAnswer(q.getId(), "  "));
    }

    // ---------- resolveQuestion ----------

    // The asker can mark an answer as accepted.
    @Test
    void resolveQuestionByAsker() {
        Question q = ask("MAT 117", 1, "Q", "");
        loginAs(bob, Role.STUDENT);
        Answer a = service.proposeAnswer(q.getId(), "answer");
        loginAs(alice, Role.STUDENT);
        service.resolveQuestion(q.getId(), a.getId());
        assertTrue(q.isResolved());
        assertSame(a, q.getAcceptedAnswer());
    }

    // Someone who did not ask the question cannot resolve it.
    @Test
    void resolveQuestionRejectsNonAsker() {
        Question q = ask("MAT 117", 1, "Q", "");
        Answer a = service.proposeAnswer(q.getId(), "answer");
        loginAs(bob, Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.resolveQuestion(q.getId(), a.getId()));
        assertFalse(q.isResolved());
    }

    // Resolving twice fails.
    @Test
    void resolveQuestionTwiceFails() {
        Question q = ask("MAT 117", 1, "Q", "");
        Answer a = service.proposeAnswer(q.getId(), "answer");
        service.resolveQuestion(q.getId(), a.getId());
        assertThrows(IllegalStateException.class, () -> service.resolveQuestion(q.getId(), a.getId()));
    }

    // Unknown question or answer is rejected.
    @Test
    void resolveQuestionRejectsUnknownIds() {
        Question q = ask("MAT 117", 1, "Q", "");
        assertThrows(IllegalArgumentException.class, () -> service.resolveQuestion(999, 1));
        assertThrows(IllegalArgumentException.class, () -> service.resolveQuestion(q.getId(), 999));
    }

    // Only students can resolve.
    @Test
    void resolveQuestionRequiresStudent() {
        Question q = ask("MAT 117", 1, "Q", "");
        Answer a = service.proposeAnswer(q.getId(), "answer");
        qa.setCurrentRole(Role.ADMIN);
        assertThrows(IllegalStateException.class, () -> service.resolveQuestion(q.getId(), a.getId()));
    }

    // ---------- deleteQuestion ----------

    // An admin can delete a question.
    @Test
    void deleteQuestionByAdmin() {
        Question q = ask("MAT 117", 1, "Q", "");
        qa.setCurrentRole(Role.ADMIN);
        service.deleteQuestion(q.getId());
        assertNull(service.findQuestion(q.getId()));
    }

    // Deleting a question that doesn't exist is an error.
    @Test
    void deleteQuestionRejectsUnknown() {
        qa.setCurrentRole(Role.ADMIN);
        assertThrows(IllegalArgumentException.class, () -> service.deleteQuestion(77));
    }

    // Students cannot delete questions.
    @Test
    void deleteQuestionRequiresAdmin() {
        Question q = ask("MAT 117", 1, "Q", "");
        assertThrows(IllegalStateException.class, () -> service.deleteQuestion(q.getId()));
        assertNotNull(service.findQuestion(q.getId()));
    }

    // ---------- permission checks for the UI ----------

    // canAsk: only students.
    @Test
    void canAskOnlyForStudent() {
        assertTrue(service.canAsk());
        qa.setCurrentRole(Role.ADMIN);
        assertFalse(service.canAsk());
    }

    // canAnswer: student AND the question is still open.
    @Test
    void canAnswerRequiresStudentAndUnresolved() {
        Question q = ask("MAT 117", 1, "Q", "");
        assertTrue(service.canAnswer(q));
        Answer a = service.proposeAnswer(q.getId(), "answer");
        service.resolveQuestion(q.getId(), a.getId());
        assertFalse(service.canAnswer(q));

        Question open = ask("MAT 117", 1, "Open", "");
        qa.setCurrentRole(Role.REVIEWER);
        assertFalse(service.canAnswer(open));
    }

    // canResolve: student, the asker, and still unresolved - same rules as resolveQuestion.
    @Test
    void canResolveMatchesResolveRules() {
        Question q = ask("MAT 117", 1, "Q", "");
        assertTrue(service.canResolve(q));

        loginAs(bob, Role.STUDENT);
        assertFalse(service.canResolve(q)); // not the asker

        loginAs(alice, Role.ADMIN);
        assertFalse(service.canResolve(q)); // wrong role

        loginAs(alice, Role.STUDENT);
        Answer a = service.proposeAnswer(q.getId(), "answer");
        service.resolveQuestion(q.getId(), a.getId());
        assertFalse(service.canResolve(q)); // already resolved
    }

    // canDelete: only admins.
    @Test
    void canDeleteOnlyForAdmin() {
        assertFalse(service.canDelete());
        qa.setCurrentRole(Role.ADMIN);
        assertTrue(service.canDelete());
    }
}
