package qasystem.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Role enum.
 */
class RoleTest {

    // displayName turns "ADMIN" into "Admin" for showing on screen.
    @Test
    void displayNameCapitalizesFirstLetterOnly() {
        assertEquals("Admin", Role.ADMIN.displayName());
        assertEquals("Student", Role.STUDENT.displayName());
        assertEquals("Reviewer", Role.REVIEWER.displayName());
    }

    // There are exactly three roles; this catches someone adding one without updating the app.
    @Test
    void thereAreExactlyThreeRoles() {
        assertEquals(3, Role.values().length);
    }

    // valueOf converts the exact name back to the enum constant.
    @Test
    void valueOfFindsRoleByName() {
        assertEquals(Role.REVIEWER, Role.valueOf("REVIEWER"));
    }
}
