package qasystem.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Verdict enum.
 */
class VerdictTest {

    // Each verdict has a human-friendly name for the screen.
    @Test
    void displayNameIsHumanReadable() {
        assertEquals("Correct", Verdict.CORRECT.displayName());
        assertEquals("Partially correct", Verdict.PARTIALLY_CORRECT.displayName());
        assertEquals("Incorrect", Verdict.INCORRECT.displayName());
    }

    // The order of constants matters for drop-down menus: best verdict first.
    @Test
    void verdictsAreInExpectedOrder() {
        assertArrayEquals(
                new Verdict[] {Verdict.CORRECT, Verdict.PARTIALLY_CORRECT, Verdict.INCORRECT},
                Verdict.values());
    }
}
