package qasystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import qasystem.data.Database;
import qasystem.model.Answer;
import qasystem.model.Course;
import qasystem.model.Invitation;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Review;
import qasystem.model.Role;
import qasystem.model.Verdict;
import qasystem.service.CourseService;
import qasystem.service.InvitationService;
import qasystem.service.QuestionService;
import qasystem.service.ReviewService;
import qasystem.service.UserService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests: one group per user story.
 * These go through the services like a real user would (log in, then do stuff),
 * instead of testing one class at a time like the other test files.
 */
@DisplayName("User stories")
class UserStoryAcceptanceTest {

    // lets us jump forward in time (for expired invites)
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static final String PW = "Passw0rd!";

    private MutableClock clock;
    private Database database;
    private QASystem qa;
    private UserService users;
    private InvitationService invites;
    private CourseService courses;
    private QuestionService questions;
    private ReviewService reviews;

    // Starting point for every test:
    // admin account, a course CSE 360 with chapter 1, two students (alice, bob)
    // and a reviewer (rita) who joined with an invite and is also a student.
    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        database = Database.inMemory(); // the team's version stores data in a database; tests use a fresh in-memory one
        qa = new QASystem(database);
        users = new UserService(qa, clock);
        invites = new InvitationService(qa, clock);
        courses = new CourseService(qa);
        questions = new QuestionService(qa, clock);
        reviews = new ReviewService(qa, clock);

        users.registerFirstUser("admin", PW);
        users.login("admin", PW);
        courses.createCourse("CSE 360", "Software Engineering");
        courses.addChapter("CSE 360", 1, "Intro");
        Invitation ritaInvite = invites.createInvitation(Set.of(Role.REVIEWER, Role.STUDENT), 7);
        users.logout();

        users.registerUser("alice", PW, null);
        users.registerUser("bob", PW, null);
        users.registerUser("rita", PW, ritaInvite.getCode());
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private void loginAs(String username) {
        users.logout();
        assertTrue(users.login(username, PW));
    }

    private void loginAs(String username, Role role) {
        loginAs(username);
        users.selectRole(role);
    }

    // =====================================================================

    @Nested
    @DisplayName("US-01: As a new user, I can create an account and log in")
    class RegisterAndLogIn {

        @Test
        @DisplayName("a new user without an invite code becomes a student and can log in")
        void signUpBecomesStudent() {
            users.registerUser("carlos", PW, null);
            assertTrue(users.login("carlos", PW));
            // only one role, so it gets picked automatically
            assertEquals(Role.STUDENT, users.getCurrentRole());
        }

