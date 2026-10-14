package qasystem.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Chapter: a numbered, titled part of a course.
 */
class ChapterTest {

    // A valid chapter keeps its number and title.
    @Test
    void constructorStoresNumberAndTitle() {
        Chapter chapter = new Chapter(3, "Derivatives");
        assertEquals(3, chapter.getNumber());
        assertEquals("Derivatives", chapter.getTitle());
    }

    // The title is trimmed so stray spaces do not show up on screen.
    @Test
    void constructorTrimsTitle() {
        assertEquals("Limits", new Chapter(1, "  Limits  ").getTitle());
    }

    // Boundary: 1 is the smallest allowed chapter number.
    @Test
    void constructorAcceptsChapterOne() {
        assertEquals(1, new Chapter(1, "Intro").getNumber());
    }

    // Boundary: 0 and negative numbers are not valid chapter numbers.
    @Test
    void constructorRejectsZeroAndNegativeNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new Chapter(0, "Intro"));
        assertThrows(IllegalArgumentException.class, () -> new Chapter(-5, "Intro"));
    }

    // A chapter must have a title: null is rejected.
    @Test
    void constructorRejectsNullTitle() {
        assertThrows(IllegalArgumentException.class, () -> new Chapter(1, null));
    }

    // Empty or whitespace-only titles are rejected too.
    @Test
    void constructorRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> new Chapter(1, ""));
        assertThrows(IllegalArgumentException.class, () -> new Chapter(1, "   "));
    }
}
