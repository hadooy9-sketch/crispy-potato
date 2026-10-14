package qasystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Course: course code normalization and chapter management.
 */
class CourseTest {

    private Course course;

    // Fresh course before every test so chapters added in one test don't leak into another.
    @BeforeEach
    void setUp() {
        course = new Course("MAT 117", "Calculus I");
    }

    // ---------- normalizeCode ----------

    // The documented examples: all of these mean the same course.
    @Test
    void normalizeCodeHandlesDifferentFormats() {
        assertEquals("MAT 117", Course.normalizeCode("mat117"));
        assertEquals("MAT 117", Course.normalizeCode(" Mat  117 "));
        assertEquals("MAT 117", Course.normalizeCode("MAT 117"));
    }

    // null should not crash; it becomes an empty string.
    @Test
    void normalizeCodeTurnsNullIntoEmptyString() {
        assertEquals("", Course.normalizeCode(null));
    }

    // Whitespace-only input becomes empty.
    @Test
    void normalizeCodeTurnsBlankIntoEmptyString() {
        assertEquals("", Course.normalizeCode("   "));
    }

    // A code with no digits is only upper-cased and trimmed.
    @Test
    void normalizeCodeWithoutDigitsJustUppercases() {
        assertEquals("ENGLISH", Course.normalizeCode("  english "));
    }

    // ---------- constructor ----------

    // The constructor normalizes the code and trims the title.
    @Test
    void constructorNormalizesCodeAndTrimsTitle() {
        Course c = new Course("cse360", "  Software Engineering  ");
        assertEquals("CSE 360", c.getCode());
        assertEquals("Software Engineering", c.getTitle());
    }

    // A code is required: null or blank must fail.
    @Test
    void constructorRejectsMissingCode() {
        assertThrows(IllegalArgumentException.class, () -> new Course(null, "Title"));
        assertThrows(IllegalArgumentException.class, () -> new Course("   ", "Title"));
    }

    // Boundary: 15 characters (after normalizing) is the max and is allowed.
    @Test
    void constructorAcceptsCodeAtMaxLength() {
        String code = "ABCDEFGHIJKLMNO"; // 15 letters
        assertEquals(code, new Course(code, "Title").getCode());
    }

    // Boundary: 16 characters is one too many.
    @Test
    void constructorRejectsCodeOverMaxLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new Course("ABCDEFGHIJKLMNOP", "Title"));
    }

    // A title is required: null or blank must fail.
    @Test
    void constructorRejectsMissingTitle() {
        assertThrows(IllegalArgumentException.class, () -> new Course("MAT 117", null));
        assertThrows(IllegalArgumentException.class, () -> new Course("MAT 117", "  "));
    }

    // ---------- chapters ----------

    // A new course starts with no chapters.
    @Test
    void newCourseHasNoChapters() {
        assertTrue(course.getChapters().isEmpty());
    }

    // Adding a chapter succeeds and it can be found by number.
    @Test
    void addChapterThenFindIt() {
        Chapter ch = new Chapter(1, "Limits");
        assertTrue(course.addChapter(ch));
        assertSame(ch, course.findChapter(1));
    }

    // findChapter returns null for a number that was never added.
    @Test
    void findChapterReturnsNullWhenMissing() {
        assertNull(course.findChapter(42));
    }

    // Two chapters with the same number are not allowed; the original is kept.
    @Test
    void addChapterRejectsDuplicateNumber() {
        Chapter original = new Chapter(1, "Limits");
        course.addChapter(original);
        assertFalse(course.addChapter(new Chapter(1, "Other")));
        assertSame(original, course.findChapter(1));
    }

    // Chapters come back sorted by number, even if added out of order (TreeMap).
    @Test
    void getChaptersIsSortedByNumber() {
        Chapter c3 = new Chapter(3, "C");
        Chapter c1 = new Chapter(1, "A");
        Chapter c2 = new Chapter(2, "B");
        course.addChapter(c3);
        course.addChapter(c1);
        course.addChapter(c2);
        assertEquals(List.of(c1, c2, c3), course.getChapters());
    }

    // The list returned is read-only, so callers cannot sneak chapters in.
    @Test
    void getChaptersIsReadOnly() {
        List<Chapter> chapters = course.getChapters();
        assertThrows(UnsupportedOperationException.class, () -> chapters.add(new Chapter(1, "X")));
    }

    // Removing an existing chapter returns true and it is gone afterwards.
    @Test
    void removeChapterRemovesExisting() {
        course.addChapter(new Chapter(1, "A"));
        assertTrue(course.removeChapter(1));
        assertNull(course.findChapter(1));
    }

    // Removing a chapter that does not exist returns false.
    @Test
    void removeChapterReturnsFalseWhenMissing() {
        assertFalse(course.removeChapter(5));
    }

    // With no chapters, the suggested next chapter is 1.
    @Test
    void nextChapterNumberIsOneWhenEmpty() {
        assertEquals(1, course.nextChapterNumber());
    }

    // The suggestion is one after the HIGHEST number, not one after the count.
    @Test
    void nextChapterNumberIsOneAfterHighest() {
        course.addChapter(new Chapter(1, "A"));
        course.addChapter(new Chapter(5, "E"));
        assertEquals(6, course.nextChapterNumber());
    }
}
