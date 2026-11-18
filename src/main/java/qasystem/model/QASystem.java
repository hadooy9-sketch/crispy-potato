package qasystem.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qasystem.data.DataAccessException;
import qasystem.data.Database;

/**
 * The single gateway to the application's data.
 *
 * Data (users, courses, questions...) lives in the DATABASE, so it survives
 * closing the app, and later can be shared online. Every method here is SQL.
 * Services never touch the database directly - they call these methods.
 *
 * The session (who is logged in, in which role) stays in MEMORY:
 * it belongs to this one running app, not to everyone.
 *
 * Objects returned are snapshots: changing one does not change the database.
 * To change data, call a QASystem method.
 */
public class QASystem {

    private final Database database;
    private User currentUser;
    private Role currentRole;

    public QASystem(Database database) {
        this.database = database;
    }

    /** Runs several QASystem calls as one unit: all are saved, or none are. */
    public void inTransaction(Runnable steps) {
        database.transaction(c -> {
            steps.run();
            return null;
        });
    }

    // =====================================================================
    // Users
    // =====================================================================

    /** Returns the user with this username, or null if none exists. */
    public User findUser(String username) {
        return database.query(c -> {
            String hash;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    hash = rs.getString(1);
                }
            }
            return User.fromStorage(username, hash, loadRoles(c, "user_roles", "username", username));
        });
    }

    /** Saves a new user with their roles. Returns false if the username is already taken. */
    public boolean addUser(User user) {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.executeUpdate();
            } catch (SQLException e) {
                if (Database.isDuplicateKey(e)) {
                    return false;
                }
                throw e;
            }
            saveRoles(c, "user_roles", "username", user.getUsername(), user.getRoles());
            return true;
        });
    }

    /**
     * Removes a user account. Returns false if no user has that name.
     * If they never posted anything, the row is deleted. If they did, their questions, answers
     * and reviews still point at their username (the database requires it), so the account is
     * closed instead: roles and follows are removed and the password can never match again.
     * Their posts stay, with their name on them.
     */
    public boolean removeUser(String username) {
        return database.transaction(c -> {
            boolean posted = countWhere(c, "SELECT COUNT(*) FROM questions WHERE author = ?", username)
                    + countWhere(c, "SELECT COUNT(*) FROM answers WHERE author = ?", username)
                    + countWhere(c, "SELECT COUNT(*) FROM reviews WHERE reviewer = ?", username) > 0;
            if (!posted) {
                try (PreparedStatement ps = prepare(c, "DELETE FROM users WHERE username = ?", username)) {
                    return ps.executeUpdate() > 0; // CASCADE removes their roles and follows
                }
            }
            try (PreparedStatement ps = prepare(c, "DELETE FROM user_roles WHERE username = ?", username)) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = prepare(c, "DELETE FROM follows WHERE username = ?", username)) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = prepare(c,
                    "UPDATE users SET password_hash = ? WHERE username = ?", CLOSED_ACCOUNT, username)) {
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Stored instead of a password hash for a closed account. It never matches any password. */
    private static final String CLOSED_ACCOUNT = "closed-account";

    /** How many users have the given role (used to protect the last admin). */
    public int countUsersWithRole(Role role) {
        return database.query(c -> (int) countWhere(c, "SELECT COUNT(*) FROM user_roles WHERE role = ?", role.name()));
    }

    private static long countWhere(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public boolean hasNoUsers() {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) == 0;
            }
        });
    }

    // =====================================================================
    // Invitations
    // =====================================================================

    /** Returns the invitation with this code, or null if none exists. */
    public Invitation findInvitation(String code) {
        return database.query(c -> {
            LocalDateTime expiresAt;
            boolean used;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT expires_at, used FROM invitations WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    expiresAt = rs.getObject("expires_at", LocalDateTime.class);
                    used = rs.getBoolean("used");
                }
            }
            return new Invitation(code, loadRoles(c, "invitation_roles", "code", code), expiresAt, used);
        });
    }

    /** Saves a new invitation. Returns false if the code already exists. */
    public boolean addInvitation(Invitation invitation) {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO invitations (code, expires_at, used) VALUES (?, ?, FALSE)")) {
                ps.setString(1, invitation.getCode());
                ps.setObject(2, invitation.getExpiresAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                if (Database.isDuplicateKey(e)) {
                    return false;
                }
                throw e;
            }
            saveRoles(c, "invitation_roles", "code", invitation.getCode(), invitation.getRoles());
            return true;
        });
    }

    /**
     * Uses up an invitation - in ONE statement, so it is safe even if two people
     * try the same code at the same moment: only one UPDATE can match.
     * Returns false if it does not exist, is already used, or has expired.
     */
    public boolean claimInvitation(String code, LocalDateTime now) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE invitations SET used = TRUE WHERE code = ? AND used = FALSE AND expires_at >= ?")) {
                ps.setString(1, code);
                ps.setObject(2, now);
                return ps.executeUpdate() == 1;
            }
        });
    }

    // =====================================================================
    // Courses and chapters
    // =====================================================================

    /** Returns the course (with its chapters) for this normalized code, or null. */
    public Course findCourse(String code) {
        return database.query(c -> {
            Course course;
            try (PreparedStatement ps = c.prepareStatement("SELECT title FROM courses WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    course = new Course(code, rs.getString("title"));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, title FROM chapters WHERE course_code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        course.addChapter(new Chapter(rs.getInt("number"), rs.getString("title")));
                    }
                }
            }
            return course;
        });
    }

    /** All courses with their chapters, in the order they were added. Two queries in total. */
    public List<Course> getCourses() {
        return database.query(c -> {
            Map<String, Course> courses = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT code, title FROM courses ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course course = new Course(rs.getString("code"), rs.getString("title"));
                    courses.put(course.getCode(), course);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT course_code, number, title FROM chapters");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course course = courses.get(rs.getString("course_code"));
                    if (course != null) {
                        course.addChapter(new Chapter(rs.getInt("number"), rs.getString("title")));
                    }
                }
            }
            return List.copyOf(courses.values());
        });
    }

    /** Saves a new course. Returns false if the code already exists. */
    public boolean addCourse(Course course) {
        return insertIgnoringDuplicate("INSERT INTO courses (code, title) VALUES (?, ?)",
                course.getCode(), course.getTitle());
    }

    /**
     * Deletes a course. The database's ON DELETE CASCADE rules also delete its chapters,
     * their questions, answers and reviews, and every follow of it.
     */
    public boolean removeCourse(String code) {
        return update("DELETE FROM courses WHERE code = ?", code) > 0;
    }

    /** Saves a new chapter. Returns false if that number already exists in the course. */
    public boolean addChapter(String courseCode, Chapter chapter) {
        return insertIgnoringDuplicate("INSERT INTO chapters (course_code, number, title) VALUES (?, ?, ?)",
                courseCode, chapter.getNumber(), chapter.getTitle());
    }

    /** Deletes a chapter; CASCADE deletes its questions (and their answers and reviews). */
    public boolean removeChapter(String courseCode, int chapterNumber) {
        return update("DELETE FROM chapters WHERE course_code = ? AND number = ?", courseCode, chapterNumber) > 0;
    }

    // =====================================================================
    // Following courses
    // =====================================================================

    /** Returns false if the user already follows the course. */
    public boolean followCourse(String username, String courseCode) {
        return insertIgnoringDuplicate("INSERT INTO follows (username, course_code) VALUES (?, ?)",
                username, courseCode);
    }

    public boolean unfollowCourse(String username, String courseCode) {
        return update("DELETE FROM follows WHERE username = ? AND course_code = ?", username, courseCode) > 0;
    }

    public boolean isFollowing(String username, String courseCode) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM follows WHERE username = ? AND course_code = ?")) {
                ps.setString(1, username);
                ps.setString(2, courseCode);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** The course codes this user follows, in the order they followed them. */
    public List<String> getFollowedCourseCodes(String username) {
        return database.query(c -> {
            List<String> codes = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT course_code FROM follows WHERE username = ? ORDER BY id")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        codes.add(rs.getString(1));
                    }
                }
            }
            return codes;
        });
    }

    // =====================================================================
    // Questions
    // =====================================================================

    /**
     * Saves a new question. The DATABASE picks the id (so two people can never get
     * the same one), and the saved copy with its real id is returned.
     */
    public Question addQuestion(Question draft) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO questions (course_code, chapter_number, title, body, author, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", new String[] {"id"})) {
                ps.setString(1, draft.getCourseCode());
                ps.setInt(2, draft.getChapterNumber());
                ps.setString(3, draft.getTitle());
                ps.setString(4, draft.getBody());
                ps.setString(5, draft.getAuthor());
                ps.setObject(6, draft.getCreatedAt());
                ps.executeUpdate();
                long id = generatedId(ps);
                return new Question(id, draft.getCourseCode(), draft.getChapterNumber(), draft.getTitle(),
                        draft.getBody(), draft.getAuthor(), draft.getCreatedAt());
            }
        });
    }

    /** One question with its answers and reviews, or null. */
    public Question findQuestion(long id) {
        List<Question> found = loadQuestions(" WHERE q.id = ?", id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Every question with its answers and reviews, oldest first. */
    public List<Question> getQuestions() {
        return loadQuestions("");
    }

    /** Deletes a question; CASCADE deletes its answers and their reviews. */
    public boolean removeQuestion(long id) {
        return update("DELETE FROM questions WHERE id = ?", id) > 0;
    }

    /**
     * Counts questions in a course (and optionally one chapter), letting the database
     * count instead of loading every question. chapterNumber null = whole course.
     */
    public int countQuestions(String courseCode, Integer chapterNumber, boolean unresolvedOnly) {
        String sql = "SELECT COUNT(*) FROM questions WHERE course_code = ?"
                + (chapterNumber != null ? " AND chapter_number = ?" : "")
                + (unresolvedOnly ? " AND accepted_answer_id IS NULL" : "");
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, courseCode);
                if (chapterNumber != null) {
                    ps.setInt(2, chapterNumber);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    /**
     * Marks an answer as accepted. The Question object checks the rules first
     * (not already resolved, answer belongs to it); then the database is updated.
     */
    public void resolveQuestion(Question question, long answerId) {
        question.markResolved(answerId); // throws if the rules are broken
        int changed = update("UPDATE questions SET accepted_answer_id = ? WHERE id = ? AND accepted_answer_id IS NULL",
                answerId, question.getId());
        if (changed == 0) {
            throw new IllegalStateException("This question is already resolved.");
        }
    }

    // =====================================================================
    // Answers and reviews
    // =====================================================================

    /** Saves a new answer; the database picks its id. Returns the saved copy. */
    public Answer addAnswer(long questionId, Answer draft) {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answers (question_id, text, author, created_at) VALUES (?, ?, ?, ?)",
                    new String[] {"id"})) {
                ps.setLong(1, questionId);
                ps.setString(2, draft.getText());
                ps.setString(3, draft.getAuthor());
                ps.setObject(4, draft.getCreatedAt());
                ps.executeUpdate();
                return new Answer(generatedId(ps), draft.getText(), draft.getAuthor(), draft.getCreatedAt());
            }
        });
    }

    /** Saves a new review. Returns false if this reviewer already reviewed the answer. */
    public boolean addReview(long answerId, Review review) {
        return insertIgnoringDuplicate(
                "INSERT INTO reviews (answer_id, reviewer, verdict, comment, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                answerId, review.getReviewer(), review.getVerdict().name(), review.getComment(),
                review.getCreatedAt(), review.getUpdatedAt());
    }

    /** Changes an existing review (the Review object validates the new content first). */
    public void updateReview(long answerId, Review review, Verdict verdict, String comment, LocalDateTime now) {
        review.update(verdict, comment, now); // throws if invalid
        update("UPDATE reviews SET verdict = ?, comment = ?, updated_at = ? WHERE answer_id = ? AND reviewer = ?",
                review.getVerdict().name(), review.getComment(), review.getUpdatedAt(),
                answerId, review.getReviewer());
    }

    // =====================================================================
    // Session (in memory - belongs to this running app only)
    // =====================================================================

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

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Loads questions, then ALL their answers, then ALL those answers' reviews:
     * three queries no matter how many questions there are, instead of one per question.
     */
    private List<Question> loadQuestions(String where, Object... params) {
        return database.query(c -> {
            Map<Long, Question> questions = new LinkedHashMap<>();
            Map<Long, Long> acceptedIds = new HashMap<>();
            try (PreparedStatement ps = prepare(c, "SELECT * FROM questions q" + where + " ORDER BY q.id", params);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question(rs.getLong("id"), rs.getString("course_code"),
                            rs.getInt("chapter_number"), rs.getString("title"), rs.getString("body"),
                            rs.getString("author"), rs.getObject("created_at", LocalDateTime.class));
                    questions.put(q.getId(), q);
                    long accepted = rs.getLong("accepted_answer_id");
                    if (!rs.wasNull()) {
                        acceptedIds.put(q.getId(), accepted);
                    }
                }
            }
            if (questions.isEmpty()) {
                return List.of();
            }

            Map<Long, Answer> answers = new HashMap<>();
            try (PreparedStatement ps = prepare(c,
                    "SELECT a.* FROM answers a JOIN questions q ON q.id = a.question_id" + where + " ORDER BY a.id",
                    params);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Answer a = new Answer(rs.getLong("id"), rs.getString("text"), rs.getString("author"),
                            rs.getObject("created_at", LocalDateTime.class));
                    answers.put(a.getId(), a);
                    questions.get(rs.getLong("question_id")).addAnswer(a);
                }
            }

            try (PreparedStatement ps = prepare(c,
                    "SELECT r.* FROM reviews r JOIN answers a ON a.id = r.answer_id "
                            + "JOIN questions q ON q.id = a.question_id" + where + " ORDER BY r.created_at",
                    params);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review(rs.getString("reviewer"), Verdict.valueOf(rs.getString("verdict")),
                            rs.getString("comment"), rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("updated_at", LocalDateTime.class));
                    answers.get(rs.getLong("answer_id")).addReview(r);
                }
            }

            acceptedIds.forEach((questionId, answerId) -> questions.get(questionId).markResolved(answerId));
            return List.copyOf(questions.values());
        });
    }

    private static EnumSet<Role> loadRoles(Connection c, String table, String keyColumn, String key)
            throws SQLException {
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT role FROM " + table + " WHERE " + keyColumn + " = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(Role.valueOf(rs.getString(1)));
                }
            }
        }
        return roles;
    }

    private static void saveRoles(Connection c, String table, String keyColumn, String key, Set<Role> roles)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO " + table + " (" + keyColumn + ", role) VALUES (?, ?)")) {
            for (Role role : roles) {
                ps.setString(1, key);
                ps.setString(2, role.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** INSERT that returns false instead of failing when the key already exists. */
    private boolean insertIgnoringDuplicate(String sql, Object... params) {
        try {
            update(sql, params);
            return true;
        } catch (DataAccessException e) {
            if (e.getCause() instanceof SQLException sqlError && Database.isDuplicateKey(sqlError)) {
                return false;
            }
            throw e;
        }
    }

    /** Runs an INSERT/UPDATE/DELETE and returns how many rows it changed. */
    private int update(String sql, Object... params) {
        return database.query(c -> {
            try (PreparedStatement ps = prepare(c, sql, params)) {
                return ps.executeUpdate();
            }
        });
    }

    /** Fills in the ? placeholders. Values are never pasted into the SQL text (prevents SQL injection). */
    private static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    private static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }
}
