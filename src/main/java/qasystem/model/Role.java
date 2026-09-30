package qasystem.model;

/**
 * Represents every role a user can have
 * in the Question & Answer System.
 */
public enum Role {
    ADMIN,
    STUDENT,
    REVIEWER;

    /** "ADMIN" -> "Admin", for showing on screen. */
    public String displayName() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
