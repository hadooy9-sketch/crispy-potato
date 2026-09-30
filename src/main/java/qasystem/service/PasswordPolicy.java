package qasystem.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The password rules - ONE source of truth.
 * UserService enforces them; the sign-up screens draw their checklist from the same list,
 * so the checklist and the real check can never disagree.
 */
public final class PasswordPolicy {

    /** One rule: a description to show, and a check to run. */
    public static final class Rule {
        private final String description;
        private final Predicate<String> check;

        private Rule(String description, Predicate<String> check) {
            this.description = description;
            this.check = check;
        }

        public String getDescription() {
            return description;
        }

        public boolean isMetBy(String password) {
            return check.test(password == null ? "" : password);
        }
    }

    private static final List<Rule> RULES = List.of(
        new Rule("At least 8 characters",
                 p -> p.length() >= 8),
        new Rule("One uppercase letter (A-Z)",
                 p -> p.chars().anyMatch(Character::isUpperCase)),
        new Rule("One lowercase letter (a-z)",
                 p -> p.chars().anyMatch(Character::isLowerCase)),
        new Rule("One number (0-9)",
                 p -> p.chars().anyMatch(Character::isDigit)),
        new Rule("One special character (! @ # $ % ...)",
                 p -> p.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)))
    );

    private PasswordPolicy() {
        // utility class - never instantiated
    }

    public static List<Rule> rules() {
        return RULES;
    }

    public static List<Rule> unmetRules(String password) {
        List<Rule> unmet = new ArrayList<>();
        for (Rule rule : RULES) {
            if (!rule.isMetBy(password)) {
                unmet.add(rule);
            }
        }
        return unmet;
    }

    public static boolean isValid(String password) {
        return unmetRules(password).isEmpty();
    }
}
