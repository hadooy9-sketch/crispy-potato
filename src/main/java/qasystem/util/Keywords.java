package qasystem.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Turns text into a set of meaningful keywords, for "related question" search.
 * "How do I find the derivatives?" -> {find, derivative}
 *
 * Deliberately simple: no synonyms, no typo tolerance, crude plural handling.
 */
public final class Keywords {

    private static final int MIN_WORD_LENGTH = 2;

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "but", "by", "can", "do", "does",
        "for", "from", "get", "have", "how", "i", "if", "in", "into", "is", "it", "its",
        "me", "my", "no", "not", "of", "on", "or", "so", "that", "the", "this", "to",
        "was", "we", "what", "when", "where", "which", "who", "why", "will", "with",
        "you", "your", "there", "their", "should", "would", "could", "about", "any"
    );

    private Keywords() {
        // utility class - never instantiated
    }

    /** Returns a NEW mutable set each time, so callers may modify it. */
    public static Set<String> extract(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null) {
            return keywords;
        }
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() < MIN_WORD_LENGTH || STOP_WORDS.contains(word)) {
                continue;
            }
            keywords.add(singular(word));
        }
        return keywords;
    }

    /** Crude plural removal: "derivatives" -> "derivative", but "class" stays "class". */
    private static String singular(String word) {
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
