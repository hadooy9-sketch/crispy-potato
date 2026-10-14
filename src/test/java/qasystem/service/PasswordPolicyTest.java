package qasystem.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordPolicy: the single list of password rules.
 * Each test breaks exactly ONE rule, so if it fails we know which rule is wrong.
 */
class PasswordPolicyTest {

    private static List<String> unmetDescriptions(String password) {
        return PasswordPolicy.unmetRules(password).stream()
                .map(PasswordPolicy.Rule::getDescription)
                .toList();
    }

    // There are five rules, in a fixed order (the sign-up checklist shows them in this order).
    @Test
    void rulesAreListedInOrder() {
        List<String> descriptions = PasswordPolicy.rules().stream()
                .map(PasswordPolicy.Rule::getDescription)
                .toList();
        assertEquals(List.of(
                "At least 8 characters",
                "One uppercase letter (A-Z)",
                "One lowercase letter (a-z)",
                "One number (0-9)",
                "One special character (! @ # $ % ...)"), descriptions);
    }

    // The rule list cannot be changed by callers.
    @Test
    void rulesListIsReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> PasswordPolicy.rules().clear());
    }

    // A password that meets every rule is valid and has no unmet rules.
    @Test
    void strongPasswordIsValid() {
        assertTrue(PasswordPolicy.isValid("Abcdef1!"));
        assertTrue(PasswordPolicy.unmetRules("Abcdef1!").isEmpty());
    }

    // Boundary: 7 characters fails the length rule only.
    @Test
    void sevenCharactersIsTooShort() {
        assertEquals(List.of("At least 8 characters"), unmetDescriptions("Abcde1!"));
        assertFalse(PasswordPolicy.isValid("Abcde1!"));
    }

    // Missing an uppercase letter.
    @Test
    void missingUppercase() {
        assertEquals(List.of("One uppercase letter (A-Z)"), unmetDescriptions("abcdef1!"));
    }

    // Missing a lowercase letter.
    @Test
    void missingLowercase() {
        assertEquals(List.of("One lowercase letter (a-z)"), unmetDescriptions("ABCDEF1!"));
    }

    // Missing a digit.
    @Test
    void missingDigit() {
        assertEquals(List.of("One number (0-9)"), unmetDescriptions("Abcdefg!"));
    }

    // Missing a special character.
    @Test
    void missingSpecialCharacter() {
        assertEquals(List.of("One special character (! @ # $ % ...)"), unmetDescriptions("Abcdefg1"));
    }

    // A space does NOT count as a special character.
    @Test
    void spaceIsNotASpecialCharacter() {
        assertFalse(PasswordPolicy.isValid("Abcdef1 "));
    }

    // null is treated like an empty string: it fails every rule but doesn't crash.
    @Test
    void nullPasswordFailsAllRulesWithoutCrashing() {
        assertEquals(5, PasswordPolicy.unmetRules(null).size());
        assertFalse(PasswordPolicy.isValid(null));
    }

    // The empty string fails every rule.
    @Test
    void emptyPasswordFailsAllRules() {
        assertEquals(5, PasswordPolicy.unmetRules("").size());
    }

    // Rule.isMetBy can be used one rule at a time (this is what the live checklist does).
    @Test
    void individualRuleIsMetBy() {
        PasswordPolicy.Rule lengthRule = PasswordPolicy.rules().get(0);
        assertTrue(lengthRule.isMetBy("12345678"));
        assertFalse(lengthRule.isMetBy("1234567"));
        assertFalse(lengthRule.isMetBy(null));
    }
}