        @Test
        @DisplayName("a weak password is rejected and the user is told why")
        void weakPasswordRejected() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> users.registerUser("carlos", "abc", null));
            assertEquals("Password needs: at least 8 characters.", e.getMessage());
        }

        @Test
        @DisplayName("a username that is already taken is rejected")
        void duplicateUsernameRejected() {
            assertThrows(IllegalArgumentException.class, () -> users.registerUser("alice", PW, null));
        }

        @Test
        @DisplayName("logging in with the wrong password fails")
        void wrongPasswordFails() {
            assertFalse(users.login("alice", "WrongPass1!"));
            assertNull(users.getCurrentUser());
        }

        @Test
        @DisplayName("a user with two roles has to pick one after logging in")
        void twoRolesMustPick() {
            assertTrue(users.login("rita", PW));
            assertTrue(users.needsRoleSelection());
            users.selectRole(Role.REVIEWER);
            assertEquals(Role.REVIEWER, users.getCurrentRole());
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-02: As an admin, I can invite people with a one-time code")
    class InviteUsers {

        @Test
        @DisplayName("an admin gets an 8-character invite code")
        void adminCreatesCode() {
            loginAs("admin");
            Invitation inv = invites.createInvitation(Set.of(Role.REVIEWER), 3);
            assertEquals(8, inv.getCode().length());
        }

        @Test
        @DisplayName("someone who signs up with the code gets the roles from the invite")
        void codeGivesRoles() {
            loginAs("admin");
            String code = invites.createInvitation(Set.of(Role.REVIEWER), 3).getCode();
            users.logout();

            users.registerUser("dana", PW, code);
            assertEquals(Set.of(Role.REVIEWER), qa.findUser("dana").getRoles());
        }

        @Test
        @DisplayName("an invite code only works once")
        void codeOnlyOnce() {
            loginAs("admin");
            String code = invites.createInvitation(Set.of(Role.REVIEWER), 3).getCode();
            users.logout();

            users.registerUser("dana", PW, code);
            assertThrows(IllegalArgumentException.class, () -> users.registerUser("eli", PW, code));
        }

        @Test
        @DisplayName("an invite code stops working after it expires")
        void expiredCodeRejected() {
            loginAs("admin");
            String code = invites.createInvitation(Set.of(Role.REVIEWER), 1).getCode();
            users.logout();

            clock.advance(Duration.ofDays(2)); // valid for 1 day, now 2 days later
            assertThrows(IllegalArgumentException.class, () -> users.registerUser("dana", PW, code));
        }

        @Test
        @DisplayName("a student is not allowed to create invites")
        void studentCantInvite() {
            loginAs("alice");
            assertThrows(IllegalStateException.class,
                    () -> invites.createInvitation(Set.of(Role.ADMIN), 3));
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-03: As an admin, I can manage courses and chapters")
    class ManageCourses {

        @Test
        @DisplayName("an admin can create a course and add chapters to it")
        void adminCreatesCourse() {
            loginAs("admin");
            courses.createCourse("MAT 117", "Calculus I");
            courses.addChapter("MAT 117", 1, "Limits");
            courses.addChapter("MAT 117", 2, "Derivatives");

            assertEquals(2, courses.findCourse("MAT 117").getChapters().size());
        }

        @Test
        @DisplayName("the same course can't be created twice, even if it's typed differently")
        void noDuplicateCourse() {
            loginAs("admin");
            // "cse360" is the same course as "CSE 360"
            assertThrows(IllegalArgumentException.class, () -> courses.createCourse("cse360", "Again"));
        }

        @Test
        @DisplayName("a student is not allowed to create courses")
        void studentCantCreateCourse() {
            loginAs("alice");
            assertThrows(IllegalStateException.class, () -> courses.createCourse("MAT 117", "Calculus I"));
        }

        @Test
        @DisplayName("deleting a chapter also deletes the questions in it")
        void deleteChapterDeletesQuestions() {
            loginAs("alice");
            Question q = questions.askQuestion("CSE 360", 1, "What is a user story?", "");

            loginAs("admin");
            courses.deleteChapter("CSE 360", 1);
            assertNull(questions.findQuestion(q.getId()));
        }

        @Test
        @DisplayName("deleting a course removes it along with its questions")
        void deleteCourseDeletesQuestions() {
            loginAs("alice");
            Question q = questions.askQuestion("CSE 360", 1, "What is a user story?", "");

            loginAs("admin");
            courses.deleteCourse("CSE 360");
            assertNull(courses.findCourse("CSE 360"));
            assertNull(questions.findQuestion(q.getId()));
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-04: As a student, I can find and follow courses")
    class FollowCourses {

        @Test
        @DisplayName("a student can search for a course by part of its name")
        void searchByTitle() {
            loginAs("alice");
            List<Course> found = courses.searchCourses("software");
            assertEquals("CSE 360", found.get(0).getCode());
        }

        @Test
        @DisplayName("a followed course shows up in the student's course list")
        void followShowsUp() {
            loginAs("alice");
            courses.followCourse("cse360");
            assertEquals(1, courses.getFollowedCourses().size());
        }

        @Test
        @DisplayName("unfollowing removes it from the list again")
        void unfollow() {
            loginAs("alice");
            courses.followCourse("CSE 360");
            courses.unfollowCourse("CSE 360");
            assertTrue(courses.getFollowedCourses().isEmpty());
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-05: As a student, I can ask a question in a chapter")
    class AskQuestion {

        @Test
        @DisplayName("a student can ask a question and it shows up in that chapter")
        void askShowsUp() {
            loginAs("alice");
            Question q = questions.askQuestion("CSE 360", 1, "What is a user story?", "Not sure what goes in one");

            assertEquals(List.of(q), questions.getQuestions("CSE 360", 1));
            assertEquals("alice", q.getAuthor());
        }

        @Test
        @DisplayName("similar questions that were already asked get suggested")
        void relatedSuggested() {
            loginAs("alice");
            Question old = questions.askQuestion("CSE 360", 1, "How do I write JUnit tests?", "");

            loginAs("bob");
            assertEquals(List.of(old), questions.findRelatedQuestions("CSE 360", "JUnit test help", ""));
        }

        @Test
        @DisplayName("asking in a chapter that doesn't exist is rejected")
        void missingChapterRejected() {
            loginAs("alice");
            assertThrows(IllegalArgumentException.class,
                    () -> questions.askQuestion("CSE 360", 9, "Anyone?", ""));
        }

        @Test
        @DisplayName("a question without a title is rejected")
        void noTitleRejected() {
            loginAs("alice");
            assertThrows(IllegalArgumentException.class,
                    () -> questions.askQuestion("CSE 360", 1, "   ", "body only"));
        }

        @Test
        @DisplayName("an admin is not allowed to ask questions")
        void adminCantAsk() {
            loginAs("admin");
            assertThrows(IllegalStateException.class,
                    () -> questions.askQuestion("CSE 360", 1, "Admin question", ""));
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-06: As a student, I can answer questions and mark the one that solved mine")
    class AnswerAndResolve {

        private Question question;

        // alice asks something before each test in this group
        @BeforeEach
        void aliceAsks() {
            loginAs("alice");
            question = questions.askQuestion("CSE 360", 1, "What is a user story?", "");
        }

        @Test
        @DisplayName("another student can answer an open question")
        void bobAnswers() {
            loginAs("bob");
            Answer a = questions.proposeAnswer(question.getId(), "It's a feature from the user's point of view");
            // The database version returns snapshots, so read the question again to see the change.
            Question fresh = questions.findQuestion(question.getId());
            assertEquals(1, fresh.getAnswers().size());
            assertEquals(a.getId(), fresh.getAnswers().get(0).getId());
        }

        @Test
        @DisplayName("the student who asked can mark an answer as the solution")
        void askerResolves() {
            loginAs("bob");
            Answer a = questions.proposeAnswer(question.getId(), "It's a feature from the user's point of view");

            loginAs("alice");
            questions.resolveQuestion(question.getId(), a.getId());
            Question fresh = questions.findQuestion(question.getId()); // read again (snapshot)
            assertTrue(fresh.isResolved());
            assertEquals(a.getId(), fresh.getAcceptedAnswer().getId());
        }

        @Test
        @DisplayName("only the student who asked can mark it solved")
        void onlyAskerResolves() {
            loginAs("bob");
            Answer a = questions.proposeAnswer(question.getId(), "my answer");
            // bob tries to accept his own answer on alice's question
            assertThrows(IllegalStateException.class,
                    () -> questions.resolveQuestion(question.getId(), a.getId()));
        }

        @Test
        @DisplayName("a solved question can't get any more answers")
        void solvedTakesNoAnswers() {
            loginAs("bob");
            Answer a = questions.proposeAnswer(question.getId(), "first");
            loginAs("alice");
            questions.resolveQuestion(question.getId(), a.getId());

            loginAs("bob");
            assertThrows(IllegalArgumentException.class,
                    () -> questions.proposeAnswer(question.getId(), "too late"));
        }

        @Test
        @DisplayName("solved questions drop off the unresolved list")
        void leavesUnresolvedList() {
            assertEquals(List.of(question), questions.getUnresolvedQuestions());

            loginAs("bob");
            Answer a = questions.proposeAnswer(question.getId(), "answer");
            loginAs("alice");
            questions.resolveQuestion(question.getId(), a.getId());

            assertTrue(questions.getUnresolvedQuestions().isEmpty());
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("US-07: As a reviewer, I can review answers")
    class ReviewAnswers {

        private Question question;
        private Answer bobsAnswer;

        /** The answer's reviews as saved now (services hand out snapshots, so look it up again). */
        private List<Review> reviewsOf(Answer answer) {
            for (Answer a : questions.findQuestion(question.getId()).getAnswers()) {
                if (a.getId() == answer.getId()) {
                    return a.getReviews();
                }
            }
            return List.of();
        }

        // alice asks, bob answers
        @BeforeEach
        void questionWithAnswer() {
            loginAs("alice");
            question = questions.askQuestion("CSE 360", 1, "What is a user story?", "");
            loginAs("bob");
            bobsAnswer = questions.proposeAnswer(question.getId(), "A feature from the user's view");
        }

        @Test
        @DisplayName("a reviewer can review a student's answer")
        void reviewerReviews() {
            loginAs("rita", Role.REVIEWER);
            Review r = reviews.submitReview(question.getId(), bobsAnswer.getId(), Verdict.CORRECT, "Good answer");
            List<Review> saved = reviewsOf(bobsAnswer); // read again (snapshot)
            assertEquals(1, saved.size());
            assertEquals(r.getReviewer(), saved.get(0).getReviewer());
            assertEquals(Verdict.CORRECT, saved.get(0).getVerdict());
        }

        @Test
        @DisplayName("reviewing the same answer again updates the review instead of adding a second one")
        void secondReviewUpdates() {
            loginAs("rita", Role.REVIEWER);
            reviews.submitReview(question.getId(), bobsAnswer.getId(), Verdict.CORRECT, "Good");
            reviews.submitReview(question.getId(), bobsAnswer.getId(), Verdict.PARTIALLY_CORRECT, "Missing an example");

            List<Review> saved = reviewsOf(bobsAnswer); // read again (snapshot)
            assertEquals(1, saved.size());
            assertEquals(Verdict.PARTIALLY_CORRECT, saved.get(0).getVerdict());
        }

        @Test
        @DisplayName("a reviewer can't review their own answer")
        void cantReviewOwn() {
            // rita is also a student, so she answers first
            loginAs("rita", Role.STUDENT);
            Answer ritas = questions.proposeAnswer(question.getId(), "rita's answer");

            loginAs("rita", Role.REVIEWER);
            assertThrows(IllegalStateException.class,
                    () -> reviews.submitReview(question.getId(), ritas.getId(), Verdict.CORRECT, "I'm right"));
        }

        @Test
        @DisplayName("a student is not allowed to review answers")
        void studentCantReview() {
            loginAs("alice");
            assertThrows(IllegalStateException.class,
                    () -> reviews.submitReview(question.getId(), bobsAnswer.getId(), Verdict.CORRECT, "nice"));
        }

        @Test
        @DisplayName("answers waiting for a review show up in the reviewer's list until they're reviewed")
        void awaitingReviewList() {
            loginAs("rita", Role.REVIEWER);
            assertEquals(List.of(question), reviews.getQuestionsAwaitingReview());

            reviews.submitReview(question.getId(), bobsAnswer.getId(), Verdict.CORRECT, "ok");
            assertTrue(reviews.getQuestionsAwaitingReview().isEmpty());
        }
    }
}
