package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import qasystem.model.Invitation;
import qasystem.model.QASystem;
import qasystem.model.Role;
import qasystem.model.User;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService: first-user setup, registration, login, roles and logout.
 */
class UserServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(START, ZoneOffset.UTC);
    private static final String GOOD_PASSWORD = "Passw0rd!";

    private QASystem qa;
    private UserService service;

    // A fixed clock makes "now" always the same, so invitation expiry is predictable.
    @BeforeEach
    void setUp() {
        qa = new QASystem();
        service = new UserService(qa, Clock.fixed(START, ZoneOffset.UTC));
    }

    /** Most tests need the admin to exist first, because registerUser requires it. */
    private void createAdmin() {
        service.registerFirstUser("admin", GOOD_PASSWORD);
    }

    private Invitation addInvitation(String code, Set<Role> roles, LocalDateTime expiresAt) {
        Invitation inv = new Invitation(code, roles, expiresAt);
        qa.addInvitation(inv);
        return inv;
    }

    // ---------- first user ----------

    // An empty system needs a first user; after creating one it doesn't.
    @Test
    void needsFirstUserUntilOneExists() {
        assertTrue(service.needsFirstUser());
        createAdmin();
        assertFalse(service.needsFirstUser());
    }

    // The first user automatically becomes an ADMIN (and only an admin).
    @Test
    void registerFirstUserMakesAdmin() {
        createAdmin();
        User admin = qa.findUser("admin");
        assertNotNull(admin);
        assertEquals(Set.of(Role.ADMIN), admin.getRoles());
        assertTrue(admin.checkPassword(GOOD_PASSWORD));
    }

    // You can only create the first user once.
    @Test
    void registerFirstUserTwiceFails() {
        createAdmin();
        assertThrows(IllegalStateException.class,
                () -> service.registerFirstUser("admin2", GOOD_PASSWORD));
    }

    // The first user must also pass username and password validation.
    @Test
    void registerFirstUserValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> service.registerFirstUser("ab", GOOD_PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> service.registerFirstUser("admin", "weak"));
        assertTrue(service.needsFirstUser());
    }

    // ---------- registerUser: validation ----------

    // Regular sign-up is blocked until the admin exists.
    @Test
    void registerUserBeforeAdminFails() {
        assertThrows(IllegalStateException.class,
                () -> service.registerUser("alice", GOOD_PASSWORD, null));
    }

    // Username is required.
    @Test
    void registerUserRejectsMissingUsername() {
        createAdmin();
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(null, GOOD_PASSWORD, null));
        assertThrows(IllegalArgumentException.class, () -> service.registerUser("   ", GOOD_PASSWORD, null));
    }

    // Boundary: 2 characters is too short, 3 is just enough.
    @Test
    void registerUserUsernameLengthBoundary() {
        createAdmin();
        assertThrows(IllegalArgumentException.class, () -> service.registerUser("ab", GOOD_PASSWORD, null));
        assertDoesNotThrow(() -> service.registerUser("abc", GOOD_PASSWORD, null));
    }

    // Usernames can't contain spaces.
    @Test
    void registerUserRejectsSpacesInUsername() {
        createAdmin();
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("john doe", GOOD_PASSWORD, null));
    }

    // Weak passwords are rejected, and the message names the first unmet rule.
    @Test
    void registerUserRejectsWeakPassword() {
        createAdmin();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("alice", "short", null));
        assertEquals("Password needs: at least 8 characters.", e.getMessage());
        assertNull(qa.findUser("alice"));
    }

    // Two users can't share a username.
    @Test
    void registerUserRejectsDuplicateUsername() {
        createAdmin();
        service.registerUser("alice", GOOD_PASSWORD, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("alice", GOOD_PASSWORD, null));
    }

    // ---------- registerUser: roles and invitations ----------

    // With no invite code (null or blank), the new user is a STUDENT.
    @Test
    void registerUserWithoutCodeBecomesStudent() {
        createAdmin();
        service.registerUser("alice", GOOD_PASSWORD, null);
        service.registerUser("bobby", GOOD_PASSWORD, "   ");
        assertEquals(Set.of(Role.STUDENT), qa.findUser("alice").getRoles());
        assertEquals(Set.of(Role.STUDENT), qa.findUser("bobby").getRoles());
    }

    // With a valid code, the user gets exactly the invitation's roles and the code is used up.
    @Test
    void registerUserWithCodeGetsInvitationRoles() {
        createAdmin();
        Invitation inv = addInvitation("ABCD2345", Set.of(Role.REVIEWER, Role.STUDENT), NOW.plusDays(1));
        service.registerUser("rita", GOOD_PASSWORD, "ABCD2345");
        assertEquals(Set.of(Role.REVIEWER, Role.STUDENT), qa.findUser("rita").getRoles());
        assertTrue(inv.isUsed());
    }

    // The code is trimmed and upper-cased, so typing it in lower case still works.
    @Test
    void registerUserNormalizesInviteCode() {
        createAdmin();
        addInvitation("ABCD2345", Set.of(Role.REVIEWER), NOW.plusDays(1));
        service.registerUser("rita", GOOD_PASSWORD, "  abcd2345 ");
        assertTrue(qa.findUser("rita").hasRole(Role.REVIEWER));
    }

    // A code that doesn't exist is rejected.
    @Test
    void registerUserRejectsUnknownCode() {
        createAdmin();
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("rita", GOOD_PASSWORD, "NOPE1234"));
        assertNull(qa.findUser("rita"));
    }

    // An expired code is rejected.
    @Test
    void registerUserRejectsExpiredCode() {
        createAdmin();
        addInvitation("OLD23456", Set.of(Role.REVIEWER), NOW.minusSeconds(1));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("rita", GOOD_PASSWORD, "OLD23456"));
    }

    // A code can only be used once.
    @Test
    void registerUserRejectsAlreadyUsedCode() {
        createAdmin();
        addInvitation("ONCE2345", Set.of(Role.REVIEWER), NOW.plusDays(1));
        service.registerUser("rita", GOOD_PASSWORD, "ONCE2345");
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("sam", GOOD_PASSWORD, "ONCE2345"));
    }

    // If sign-up fails for another reason (e.g. weak password), the code is NOT wasted.
    @Test
    void failedRegistrationDoesNotUseUpCode() {
        createAdmin();
        Invitation inv = addInvitation("KEEP2345", Set.of(Role.REVIEWER), NOW.plusDays(1));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("rita", "weak", "KEEP2345"));
        assertFalse(inv.isUsed());
        // also when the username is already taken
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("admin", GOOD_PASSWORD, "KEEP2345"));
        assertFalse(inv.isUsed());
    }

    // ---------- login ----------

    // Correct username and password logs in; a single-role user gets that role automatically.
    @Test
    void loginWithSingleRoleSelectsItAutomatically() {
        createAdmin();
        assertTrue(service.login("admin", GOOD_PASSWORD));
        assertEquals("admin", service.getCurrentUser().getUsername());
        assertEquals(Role.ADMIN, service.getCurrentRole());
        assertFalse(service.needsRoleSelection());
    }

    // A user with several roles must pick one after logging in.
    @Test
    void loginWithMultipleRolesNeedsSelection() {
        createAdmin();
        addInvitation("MULT2345", Set.of(Role.REVIEWER, Role.STUDENT), NOW.plusDays(1));
        service.registerUser("rita", GOOD_PASSWORD, "MULT2345");
        assertTrue(service.login("rita", GOOD_PASSWORD));
        assertNull(service.getCurrentRole());
        assertTrue(service.needsRoleSelection());
    }

    // Wrong password, unknown user, or nulls -> login fails and nobody is logged in.
    @Test
    void loginFailsWithBadCredentials() {
        createAdmin();
        assertFalse(service.login("admin", "wrong"));
        assertFalse(service.login("ghost", GOOD_PASSWORD));
        assertFalse(service.login(null, null));
        assertNull(service.getCurrentUser());
    }

    // ---------- needsRoleSelection / selectRole ----------

    // Nobody logged in -> no role selection needed.
    @Test
    void needsRoleSelectionFalseWhenLoggedOut() {
        assertFalse(service.needsRoleSelection());
    }

    // Selecting a role the user has works.
    @Test
    void selectRoleThatUserHas() {
        createAdmin();
        addInvitation("MULT2345", Set.of(Role.REVIEWER, Role.STUDENT), NOW.plusDays(1));
        service.registerUser("rita", GOOD_PASSWORD, "MULT2345");
        service.login("rita", GOOD_PASSWORD);
        service.selectRole(Role.REVIEWER);
        assertEquals(Role.REVIEWER, service.getCurrentRole());
        assertFalse(service.needsRoleSelection());
    }

    // Selecting a role the user does NOT have is rejected.
    @Test
    void selectRoleUserLacksFails() {
        createAdmin();
        service.login("admin", GOOD_PASSWORD);
        assertThrows(IllegalArgumentException.class, () -> service.selectRole(Role.STUDENT));
        assertEquals(Role.ADMIN, service.getCurrentRole());
    }

    // Selecting a role with nobody logged in is an error.
    @Test
    void selectRoleWhenLoggedOutFails() {
        assertThrows(IllegalStateException.class, () -> service.selectRole(Role.STUDENT));
    }

    // ---------- logout ----------

    // Logging out clears both the user and the role.
    @Test
    void logoutClearsSession() {
        createAdmin();
        service.login("admin", GOOD_PASSWORD);
        service.logout();
        assertNull(service.getCurrentUser());
        assertNull(service.getCurrentRole());
    }
}
