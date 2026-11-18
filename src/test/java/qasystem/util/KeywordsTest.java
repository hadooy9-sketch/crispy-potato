package qasystem.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Keywords: turning text into search keywords")
class KeywordsTest {

    @Test
    @DisplayName("Lower-cases, splits on punctuation, and drops stop words")
    void dropsStopWords() {
        assertEquals(Set.of("find", "derivative"), Keywords.extract("How do I find the Derivative?"));
    }

    @Test
    @DisplayName("Removes a plural 's' but leaves words ending in 'ss' alone")
    void pluralHandling() {
        assertEquals(Set.of("derivative"), Keywords.extract("derivatives"));
        assertEquals(Set.of("class"), Keywords.extract("class"));
    }

    @Test
    @DisplayName("Null or empty text gives no keywords")
    void emptyInput() {
        assertTrue(Keywords.extract(null).isEmpty());
        assertTrue(Keywords.extract("   ").isEmpty());
    }

    @Test
    @DisplayName("Each call returns a new set the caller may change")
    void returnsFreshSet() {
        Set<String> first = Keywords.extract("limits");
        first.clear();

        assertEquals(Set.of("limit"), Keywords.extract("limits"));
    }
}
