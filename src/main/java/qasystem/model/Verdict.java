package qasystem.model;

/**
 * A reviewer's judgment of an answer.
 * An enum, not a 1-5 number: it can never hold an invalid value,
 * and "Partially correct" tells a student more than "3".
 */
public enum Verdict {
    CORRECT("Correct"),
    PARTIALLY_CORRECT("Partially correct"),
    INCORRECT("Incorrect");

    private final String displayName;

    Verdict(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
