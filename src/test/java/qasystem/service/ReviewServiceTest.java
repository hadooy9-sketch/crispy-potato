package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import qasystem.model.Answer;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Review;
import qasystem.model.Role;
import qasystem.model.User;
import qasystem.model.Verdict;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReviewService: reviewers review other people's answers,
 * one review per answer (submitting again updates it).
 */
class ReviewServiceTest {

    /** A clock we can move forward by hand, to test "edited" timestamps. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 12, 0);

    private QASystem qa;
    private MutableClock clock;
    private ReviewService service;
    private User rita;       // a reviewer
    private Question question;
    private Answer bobsAnswer;

    // Set up: one question by alice, with one answer by bob. Rita is logged in as REVIEWER.
    @BeforeEach
    void setUp() {
        qa = new QASystem();
        clock = new MutableClock();
        service = new ReviewService(qa, clock);

        question = new Question(1, "MAT 117", 1, "What is a limit?", "", "alice", T0);
        bobsAnswer = new Answer(10, "It's a value approached", "bob", T0);
        question.addAnswer(bobsAnswer);
        qa.addQuestion(question);

        rita = new User("rita", "pw");
        rita.addRole(Role.REVIEWER);
        rita.addRole(Role.STUDENT);
        qa.addUser(rita);
        qa.setCurrentUser(rita);
        qa.setCurrentRole(Role.REVIEWER);
    }

    // ---------- submitReview ----------

    // First submission creates a review stored on the answer.
    @Test
    void submitReviewCreatesReview() {
        Review r = service.submitReview(1, 10, Verdict.CORRECT, "Good");
        assertEquals("rita", r.getReviewer());
        assertEquals(Verdict.CORRECT, r.getVerdict());
        assertEquals(LocalDateTime.now(clock), r.getCreatedAt());
        assertEquals(List.of(r), bobsAnswer.getReviews());
    }

    // Submitting again UPDATES the same review instead of adding a second one.
    @Test
    void submitReviewTwiceUpdatesExistingReview() {
        Review first = service.submitReview(1, 10, Verdict.CORRECT, "Good");
        clock.advance(Duration.ofHours(1));
        Review second = service.submitReview(1, 10, Verdict.INCORRECT, "Actually wrong");

        assertSame(first, second);
        assertEquals(1, bobsAnswer.getReviews().size());
        assertEquals(Verdict.INCORRECT, second.getVerdict());
        assertTrue(second.isEdited());
    }

    // You may not review your own answer.
    @Test
    void submitReviewRejectsOwnAnswer() {
        Answer ritasAnswer = new Answer(11, "My answer", "rita", T0);
        question.addAnswer(ritasAnswer);
        assertThrows(IllegalStateException.class,
                () -> service.submitReview(1, 11, Verdict.CORRECT, "I'm great"));
        assertFalse(ritasAnswer.hasReviews());
    }

    // Only reviewers can review (even if the user HAS the reviewer role, they must be acting as one).
    @Test
    void submitReviewRequiresReviewerRole() {
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class,
                () -> service.submitReview(1, 10, Verdict.CORRECT, "Good"));
    }

    // Unknown question or answer ids are rejected.
    @Test
    void submitReviewRejectsUnknownQuestionOrAnswer() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(99, 10, Verdict.CORRECT, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(1, 99, Verdict.CORRECT, "x"));
    }

    // Invalid review content (no verdict, blank comment) is rejected and nothing is stored.
    @Test
    void submitReviewRejectsInvalidContent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(1, 10, null, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(1, 10, Verdict.CORRECT, " "));
        assertFalse(bobsAnswer.hasReviews());
    }

    // ---------- canReview ----------

    // A reviewer can review someone else's answer.
    @Test
    void canReviewOthersAnswer() {
        assertTrue(service.canReview(bobsAnswer));
    }

    // A reviewer cannot review their own answer.
    @Test
    void cannotReviewOwnAnswer() {
        assertFalse(service.canReview(new Answer(11, "mine", "rita", T0)));
    }

    // Not acting as reviewer -> cannot review.
    @Test
    void cannotReviewWithoutReviewerRole() {
        qa.setCurrentRole(Role.STUDENT);
        assertFalse(service.canReview(bobsAnswer));
    }

    // ---------- findMyReview ----------

    // Returns the current user's review of the answer.
    @Test
    void findMyReviewReturnsOwnReview() {
        Review r = service.submitReview(1, 10, Verdict.CORRECT, "Good");
        assertSame(r, service.findMyReview(bobsAnswer));
    }

    // Returns null if the current user hasn't reviewed it.
    @Test
    void findMyReviewReturnsNullWhenNotReviewed() {
        assertNull(service.findMyReview(bobsAnswer));
    }

    // Returns null (no crash) if nobody is logged in.
    @Test
    void findMyReviewReturnsNullWhenLoggedOut() {
        qa.clearCurrentUser();
        assertNull(service.findMyReview(bobsAnswer));
    }

    // ---------- countAnswersAwaitingReview ----------

    // Counts unreviewed answers by other people; your own and already-reviewed ones don't count.
    @Test
    void countAnswersAwaitingReview() {
        question.addAnswer(new Answer(11, "mine", "rita", T0));
        question.addAnswer(new Answer(12, "carol's", "carol", T0));
        assertEquals(2, service.countAnswersAwaitingReview(question)); // bob + carol

        service.submitReview(1, 10, Verdict.CORRECT, "ok");
        assertEquals(1, service.countAnswersAwaitingReview(question)); // carol only
    }

    // A question with no answers has nothing to review.
    @Test
    void countAnswersAwaitingReviewZeroForNoAnswers() {
        Question empty = new Question(2, "MAT 117", 1, "Empty", "", "alice", T0);
        assertEquals(0, service.countAnswersAwaitingReview(empty));
    }

    // ---------- getQuestionsAwaitingReview ----------

    // Lists questions with something to review, oldest first; fully-reviewed ones are left out.
    @Test
    void getQuestionsAwaitingReviewOldestFirst() {
        Question newer = new Question(2, "MAT 117", 1, "Newer", "", "alice", T0.plusDays(1));
        newer.addAnswer(new Answer(20, "ans", "bob", T0));
        Question older = new Question(3, "MAT 117", 1, "Older", "", "alice", T0.minusDays(1));
        older.addAnswer(new Answer(30, "ans", "bob", T0));
        Question noAnswers = new Question(4, "MAT 117", 1, "None", "", "alice", T0);
        qa.addQuestion(newer);
        qa.addQuestion(older);
        qa.addQuestion(noAnswers);

        assertEquals(List.of(older, question, newer), service.getQuestionsAwaitingReview());

        service.submitReview(1, 10, Verdict.CORRECT, "ok");
        assertEquals(List.of(older, newer), service.getQuestionsAwaitingReview());
    }

    // Only reviewers can see this list.
    @Test
    void getQuestionsAwaitingReviewRequiresReviewer() {
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.getQuestionsAwaitingReview());
    }
}
