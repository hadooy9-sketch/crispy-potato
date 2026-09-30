package qasystem.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A potential answer to one question.
 * Owned by its Question; owns its Reviews (composition all the way down).
 */
public class Answer {

    private static final int MAX_TEXT_LENGTH = 5000;

    private final long id;
    private final String text;
    private final String author;          // username, not the User object
    private final LocalDateTime createdAt;
    private final ArrayList<Review> reviews;

    public Answer(long id, String text, String author, LocalDateTime createdAt) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Answer text is required.");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                "Answer must be at most " + MAX_TEXT_LENGTH + " characters.");
        }
        this.id = id;
        this.text = text.trim();
        this.author = author;
        this.createdAt = createdAt;
        this.reviews = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ---------- Reviews ----------

    /** Read-only snapshot, oldest review first. */
    public List<Review> getReviews() {
        return List.copyOf(reviews);
    }

    public boolean hasReviews() {
        return !reviews.isEmpty();
    }

    /** The review this reviewer wrote, or null. */
    public Review findReviewBy(String reviewer) {
        for (Review review : reviews) {
            if (review.getReviewer().equals(reviewer)) {
                return review;
            }
        }
        return null;
    }

    /** Invariant: one review per reviewer. Returns false if they already reviewed. */
    public boolean addReview(Review review) {
        if (findReviewBy(review.getReviewer()) != null) {
            return false;
        }
        reviews.add(review);
        return true;
    }
}
