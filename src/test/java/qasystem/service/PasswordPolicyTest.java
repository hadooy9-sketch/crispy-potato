package qasystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordPolicy: the rules shown on the sign-up screens")
class PasswordPolicyTest {

    @Test
    @DisplayName("A strong password meets all five rules")
    void strongPassword() {
        assertTrue(PasswordPolicy.isValid("Calc#2026"));
        assertEquals(0, PasswordPolicy.unmetRules("Calc#2026").size());
    }

    @Test
    @DisplayName("An empty password fails all five rules")
    void emptyPassword() {
        assertFalse(PasswordPolicy.isValid(""));
        assertEquals(5, PasswordPolicy.unmetRules("").size());
        assertEquals(5, PasswordPolicy.unmetRules(null).size());
    }

    @Test
    @DisplayName("Exactly the missing rule is reported")
    void reportsMissingRule() {
        assertEquals("One special character (! @ # $ % ...)",
                PasswordPolicy.unmetRules("Calc2026").get(0).getDescription());
    }

    @Test
    @DisplayName("A space does not count as a special character")
    void spaceIsNotSpecial() {
        assertFalse(PasswordPolicy.isValid("Calc 2026"));
    }
}
