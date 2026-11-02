package qasystem.model;

import java.util.List;
import java.util.TreeMap;

/**
 * A course (e.g. "MAT 117 - Calculus I") and its chapters.
 * Chapters are kept in a TreeMap: always sorted by number, no duplicates.
 */
public class Course {

    private static final int MAX_CODE_LENGTH = 15;   // matches courses.code VARCHAR(15)
    private static final int MAX_TITLE_LENGTH = 200; // matches courses.title VARCHAR(200)

    private final String code;
    private final String title;
    private final TreeMap<Integer, Chapter> chapters;

    public Course(String code, String title) {
        String normalized = normalizeCode(code);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                "Course code must be at most " + MAX_CODE_LENGTH + " characters.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title is required.");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "Course title must be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        this.code = normalized;
        this.title = title.trim();
        this.chapters = new TreeMap<>();
    }

    /**
     * "mat117", " Mat  117 " and "MAT 117" all become "MAT 117",
     * so the same course can never be added twice.
     */
    public static String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().replaceAll("\\s+", " ").toUpperCase();
        return cleaned.replaceAll("^([A-Z]+)\\s*(\\d)", "$1 $2");
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    // ---------- Chapters ----------

    /** Read-only snapshot, sorted by chapter number. */
    public List<Chapter> getChapters() {
        return List.copyOf(chapters.values());
    }

    public Chapter findChapter(int number) {
        return chapters.get(number);
    }

    /**
     * Returns false if a chapter with that number already exists.
     * Package-private: only QASystem builds courses (adding a chapter for real goes through QASystem).
     */
    boolean addChapter(Chapter chapter) {
        return chapters.putIfAbsent(chapter.getNumber(), chapter) == null;
    }

    /** Suggestion for the next chapter: one after the highest, or 1. */
    public int nextChapterNumber() {
        return chapters.isEmpty() ? 1 : chapters.lastKey() + 1;
    }
}
