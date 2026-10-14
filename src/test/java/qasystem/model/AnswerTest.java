package qasystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Answer.
 * An Answer holds some text, an author, and a list of Reviews (at most one per reviewer).
 */
class AnswerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private Answer answer;

    // @BeforeEach runs before EVERY test, so each test gets a brand-new Answer
    // and tests can never affect each other.
    @BeforeEach
    void setUp() {
        answer = new Answer(7, "Use the chain rule.", "alice", NOW);
    }

    // Checks that the constructor stores every value we pass in.
    @Test
    void constructorStoresAllFields() {
        assertEquals(7, answer.getId());
        assertEquals("Use the chain rule.", answer.getText());
        assertEquals("alice", answer.getAuthor());
        assertEquals(NOW, answer.getCreatedAt());
    }

    // Leading/trailing spaces should be removed so the UI shows clean text.
    @Test
    void constructorTrimsText() {
        Answer a = new Answer(1, "   padded   ", "bob", NOW);
        assertEquals("padded", a.getText());
    }

    // An answer with no text makes no sense, so null must be rejected.
    @Test
    void constructorRejectsNullText() {
        assertThrows(IllegalArgumentException.class, () -> new Answer(1, null, "bob", NOW));
    }

    // Empty and whitespace-only text are both "blank" and must be rejected.
    @Test
    void constructorRejectsBlankText() {
        assertThrows(IllegalArgumentException.class, () -> new Answer(1, "", "bob", NOW));
        assertThrows(IllegalArgumentException.class, () -> new Answer(1, "   ", "bob", NOW));
    }

    // Boundary test: exactly 5000 characters is the maximum allowed, so it must work.
    @Test
    void constructorAcceptsTextAtMaxLength() {
        String text = "a".repeat(5000);
        assertEquals(5000, new Answer(1, text, "bob", NOW).getText().length());
    }

    // Boundary test: one character over the limit (5001) must fail.
    @Test
    void constructorRejectsTextOverMaxLength() {
        String text = "a".repeat(5001);
        assertThrows(IllegalArgumentException.class, () -> new Answer(1, text, "bob", NOW));
    }

    // A brand-new answer has no reviews yet.
    @Test
    void newAnswerHasNoReviews() {
        assertFalse(answer.hasReviews());
        assertTrue(answer.getReviews().isEmpty());
    }

    // Adding a review should succeed and make it visible.
    @Test
    void addReviewStoresTheReview() {
        Review review = new Review("rita", Verdict.CORRECT, "Looks right", NOW);
        assertTrue(answer.addReview(review));
        assertTrue(answer.hasReviews());
        assertEquals(List.of(review), answer.getReviews());
    }

    // The rule is "one review per reviewer": a second review by the same person is refused.
    @Test
    void addReviewRejectsSecondReviewFromSameReviewer() {
        answer.addReview(new Review("rita", Verdict.CORRECT, "first", NOW));
        boolean added = answer.addReview(new Review("rita", Verdict.INCORRECT, "second", NOW));
        assertFalse(added);
        assertEquals(1, answer.getReviews().size());
    }

    // Different reviewers can each leave a review, and order is kept (oldest first).
    @Test
    void addReviewAllowsDifferentReviewersInOrder() {
        Review r1 = new Review("rita", Verdict.CORRECT, "ok", NOW);
        Review r2 = new Review("sam", Verdict.INCORRECT, "no", NOW);
        answer.addReview(r1);
        answer.addReview(r2);
        assertEquals(List.of(r1, r2), answer.getReviews());
    }

    // findReviewBy returns the review written by that reviewer.
    @Test
    void findReviewByReturnsMatchingReview() {
        Review review = new Review("rita", Verdict.CORRECT, "ok", NOW);
        answer.addReview(review);
        assertSame(review, answer.findReviewBy("rita"));
    }

    // findReviewBy returns null when that reviewer has not reviewed this answer.
    @Test
    void findReviewByReturnsNullWhenNotFound() {
        assertNull(answer.findReviewBy("nobody"));
    }

    // getReviews returns a read-only copy: changing it must not be possible,
    // otherwise outside code could break the "one review per reviewer" rule.
    @Test
    void getReviewsIsReadOnly() {
        List<Review> reviews = answer.getReviews();
        Review review = new Review("rita", Verdict.CORRECT, "ok", NOW);
        assertThrows(UnsupportedOperationException.class, () -> reviews.add(review));
    }
}
