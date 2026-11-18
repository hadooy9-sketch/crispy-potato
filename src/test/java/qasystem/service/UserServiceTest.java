package qasystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.model.Role;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserService: first user, login, registration, invitations")
class UserServiceTest extends ServiceTestBase {

    // ---------- First user and login ----------

    @Test
    @DisplayName("The first user becomes admin and goes straight into the admin role")
    void firstUserBecomesAdmin() {
        assertFalse(userService.needsFirstUser());

        assertTrue(userService.login(ADMIN, PASSWORD));

        assertEquals(Role.ADMIN, userService.getCurrentRole());
        assertFalse(userService.needsRoleSelection());
    }

    @Test
    @DisplayName("A second 'first user' is rejected")
    void firstUserOnlyOnce() {
        assertThrows(IllegalStateException.class,
                () -> userService.registerFirstUser("someone", PASSWORD));
    }

    @Test
    @DisplayName("A wrong password fails and leaves nobody logged in")
    void wrongPasswordFails() {
        assertFalse(userService.login(ADMIN, "wrong-password"));
        assertNull(userService.getCurrentUser());
    }

    @Test
    @DisplayName("An unknown username fails exactly like a wrong password")
    void unknownUserFails() {
        assertFalse(userService.login("ghost", PASSWORD));
        assertNull(userService.getCurrentUser());
    }

    @Test
    @DisplayName("Logout clears both the user and the role")
    void logoutClearsSession() {
        loginAs(ADMIN, Role.ADMIN);

        userService.logout();

        assertNull(userService.getCurrentUser());
        assertNull(userService.getCurrentRole());
    }

    @Test
    @DisplayName("A new login never inherits the previous user's role")
    void roleIsNotInherited() {
        createUser("multi", Role.STUDENT, Role.REVIEWER);
        loginAs(ADMIN, Role.ADMIN);

        userService.logout();
        userService.login("multi", PASSWORD);

        assertNull(userService.getCurrentRole());
        assertTrue(userService.needsRoleSelection());
    }

    // ---------- Registration ----------

    @Test
    @DisplayName("Short usernames, usernames with spaces, and short passwords are rejected")
    void invalidAccountDetails() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("ab", PASSWORD, ""));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bo b", PASSWORD, ""));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "12345", ""));
    }

    @Test
    @DisplayName("A password missing any one rule is rejected")
    void passwordNeedsEveryRule() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "Sh#1", ""));        // too short
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "secret#123", ""));  // no uppercase
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "SECRET#123", ""));  // no lowercase
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "Secret#abc", ""));  // no number
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("bob", "Secret1234", ""));  // no special
        assertDoesNotThrow(() -> userService.registerUser("bob", "Secret#123", ""));
    }

    @Test
    @DisplayName("A taken username is rejected")
    void duplicateUsername() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(ADMIN, PASSWORD, ""));
    }

    @Test
    @DisplayName("Signing up without a code makes a student")
    void noCodeMakesStudent() {
        createStudent("sam");

        assertTrue(userService.login("sam", PASSWORD));

        assertEquals(Role.STUDENT, userService.getCurrentRole());
    }

    @Test
    @DisplayName("An invite code grants its roles; with two roles the user must choose")
    void inviteGrantsRoles() {
        createUser("rita", Role.STUDENT, Role.REVIEWER);

        userService.login("rita", PASSWORD);
        assertTrue(userService.needsRoleSelection());
        userService.selectRole(Role.REVIEWER);

        assertEquals(Role.REVIEWER, userService.getCurrentRole());
    }

    @Test
    @DisplayName("A user cannot select a role they do not have")
    void cannotSelectMissingRole() {
        createStudent("sam");
        userService.login("sam", PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> userService.selectRole(Role.ADMIN));
    }

    // ---------- Invite codes ----------

    @Test
    @DisplayName("An invite code works only once")
    void codeIsOneTime() {
        String code = inviteCode(7, Role.REVIEWER);

        userService.registerUser("first", PASSWORD, code);

        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("second", PASSWORD, code));
    }

    @Test
    @DisplayName("An invite code still works one minute before its deadline")
    void codeWorksBeforeDeadline() {
        String code = inviteCode(7, Role.REVIEWER);

        clock.advance(Duration.ofDays(7).minusMinutes(1));

        assertDoesNotThrow(() -> userService.registerUser("ontime", PASSWORD, code));
    }

    @Test
    @DisplayName("An invite code stops working after its deadline")
    void codeExpires() {
        String code = inviteCode(7, Role.REVIEWER);

        clock.advance(Duration.ofDays(7).plusMinutes(1));

        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("late", PASSWORD, code));
    }

    @Test
    @DisplayName("A failed sign-up does not use up the invite code")
    void failedSignupKeepsCode() {
        String code = inviteCode(7, Role.REVIEWER);

        // Username "admin" is taken, so this sign-up fails...
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(ADMIN, PASSWORD, code));

        // ...and the code must still work.
        assertDoesNotThrow(() -> userService.registerUser("newperson", PASSWORD, code));
    }

    @Test
    @DisplayName("Invite codes are not case-sensitive")
    void codeIgnoresCase() {
        String code = inviteCode(7, Role.REVIEWER);

        assertDoesNotThrow(() -> userService.registerUser("lower", PASSWORD, "  " + code.toLowerCase() + " "));
    }
}
