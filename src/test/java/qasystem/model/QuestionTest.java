package qasystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Question: validation of title/body, answers, and resolving.
 */
class QuestionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private Question question;

    @BeforeEach
    void setUp() {
        question = new Question(1, "MAT 117", 2, "What is a limit?", "Details here", "alice", NOW);
    }

    private Answer answer(long id) {
        return new Answer(id, "Answer " + id, "bob", NOW);
    }

    // ---------- constructor ----------

    // All values passed to the constructor are stored.
    @Test
    void constructorStoresAllFields() {
        assertEquals(1, question.getId());
        assertEquals("MAT 117", question.getCourseCode());
        assertEquals(2, question.getChapterNumber());
        assertEquals("What is a limit?", question.getTitle());
        assertEquals("Details here", question.getBody());
        assertEquals("alice", question.getAuthor());
        assertEquals(NOW, question.getCreatedAt());
    }

    // Title and body are trimmed.
    @Test
    void constructorTrimsTitleAndBody() {
        Question q = new Question(1, "C 1", 1, "  Title  ", "  Body  ", "a", NOW);
        assertEquals("Title", q.getTitle());
        assertEquals("Body", q.getBody());
    }

    // The body (details) is optional: null becomes an empty string.
    @Test
    void constructorAllowsNullBody() {
        Question q = new Question(1, "C 1", 1, "Title", null, "a", NOW);
        assertEquals("", q.getBody());
    }

    // The title is required: null and blank are rejected.
    @Test
    void constructorRejectsMissingTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, "C 1", 1, null, "", "a", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, "C 1", 1, "   ", "", "a", NOW));
    }

    // Boundary: a 150-character title is the max allowed.
    @Test
    void constructorAcceptsTitleAtMaxLength() {
        String title = "t".repeat(150);
        assertEquals(150, new Question(1, "C 1", 1, title, "", "a", NOW).getTitle().length());
    }

    // Boundary: 151 characters is too long.
    @Test
    void constructorRejectsTitleOverMaxLength() {
        String title = "t".repeat(151);
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, "C 1", 1, title, "", "a", NOW));
    }

    // Boundary: a 5000-character body is allowed.
    @Test
    void constructorAcceptsBodyAtMaxLength() {
        String body = "b".repeat(5000);
        assertEquals(5000, new Question(1, "C 1", 1, "T", body, "a", NOW).getBody().length());
    }

    // Boundary: a 5001-character body is too long.
    @Test
    void constructorRejectsBodyOverMaxLength() {
        String body = "b".repeat(5001);
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, "C 1", 1, "T", body, "a", NOW));
    }

    // ---------- answers ----------

    // A new question has no answers and is unresolved.
    @Test
    void newQuestionHasNoAnswersAndIsUnresolved() {
        assertTrue(question.getAnswers().isEmpty());
        assertFalse(question.isResolved());
        assertNull(question.getAcceptedAnswer());
    }

    // Answers are kept in the order they were added (oldest first).
    @Test
    void addAnswerKeepsOrder() {
        Answer a1 = answer(1);
        Answer a2 = answer(2);
        question.addAnswer(a1);
        question.addAnswer(a2);
        assertEquals(List.of(a1, a2), question.getAnswers());
    }

    // The answer list returned is read-only.
    @Test
    void getAnswersIsReadOnly() {
        List<Answer> answers = question.getAnswers();
        assertThrows(UnsupportedOperationException.class, () -> answers.add(answer(1)));
    }

    // findAnswer finds an answer by id.
    @Test
    void findAnswerReturnsMatchingAnswer() {
        Answer a = answer(5);
        question.addAnswer(a);
        assertSame(a, question.findAnswer(5));
    }

    // findAnswer returns null for an id that isn't on this question.
    @Test
    void findAnswerReturnsNullWhenMissing() {
        question.addAnswer(answer(5));
        assertNull(question.findAnswer(6));
    }

    // ---------- resolving ----------

    // Marking an answer as accepted resolves the question.
    @Test
    void markResolvedAcceptsAnswer() {
        Answer a = answer(1);
        question.addAnswer(a);
        question.markResolved(1);
        assertTrue(question.isResolved());
        assertSame(a, question.getAcceptedAnswer());
    }

    // You can't accept an answer that isn't on this question.
    @Test
    void markResolvedRejectsUnknownAnswer() {
        assertThrows(IllegalArgumentException.class, () -> question.markResolved(99));
        assertFalse(question.isResolved());
    }

    // Once resolved, the question can't be resolved again (even with a different answer).
    @Test
    void markResolvedTwiceThrows() {
        question.addAnswer(answer(1));
        question.addAnswer(answer(2));
        question.markResolved(1);
        assertThrows(IllegalStateException.class, () -> question.markResolved(2));
        assertEquals(1, question.getAcceptedAnswer().getId());
    }
}
