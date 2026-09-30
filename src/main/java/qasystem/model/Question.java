package qasystem.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A question asked in one chapter of one course.
 * Owns its answers, and is the only place that knows which answer was accepted.
 */
public class Question {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_BODY_LENGTH = 5000;

    private final long id;
    private final String courseCode;
    private final int chapterNumber;
    private final String title;
    private final String body;
    private final String author;          // username, not the User object
    private final LocalDateTime createdAt;
    private final ArrayList<Answer> answers;
    private Answer acceptedAnswer;        // null = unresolved

    public Question(long id, String courseCode, int chapterNumber, String title, String body,
                    String author, LocalDateTime createdAt) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Question title is required.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "Question title must be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        String cleanBody = (body == null) ? "" : body.trim(); // details are optional
        if (cleanBody.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException(
                "Question details must be at most " + MAX_BODY_LENGTH + " characters.");
        }
        this.id = id;
        this.courseCode = courseCode;
        this.chapterNumber = chapterNumber;
        this.title = title.trim();
        this.body = cleanBody;
        this.author = author;
        this.createdAt = createdAt;
        this.answers = new ArrayList<>();
        this.acceptedAnswer = null;
    }

    // ---------- Getters ----------

    public long getId() {
        return id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ---------- Answers ----------

    /** Read-only snapshot, oldest answer first. */
    public List<Answer> getAnswers() {
        return List.copyOf(answers);
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    /** Returns the answer with this id, or null if it is not on this question. */
    public Answer findAnswer(long answerId) {
        for (Answer answer : answers) {
            if (answer.getId() == answerId) {
                return answer;
            }
        }
        return null;
    }

    // ---------- Resolution ----------

    /** Derived, never stored separately - so it can never disagree with acceptedAnswer. */
    public boolean isResolved() {
        return acceptedAnswer != null;
    }

    public Answer getAcceptedAnswer() {
        return acceptedAnswer;
    }

    /**
     * Marks one of this question's answers as the accepted one.
     * @throws IllegalStateException if already resolved
     * @throws IllegalArgumentException if the answer is not on this question
     */
    public void markResolved(long answerId) {
        if (isResolved()) {
            throw new IllegalStateException("This question is already resolved.");
        }
        Answer answer = findAnswer(answerId);
        if (answer == null) {
            throw new IllegalArgumentException("That answer does not belong to this question.");
        }
        this.acceptedAnswer = answer;
    }
}
