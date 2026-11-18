package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.model.Answer;
import qasystem.model.Question;
import qasystem.model.Role;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QuestionService: asking, answering, resolving, searching")
class QuestionServiceTest extends ServiceTestBase {

    @BeforeEach
    void setUpCourseAndStudents() {
        createCourse("MAT 117", "Calculus I", 1, 2);
        createStudent("alice");
        createStudent("bob");
    }

    // ---------- Asking ----------

    @Test
    @DisplayName("A question must go into an existing course and chapter")
    void askNeedsExistingChapter() {
        loginAs("alice", Role.STUDENT);

        assertThrows(IllegalArgumentException.class,
                () -> questionService.askQuestion("MAT 117", 9, "Title", ""));
        assertThrows(IllegalArgumentException.class,
                () -> questionService.askQuestion("PHY 101", 1, "Title", ""));
    }

    @Test
    @DisplayName("Only students can ask questions")
    void onlyStudentsAsk() {
        loginAs(ADMIN, Role.ADMIN);

        assertThrows(IllegalStateException.class,
                () -> questionService.askQuestion("MAT 117", 1, "Title", ""));
    }

    @Test
    @DisplayName("A chapter's questions are listed newest first")
    void chapterListNewestFirst() {
        loginAs("alice", Role.STUDENT);
        Question older = questionService.askQuestion("MAT 117", 1, "First question", "");
        clock.advance(Duration.ofMinutes(5));
        Question newer = questionService.askQuestion("MAT 117", 1, "Second question", "");

        List<Question> list = questionService.getQuestions("MAT 117", 1);

        assertEquals(List.of(newer, older), list);
    }

    // ---------- Answering and resolving ----------

    @Test
    @DisplayName("Only the student who asked can mark an answer as accepted")
    void onlyAskerResolves() {
        loginAs("alice", Role.STUDENT);
        Question question = questionService.askQuestion("MAT 117", 1, "What is a limit?", "");
        loginAs("bob", Role.STUDENT);
        Answer answer = questionService.proposeAnswer(question.getId(), "The value f approaches.");

        assertFalse(questionService.canResolve(question));
        assertThrows(IllegalStateException.class,
                () -> questionService.resolveQuestion(question.getId(), answer.getId()));

        loginAs("alice", Role.STUDENT);
        assertTrue(questionService.canResolve(question));
        questionService.resolveQuestion(question.getId(), answer.getId());

        Question saved = questionService.findQuestion(question.getId()); // re-read from the database
        assertTrue(saved.isResolved());
        assertEquals(answer, saved.getAcceptedAnswer());
    }

    @Test
    @DisplayName("A resolved question accepts no new answers and cannot be resolved again")
    void resolvedIsClosed() {
        loginAs("alice", Role.STUDENT);
        Question question = questionService.askQuestion("MAT 117", 1, "What is a limit?", "");
        Answer answer = questionService.proposeAnswer(question.getId(), "My own answer.");
        questionService.resolveQuestion(question.getId(), answer.getId());

        loginAs("bob", Role.STUDENT);
        // "question" is the old snapshot from before it was resolved: re-read it.
        assertFalse(questionService.canAnswer(questionService.findQuestion(question.getId())));
        assertThrows(IllegalArgumentException.class,
                () -> questionService.proposeAnswer(question.getId(), "Too late."));

        loginAs("alice", Role.STUDENT);
        assertThrows(IllegalStateException.class,
                () -> questionService.resolveQuestion(question.getId(), answer.getId()));
    }

    @Test
    @DisplayName("You cannot accept an answer that belongs to a different question")
    void acceptOnlyOwnAnswers() {
        loginAs("alice", Role.STUDENT);
        Question first = questionService.askQuestion("MAT 117", 1, "Question one", "");
        Question second = questionService.askQuestion("MAT 117", 1, "Question two", "");
        Answer answerToSecond = questionService.proposeAnswer(second.getId(), "Answer to two.");

        assertThrows(IllegalArgumentException.class,
                () -> questionService.resolveQuestion(first.getId(), answerToSecond.getId()));
        assertFalse(questionService.findQuestion(first.getId()).isResolved());
    }

    @Test
    @DisplayName("Unresolved list is oldest first and leaves out resolved questions")
    void unresolvedListOldestFirst() {
        loginAs("alice", Role.STUDENT);
        Question oldest = questionService.askQuestion("MAT 117", 1, "Oldest", "");
        clock.advance(Duration.ofMinutes(1));
        Question solved = questionService.askQuestion("MAT 117", 1, "Solved", "");
        clock.advance(Duration.ofMinutes(1));
        Question newest = questionService.askQuestion("MAT 117", 2, "Newest", "");
        Answer answer = questionService.proposeAnswer(solved.getId(), "Answer.");
        questionService.resolveQuestion(solved.getId(), answer.getId());

        assertEquals(List.of(oldest, newest), questionService.getUnresolvedQuestions());
    }

    // ---------- Related questions ----------

    @Test
    @DisplayName("Related questions share keywords; stop words alone match nothing")
    void relatedQuestions() {
        loginAs("alice", Role.STUDENT);
        Question derivative = questionService.askQuestion("MAT 117", 1, "How do I find the derivative of x squared?", "");
        questionService.askQuestion("MAT 117", 1, "What is a limit?", "");

        List<Question> related = questionService.findRelatedQuestions("MAT 117", "Derivatives of polynomials", "");

        assertEquals(List.of(derivative), related);
        assertTrue(questionService.findRelatedQuestions("MAT 117", "how do I", "").isEmpty());
    }

    // ---------- Deleting ----------

    @Test
    @DisplayName("Only an admin can delete a question")
    void onlyAdminDeletes() {
        loginAs("alice", Role.STUDENT);
        Question question = questionService.askQuestion("MAT 117", 1, "What is a limit?", "");

        assertThrows(IllegalStateException.class, () -> questionService.deleteQuestion(question.getId()));

        loginAs(ADMIN, Role.ADMIN);
        questionService.deleteQuestion(question.getId());

        assertNull(questionService.findQuestion(question.getId()));
    }
}
