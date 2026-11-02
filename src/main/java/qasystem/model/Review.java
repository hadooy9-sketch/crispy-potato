package qasystem.model;

import java.time.LocalDateTime;

/**
 * A reviewer's review of one answer. Owned by its Answer (composition).
 * Identified by (answer, reviewer) - a natural key - so it has no id.
 */
public class Review {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final String reviewer;        // username
    private Verdict verdict;
    private String comment;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Review(String reviewer, Verdict verdict, String comment, LocalDateTime now) {
        this(reviewer, verdict, comment, now, now);
    }

    /** Package-private: QASystem rebuilds saved reviews with both dates. */
    Review(String reviewer, Verdict verdict, String comment, LocalDateTime createdAt, LocalDateTime updatedAt) {
        validate(verdict, comment);
        this.reviewer = reviewer;
        this.verdict = verdict;
        this.comment = comment.trim();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * The reviewer changed their mind: same review, new content.
     * Package-private: QASystem calls it, then saves the change.
     */
    void update(Verdict verdict, String comment, LocalDateTime now) {
        validate(verdict, comment);
        this.verdict = verdict;
        this.comment = comment.trim();
        this.updatedAt = now;
    }

    /** One set of rules for both creating and updating. */
    private static void validate(Verdict verdict, String comment) {
        if (verdict == null) {
            throw new IllegalArgumentException("Choose a verdict.");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Write a short comment explaining your verdict.");
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                "Review comment must be at most " + MAX_COMMENT_LENGTH + " characters.");
        }
    }

    public String getReviewer() {
        return reviewer;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEdited() {
        return updatedAt.isAfter(createdAt);
    }
}
