package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.model.Answer;
import qasystem.model.Question;
import qasystem.model.Review;
import qasystem.model.Role;
import qasystem.model.Verdict;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReviewService: reviewing answers")
class ReviewServiceTest extends ServiceTestBase {

    private Question question;
    private Answer aliceAnswer;   // written by a plain student
    private Answer caseyAnswer;   // written by casey, who is ALSO a reviewer

    @BeforeEach
    void setUpQuestionWithTwoAnswers() {
        createCourse("MAT 117", "Calculus I", 1);
        createStudent("alice");
        createUser("rev", Role.REVIEWER);
        createUser("casey", Role.STUDENT, Role.REVIEWER);

        loginAs("alice", Role.STUDENT);
        question = questionService.askQuestion("MAT 117", 1, "What is a limit?", "");
        aliceAnswer = questionService.proposeAnswer(question.getId(), "The value f(x) approaches.");

        loginAs("casey", Role.STUDENT);
        caseyAnswer = questionService.proposeAnswer(question.getId(), "Where the graph goes.");
    }

    /** Re-reads an answer from the database (the field holds an old snapshot). */
    private Answer reload(Answer answer) {
        return questionService.findQuestion(question.getId()).findAnswer(answer.getId());
    }

    @Test
    @DisplayName("A reviewer can review an answer and everyone can see it")
    void reviewerReviews() {
        loginAs("rev", Role.REVIEWER);

        reviewService.submitReview(question.getId(), aliceAnswer.getId(), Verdict.CORRECT, "Clear and right.");

        Answer saved = reload(aliceAnswer);
        assertEquals(1, saved.getReviews().size());
        assertEquals(Verdict.CORRECT, saved.getReviews().get(0).getVerdict());
    }

    @Test
    @DisplayName("Reviewing the same answer again updates the review instead of adding a second")
    void resubmitUpdates() {
        loginAs("rev", Role.REVIEWER);
        reviewService.submitReview(question.getId(), aliceAnswer.getId(), Verdict.CORRECT, "Looks right.");
        clock.advance(Duration.ofMinutes(10));

        reviewService.submitReview(
                question.getId(), aliceAnswer.getId(), Verdict.PARTIALLY_CORRECT, "Missing the formal definition.");

        Answer saved = reload(aliceAnswer);
        assertEquals(1, saved.getReviews().size());
        Review review = saved.getReviews().get(0);
        assertEquals(Verdict.PARTIALLY_CORRECT, review.getVerdict());
        assertTrue(review.isEdited());
    }

    @Test
    @DisplayName("A reviewer cannot review their own answer")
    void cannotReviewOwnAnswer() {
        loginAs("casey", Role.REVIEWER);

        assertFalse(reviewService.canReview(caseyAnswer));
        assertTrue(reviewService.canReview(aliceAnswer));
        assertThrows(IllegalStateException.class,
                () -> reviewService.submitReview(question.getId(), caseyAnswer.getId(), Verdict.CORRECT, "Mine is great."));
    }

    @Test
    @DisplayName("Students cannot review answers")
    void studentsCannotReview() {
        loginAs("alice", Role.STUDENT);

        assertThrows(IllegalStateException.class,
                () -> reviewService.submitReview(question.getId(), caseyAnswer.getId(), Verdict.INCORRECT, "No."));
    }

    @Test
    @DisplayName("A review needs a verdict and a comment")
    void reviewValidation() {
        loginAs("rev", Role.REVIEWER);

        assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(question.getId(), aliceAnswer.getId(), null, "Comment."));
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.submitReview(question.getId(), aliceAnswer.getId(), Verdict.CORRECT, "   "));
    }

    @Test
    @DisplayName("'Awaiting review' counts unreviewed answers and empties as they are reviewed")
    void awaitingReview() {
        loginAs("rev", Role.REVIEWER);
        assertEquals(1, reviewService.getQuestionsAwaitingReview().size());
        assertEquals(2, reviewService.countAnswersAwaitingReview(questionService.findQuestion(question.getId())));

        reviewService.submitReview(question.getId(), aliceAnswer.getId(), Verdict.CORRECT, "Right.");
        assertEquals(1, reviewService.countAnswersAwaitingReview(questionService.findQuestion(question.getId())));

        reviewService.submitReview(question.getId(), caseyAnswer.getId(), Verdict.INCORRECT, "Too vague.");
        assertTrue(reviewService.getQuestionsAwaitingReview().isEmpty());
    }
}
