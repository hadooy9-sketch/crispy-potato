package qasystem.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Invitation: a one-time code that expires at a deadline.
 * Time is passed in as a parameter, so we can test "before" and "after" without waiting.
 */
class InvitationTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 1, 0, 0);

    private Invitation newInvitation() {
        return new Invitation("ABCD2345", Set.of(Role.STUDENT, Role.REVIEWER), DEADLINE);
    }

    // The constructor stores the code, roles and deadline, and starts unused.
    @Test
    void constructorStoresFields() {
        Invitation inv = newInvitation();
        assertEquals("ABCD2345", inv.getCode());
        assertEquals(Set.of(Role.STUDENT, Role.REVIEWER), inv.getRoles());
        assertEquals(DEADLINE, inv.getExpiresAt());
        assertFalse(inv.isUsed());
    }

    // A code is required (null or blank fails).
    @Test
    void constructorRejectsMissingCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation(null, Set.of(Role.STUDENT), DEADLINE));
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation(" ", Set.of(Role.STUDENT), DEADLINE));
    }

    // At least one role is required (null or empty set fails).
    @Test
    void constructorRejectsMissingRoles() {
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation("CODE", null, DEADLINE));
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation("CODE", Set.of(), DEADLINE));
    }

    // A deadline is required.
    @Test
    void constructorRejectsMissingDeadline() {
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation("CODE", Set.of(Role.STUDENT), null));
    }

    // "Defensive copy": changing the set we passed in later must NOT change the invitation.
    @Test
    void constructorCopiesRoles() {
        Set<Role> roles = new HashSet<>(Set.of(Role.STUDENT));
        Invitation inv = new Invitation("CODE", roles, DEADLINE);
        roles.add(Role.ADMIN);
        assertEquals(EnumSet.of(Role.STUDENT), inv.getRoles());
    }

    // getRoles is read-only, so nobody can add ADMIN to an existing invitation.
    @Test
    void getRolesIsReadOnly() {
        Set<Role> roles = newInvitation().getRoles();
        assertThrows(UnsupportedOperationException.class, () -> roles.add(Role.ADMIN));
    }

    // Before the deadline the invitation is not expired.
    @Test
    void isExpiredFalseBeforeDeadline() {
        assertFalse(newInvitation().isExpired(DEADLINE.minusSeconds(1)));
    }

    // Boundary: exactly AT the deadline it is still valid (isAfter is strict).
    @Test
    void isExpiredFalseExactlyAtDeadline() {
        assertFalse(newInvitation().isExpired(DEADLINE));
    }

    // After the deadline it is expired.
    @Test
    void isExpiredTrueAfterDeadline() {
        assertTrue(newInvitation().isExpired(DEADLINE.plusSeconds(1)));
    }

    // Unused and not expired -> usable.
    @Test
    void isUsableWhenFreshAndInTime() {
        assertTrue(newInvitation().isUsable(DEADLINE.minusDays(1)));
    }

    // Expired -> not usable, even though it was never used.
    @Test
    void isNotUsableWhenExpired() {
        assertFalse(newInvitation().isUsable(DEADLINE.plusDays(1)));
    }

    // Used -> not usable, even though there is still time left.
    @Test
    void isNotUsableAfterMarkUsed() {
        Invitation inv = newInvitation();
        inv.markUsed();
        assertTrue(inv.isUsed());
        assertFalse(inv.isUsable(DEADLINE.minusDays(1)));
    }

    // Using a one-time code twice is a programming error, so it throws.
    @Test
    void markUsedTwiceThrows() {
        Invitation inv = newInvitation();
        inv.markUsed();
        assertThrows(IllegalStateException.class, inv::markUsed);
    }
}
