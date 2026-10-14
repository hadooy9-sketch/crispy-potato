package qasystem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Review: a reviewer's verdict and comment on an answer.
 */
class ReviewTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime T1 = T0.plusHours(1);

    // ---------- constructor ----------

    // All fields are stored, and created/updated start out equal.
    @Test
    void constructorStoresFields() {
        Review r = new Review("rita", Verdict.CORRECT, "Nice work", T0);
        assertEquals("rita", r.getReviewer());
        assertEquals(Verdict.CORRECT, r.getVerdict());
        assertEquals("Nice work", r.getComment());
        assertEquals(T0, r.getCreatedAt());
        assertEquals(T0, r.getUpdatedAt());
    }

    // The comment is trimmed.
    @Test
    void constructorTrimsComment() {
        assertEquals("ok", new Review("rita", Verdict.CORRECT, "  ok  ", T0).getComment());
    }

    // A verdict is required.
    @Test
    void constructorRejectsNullVerdict() {
        assertThrows(IllegalArgumentException.class, () -> new Review("rita", null, "ok", T0));
    }

    // A comment is required: null and blank are rejected.
    @Test
    void constructorRejectsMissingComment() {
        assertThrows(IllegalArgumentException.class,
                () -> new Review("rita", Verdict.CORRECT, null, T0));
        assertThrows(IllegalArgumentException.class,
                () -> new Review("rita", Verdict.CORRECT, "  ", T0));
    }

    // Boundary: a 2000-character comment is the max allowed.
    @Test
    void constructorAcceptsCommentAtMaxLength() {
        String comment = "c".repeat(2000);
        assertEquals(2000, new Review("rita", Verdict.CORRECT, comment, T0).getComment().length());
    }

    // Boundary: 2001 characters is too long.
    @Test
    void constructorRejectsCommentOverMaxLength() {
        String comment = "c".repeat(2001);
        assertThrows(IllegalArgumentException.class,
                () -> new Review("rita", Verdict.CORRECT, comment, T0));
    }

    // ---------- update ----------

    // Updating changes the verdict, comment and updatedAt, but NOT createdAt.
    @Test
    void updateChangesContentAndUpdatedAt() {
        Review r = new Review("rita", Verdict.CORRECT, "ok", T0);
        r.update(Verdict.INCORRECT, "  actually wrong  ", T1);
        assertEquals(Verdict.INCORRECT, r.getVerdict());
        assertEquals("actually wrong", r.getComment());
        assertEquals(T0, r.getCreatedAt());
        assertEquals(T1, r.getUpdatedAt());
    }

    // Update uses the same rules as the constructor, and a failed update changes nothing.
    @Test
    void updateRejectsInvalidInputAndKeepsOldValues() {
        Review r = new Review("rita", Verdict.CORRECT, "ok", T0);
        assertThrows(IllegalArgumentException.class, () -> r.update(null, "x", T1));
        assertThrows(IllegalArgumentException.class, () -> r.update(Verdict.INCORRECT, " ", T1));
        assertThrows(IllegalArgumentException.class,
                () -> r.update(Verdict.INCORRECT, "c".repeat(2001), T1));
        assertEquals(Verdict.CORRECT, r.getVerdict());
        assertEquals("ok", r.getComment());
        assertEquals(T0, r.getUpdatedAt());
    }

    // ---------- isEdited ----------

    // A review that was never updated is not "edited".
    @Test
    void isEditedFalseForNewReview() {
        assertFalse(new Review("rita", Verdict.CORRECT, "ok", T0).isEdited());
    }

    // After an update at a later time, it is "edited".
    @Test
    void isEditedTrueAfterLaterUpdate() {
        Review r = new Review("rita", Verdict.CORRECT, "ok", T0);
        r.update(Verdict.CORRECT, "still ok", T1);
        assertTrue(r.isEdited());
    }

    // Edge case: an update at the exact same instant does not count as an edit.
    @Test
    void isEditedFalseWhenUpdatedAtSameTime() {
        Review r = new Review("rita", Verdict.CORRECT, "ok", T0);
        r.update(Verdict.INCORRECT, "changed", T0);
        assertFalse(r.isEdited());
    }
}
