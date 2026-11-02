package qasystem.service;

import qasystem.model.Answer;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Review;
import qasystem.model.Role;
import qasystem.model.Verdict;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Owns review behavior: reviewers review answers so the class benefits.
 * Rules: reviewer role only, never your own answer, one review per answer (re-submitting updates it).
 */
public class ReviewService {

    private final QASystem qaSystem;
    private final Clock clock;

    public ReviewService(QASystem qaSystem, Clock clock) {
        this.qaSystem = qaSystem;
        this.clock = clock;
    }

    // =====================================================================
    // Reviewing
    // =====================================================================

    /**
     * Creates the current reviewer's review of an answer, or updates it if one exists.
     * @throws IllegalStateException if not a reviewer, or reviewing your own answer
     * @throws IllegalArgumentException if the question/answer is gone or the review is invalid
     */
    public Review submitReview(long questionId, long answerId, Verdict verdict, String comment) {
        Access.requireRole(qaSystem, Role.REVIEWER, "review answers");
        Answer answer = requireAnswer(questionId, answerId);
        String me = currentUsername();

        if (answer.getAuthor().equals(me)) {
            throw new IllegalStateException("You cannot review your own answer.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Review existing = answer.findReviewBy(me);
        if (existing != null) {
            qaSystem.updateReview(answer.getId(), existing, verdict, comment, now);
            return existing;
        }

        Review review = new Review(me, verdict, comment, now); // validates
        if (!qaSystem.addReview(answer.getId(), review)) {
            throw new IllegalStateException("You already reviewed this answer. Refresh and edit your review.");
        }
        return review;
    }

    // =====================================================================
    // Permission checks and lookups for the UI
    // =====================================================================

    /** Same checks submitReview() enforces. */
    public boolean canReview(Answer answer) {
        return qaSystem.getCurrentRole() == Role.REVIEWER
                && !answer.getAuthor().equals(currentUsername());
    }

    /** The current user's review of this answer, or null (used to prefill the form). */
    public Review findMyReview(Answer answer) {
        if (qaSystem.getCurrentUser() == null) {
            return null;
        }
        return answer.findReviewBy(currentUsername());
    }

    /** Answers on this question with no reviews yet that the current user may review. */
    public int countAnswersAwaitingReview(Question question) {
        int count = 0;
        for (Answer answer : question.getAnswers()) {
            if (!answer.hasReviews() && !answer.getAuthor().equals(currentUsername())) {
                count++;
            }
        }
        return count;
    }

    /** Questions with at least one answer awaiting review, oldest first. */
    public List<Question> getQuestionsAwaitingReview() {
        Access.requireRole(qaSystem, Role.REVIEWER, "see answers awaiting review");
        List<Question> result = new ArrayList<>();
        for (Question question : qaSystem.getQuestions()) {
            if (countAnswersAwaitingReview(question) > 0) {
                result.add(question);
            }
        }
        result.sort(Comparator.comparing(Question::getCreatedAt));
        return result;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String currentUsername() {
        return qaSystem.getCurrentUser().getUsername();
    }

    private Answer requireAnswer(long questionId, long answerId) {
        Question question = qaSystem.findQuestion(questionId);
        if (question == null) {
            throw new IllegalArgumentException("That question no longer exists.");
        }
        Answer answer = question.findAnswer(answerId);
        if (answer == null) {
            throw new IllegalArgumentException("That answer no longer exists.");
        }
        return answer;
    }
}
