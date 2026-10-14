package qasystem.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Keywords.extract, which turns text into a set of
 * search keywords (lower-case, no stop words, crude plural removal).
 */
class KeywordsTest {

    // The example from the class comment.
    @Test
    void extractsKeywordsFromQuestion() {
        assertEquals(Set.of("find", "derivative"),
                Keywords.extract("How do I find the derivatives?"));
    }

    // null input returns an empty set instead of crashing.
    @Test
    void nullGivesEmptySet() {
        assertTrue(Keywords.extract(null).isEmpty());
    }

    // Empty text and text that is only punctuation give no keywords.
    @Test
    void emptyOrPunctuationGivesEmptySet() {
        assertTrue(Keywords.extract("").isEmpty());
        assertTrue(Keywords.extract("?!... ,,").isEmpty());
    }

    // Text made only of stop words ("the", "is", ...) has no keywords.
    @Test
    void stopWordsAreRemoved() {
        assertTrue(Keywords.extract("What is the and of it").isEmpty());
    }

    // Single-character words are too short to be meaningful.
    @Test
    void singleCharacterWordsAreDropped() {
        assertEquals(Set.of("xy"), Keywords.extract("x y xy"));
    }

    // Upper and lower case are treated the same.
    @Test
    void extractionIsCaseInsensitive() {
        assertEquals(Set.of("matrix"), Keywords.extract("MATRIX Matrix matrix"));
    }

    // Numbers are kept as keywords (e.g. "chapter 3" -> the number matters).
    @Test
    void digitsAreKept() {
        assertEquals(Set.of("x2", "42"), Keywords.extract("x2 42"));
    }

    // Plural "s" is removed, but words ending in "ss" (like "class") are left alone,
    // and short words (3 letters or fewer) are never changed.
    @Test
    void pluralHandling() {
        assertEquals(Set.of("integral"), Keywords.extract("integrals"));
        assertEquals(Set.of("class"), Keywords.extract("class"));
        assertEquals(Set.of("bus"), Keywords.extract("bus"));
    }

    // The singular and plural forms collapse to one keyword.
    @Test
    void singularAndPluralCollapse() {
        assertEquals(Set.of("vector"), Keywords.extract("vector vectors"));
    }

    // Each call returns a NEW mutable set, so a caller changing it can't affect the next call.
    @Test
    void returnsNewMutableSetEachTime() {
        Set<String> first = Keywords.extract("graph");
        first.add("extra");
        assertEquals(Set.of("graph"), Keywords.extract("graph"));
    }
}
