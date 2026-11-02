package qasystem.service;

import qasystem.model.Answer;
import qasystem.model.Course;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Role;
import qasystem.util.Keywords;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns question and answer behavior: asking, searching, answering,
 * resolving, and deleting.
 */
public class QuestionService {

    private static final int MAX_RELATED_RESULTS = 5;

    private final QASystem qaSystem;
    private final Clock clock;

    public QuestionService(QASystem qaSystem, Clock clock) {
        this.qaSystem = qaSystem;
        this.clock = clock;
    }

    // =====================================================================
    // Asking
    // =====================================================================

    /**
     * A student asks a question in one chapter of a course.
     * @throws IllegalArgumentException if the course/chapter does not exist or the text is invalid
     */
    public Question askQuestion(String courseCode, int chapterNumber, String title, String body) {
        Access.requireRole(qaSystem, Role.STUDENT, "ask questions");
        Course course = requireCourse(courseCode);
        if (course.findChapter(chapterNumber) == null) {
            throw new IllegalArgumentException(
                "Chapter " + chapterNumber + " does not exist in " + course.getCode() + ".");
        }

        // id 0 = "not saved yet". Creating it here runs the validation;
        // the database then assigns the real id.
        Question draft = new Question(
                0, course.getCode(), chapterNumber, title, body, currentUsername(), LocalDateTime.now(clock));
        return qaSystem.addQuestion(draft);
    }

    // =====================================================================
    // Reading
    // =====================================================================

    public Question findQuestion(long questionId) {
        return qaSystem.findQuestion(questionId);
    }

    /** Questions in one chapter, newest first (for browsing). */
    public List<Question> getQuestions(String courseCode, int chapterNumber) {
        String code = Course.normalizeCode(courseCode);
        List<Question> result = new ArrayList<>();
        for (Question question : qaSystem.getQuestions()) {
            if (question.getCourseCode().equals(code) && question.getChapterNumber() == chapterNumber) {
                result.add(question);
            }
        }
        result.sort(Comparator.comparing(Question::getCreatedAt).reversed());
        return result;
    }

    /** Every unresolved question, oldest first (longest-waiting at the top). */
    public List<Question> getUnresolvedQuestions() {
        List<Question> result = new ArrayList<>();
        for (Question question : qaSystem.getQuestions()) {
            if (!question.isResolved()) {
                result.add(question);
            }
        }
        result.sort(Comparator.comparing(Question::getCreatedAt));
        return result;
    }

    /** Unresolved questions in one course, oldest first. */
    public List<Question> getUnresolvedQuestions(String courseCode) {
        String code = Course.normalizeCode(courseCode);
        List<Question> result = new ArrayList<>();
        for (Question question : getUnresolvedQuestions()) {
            if (question.getCourseCode().equals(code)) {
                result.add(question);
            }
        }
        return result;
    }

    /**
     * Existing questions in the same course that share keywords with a new one.
     * Sorted by: most shared keywords, then resolved first, then newest. At most 5.
     */
    public List<Question> findRelatedQuestions(String courseCode, String title, String body) {
        Set<String> target = Keywords.extract(title + " " + (body == null ? "" : body));
        if (target.isEmpty()) {
            return List.of();
        }
        String code = Course.normalizeCode(courseCode);

        Map<Question, Integer> scores = new HashMap<>();
        for (Question question : qaSystem.getQuestions()) {
            if (!question.getCourseCode().equals(code)) {
                continue;
            }
            Set<String> shared = Keywords.extract(question.getTitle() + " " + question.getBody());
            shared.retainAll(target); // keep only words both questions have
            if (!shared.isEmpty()) {
                scores.put(question, shared.size());
            }
        }

        List<Question> matches = new ArrayList<>(scores.keySet());
        matches.sort(Comparator.comparing((Question q) -> scores.get(q)).reversed()
                .thenComparing(Question::isResolved, Comparator.reverseOrder())
                .thenComparing(Question::getCreatedAt, Comparator.reverseOrder()));

        return List.copyOf(matches.subList(0, Math.min(MAX_RELATED_RESULTS, matches.size())));
    }

    // =====================================================================
    // Counts (for the card statistics)
    // =====================================================================

    public int countQuestions(String courseCode) {
        return qaSystem.countQuestions(Course.normalizeCode(courseCode), null, false);
    }

    public int countQuestions(String courseCode, int chapterNumber) {
        return qaSystem.countQuestions(Course.normalizeCode(courseCode), chapterNumber, false);
    }

    public int countUnresolved(String courseCode) {
        return qaSystem.countQuestions(Course.normalizeCode(courseCode), null, true);
    }

    public int countUnresolved(String courseCode, int chapterNumber) {
        return qaSystem.countQuestions(Course.normalizeCode(courseCode), chapterNumber, true);
    }

    // =====================================================================
    // Answering and resolving
    // =====================================================================

    /**
     * A student proposes an answer to an unresolved question.
     * @throws IllegalArgumentException if the question does not exist, is resolved, or text is invalid
     */
    public Answer proposeAnswer(long questionId, String text) {
        Access.requireRole(qaSystem, Role.STUDENT, "answer questions");
        Question question = requireQuestion(questionId);
        if (question.isResolved()) {
            throw new IllegalArgumentException("This question is already resolved.");
        }
        Answer draft = new Answer(0, text, currentUsername(), LocalDateTime.now(clock)); // validates
        return qaSystem.addAnswer(question.getId(), draft);
    }

    /**
     * The student who asked marks one answer as the one that solved it.
     * @throws IllegalStateException if someone else tries, or it is already resolved
     */
    public void resolveQuestion(long questionId, long answerId) {
        Access.requireRole(qaSystem, Role.STUDENT, "resolve questions");
        Question question = requireQuestion(questionId);
        if (!question.getAuthor().equals(currentUsername())) {
            throw new IllegalStateException("Only the student who asked can mark it resolved.");
        }
        qaSystem.resolveQuestion(question, answerId);
    }

    // =====================================================================
    // Deleting (admin)
    // =====================================================================

    public void deleteQuestion(long questionId) {
        Access.requireRole(qaSystem, Role.ADMIN, "delete questions");
        if (!qaSystem.removeQuestion(questionId)) {
            throw new IllegalArgumentException("That question no longer exists.");
        }
    }

    // =====================================================================
    // Permission checks - the UI asks these instead of copying the rules
    // =====================================================================

    public boolean canAsk() {
        return qaSystem.getCurrentRole() == Role.STUDENT;
    }

    public boolean canAnswer(Question question) {
        return qaSystem.getCurrentRole() == Role.STUDENT && !question.isResolved();
    }

    /** Same checks resolveQuestion() enforces: student, the asker, still unresolved. */
    public boolean canResolve(Question question) {
        return qaSystem.getCurrentRole() == Role.STUDENT
                && !question.isResolved()
                && question.getAuthor().equals(currentUsername());
    }

    public boolean canDelete() {
        return qaSystem.getCurrentRole() == Role.ADMIN;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String currentUsername() {
        return qaSystem.getCurrentUser().getUsername(); // requireRole guarantees someone is logged in
    }

    private Course requireCourse(String courseCode) {
        Course course = qaSystem.findCourse(Course.normalizeCode(courseCode));
        if (course == null) {
            throw new IllegalArgumentException("Course " + Course.normalizeCode(courseCode) + " does not exist.");
        }
        return course;
    }

    private Question requireQuestion(long questionId) {
        Question question = qaSystem.findQuestion(questionId);
        if (question == null) {
            throw new IllegalArgumentException("That question no longer exists.");
        }
        return question;
    }
}
