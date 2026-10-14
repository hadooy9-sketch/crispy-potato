package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import qasystem.model.QASystem;
import qasystem.model.Role;
import qasystem.model.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Access.requireRole, the shared "are you allowed?" check.
 * Access is package-private, which is why this test lives in the same package (qasystem.service).
 */
class AccessTest {

    private QASystem qa;

    @BeforeEach
    void setUp() {
        qa = new QASystem();
        qa.setCurrentUser(new User("alice", "pw"));
    }

    // When the current role matches, nothing happens (no exception).
    @Test
    void passesWhenRoleMatches() {
        qa.setCurrentRole(Role.ADMIN);
        assertDoesNotThrow(() -> Access.requireRole(qa, Role.ADMIN, "do admin things"));
    }

    // When the role is different, it throws with a helpful message.
    @Test
    void throwsWhenRoleDiffers() {
        qa.setCurrentRole(Role.STUDENT);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Access.requireRole(qa, Role.ADMIN, "delete courses"));
        assertEquals("You must be acting as Admin to delete courses.", e.getMessage());
    }

    // When no role is selected yet (null), it also throws.
    @Test
    void throwsWhenNoRoleSelected() {
        assertThrows(IllegalStateException.class,
                () -> Access.requireRole(qa, Role.STUDENT, "ask"));
    }

    // Having a role on your account is not enough: you must be ACTING in that role right now.
    @Test
    void checksActingRoleNotAccountRoles() {
        User u = new User("multi", "pw");
        u.addRole(Role.ADMIN);
        u.addRole(Role.STUDENT);
        qa.setCurrentUser(u);
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class,
                () -> Access.requireRole(qa, Role.ADMIN, "do admin things"));
    }
}
