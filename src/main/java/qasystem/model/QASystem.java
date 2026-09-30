package qasystem.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Owns the application's state: users, invitations, courses, questions,
 * and the current session. Also keeps that state CONSISTENT:
 * deleting a course or chapter deletes its questions here, in one place.
 */
public class QASystem {

    private final HashMap<String, User> users;
    private final HashMap<String, Invitation> invitations;
    private final LinkedHashMap<String, Course> courses;     // keeps insertion order
    private final LinkedHashMap<Long, Question> questions;   // keeps creation order
    private long nextQuestionId;
    private long nextAnswerId;
    private User currentUser;
    private Role currentRole;

    public QASystem() {
        this.users = new HashMap<>();
        this.invitations = new HashMap<>();
        this.courses = new LinkedHashMap<>();
        this.questions = new LinkedHashMap<>();
        this.nextQuestionId = 1;
        this.nextAnswerId = 1;
        this.currentUser = null;
        this.currentRole = null;
    }

    // ---------- Users ----------

    /** Returns the user with this username, or null if none exists. */
    public User findUser(String username) {
        return users.get(username);
    }

    /** Adds a user. Returns false if the username is already taken. */
    public boolean addUser(User user) {
        if (users.containsKey(user.getUsername())) {
            return false;
        }
        users.put(user.getUsername(), user);
        return true;
    }

    public boolean hasNoUsers() {
        return users.isEmpty();
    }

    // ---------- Invitations ----------

    /** Returns the invitation with this code, or null if none exists. */
    public Invitation findInvitation(String code) {
        return invitations.get(code);
    }

    /** Adds an invitation. Returns false if the code already exists. */
    public boolean addInvitation(Invitation invitation) {
        if (invitations.containsKey(invitation.getCode())) {
            return false;
        }
        invitations.put(invitation.getCode(), invitation);
        return true;
    }

    // ---------- Courses and chapters ----------

    /** Returns the course with this (already normalized) code, or null. */
    public Course findCourse(String code) {
        return courses.get(code);
    }

    /** Adds a course. Returns false if the code already exists. */
    public boolean addCourse(Course course) {
        return courses.putIfAbsent(course.getCode(), course) == null;
    }

    /** Removes a course, its questions, and every user's follow of it. Returns false if it did not exist. */
    public boolean removeCourse(String code) {
        if (courses.remove(code) == null) {
            return false;
        }
        questions.values().removeIf(q -> q.getCourseCode().equals(code));
        for (User user : users.values()) {
            user.unfollowCourse(code); // no user may follow a course that no longer exists
        }
        return true;
    }

    /** Removes a chapter AND all of its questions. Returns false if it did not exist. */
    public boolean removeChapter(String courseCode, int chapterNumber) {
        Course course = courses.get(courseCode);
        if (course == null || !course.removeChapter(chapterNumber)) {
            return false;
        }
        questions.values().removeIf(q ->
                q.getCourseCode().equals(courseCode) && q.getChapterNumber() == chapterNumber);
        return true;
    }

    /** Read-only snapshot, in the order the courses were added. */
    public List<Course> getCourses() {
        return List.copyOf(courses.values());
    }

    // ---------- Questions ----------

    /** Hands out a new, never-reused question id. */
    public long generateQuestionId() {
        return nextQuestionId++;
    }

    /** Hands out a new, never-reused answer id. */
    public long generateAnswerId() {
        return nextAnswerId++;
    }

    public Question findQuestion(long id) {
        return questions.get(id);
    }

    public void addQuestion(Question question) {
        questions.put(question.getId(), question);
    }

    /** Removes a question (and, through composition, its answers). */
    public boolean removeQuestion(long id) {
        return questions.remove(id) != null;
    }

    /** Read-only snapshot, in creation order. */
    public List<Question> getQuestions() {
        return List.copyOf(questions.values());
    }

    // ---------- Session ----------

    public User getCurrentUser() {
        return currentUser;
    }

    /** Starting a new session: the previous role must not carry over. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.currentRole = null;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(Role role) {
        this.currentRole = role;
    }

    /** Ends the session. A role without a user makes no sense, so both are cleared. */
    public void clearCurrentUser() {
        this.currentUser = null;
        this.currentRole = null;
    }
}
