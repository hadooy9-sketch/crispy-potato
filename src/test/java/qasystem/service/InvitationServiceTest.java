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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvitationService: admins create random, unique, time-limited codes.
 * A fixed Clock makes "now" predictable, so we can check the exact expiry time.
 */
class InvitationServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(START, ZoneOffset.UTC);

    private QASystem qa;
    private InvitationService service;

    @BeforeEach
    void setUp() {
        qa = new QASystem();
        service = new InvitationService(qa, Clock.fixed(START, ZoneOffset.UTC));
        qa.setCurrentUser(new User("admin", "pw"));
        qa.setCurrentRole(Role.ADMIN);
    }

    // A new invitation gets the requested roles, the right deadline, and is stored in QASystem.
    @Test
    void createInvitationStoresInvitation() {
        Invitation inv = service.createInvitation(Set.of(Role.REVIEWER), 7);
        assertEquals(Set.of(Role.REVIEWER), inv.getRoles());
        assertEquals(NOW.plusDays(7), inv.getExpiresAt());
        assertFalse(inv.isUsed());
        assertSame(inv, qa.findInvitation(inv.getCode()));
    }

    // Codes are 8 characters long and only use the easy-to-read alphabet (no 0, O, 1, I).
    @Test
    void codeHasExpectedFormat() {
        String code = service.createInvitation(Set.of(Role.STUDENT), 1).getCode();
        assertEquals(8, code.length());
        assertTrue(code.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"), code);
    }

    // Many invitations in a row all get different codes.
    @Test
    void codesAreUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(service.createInvitation(Set.of(Role.STUDENT), 1).getCode());
        }
        assertEquals(200, codes.size());
    }

    // Boundaries: 1 day and 30 days are both allowed.
    @Test
    void acceptsValidDaysAtBoundaries() {
        assertEquals(NOW.plusDays(1), service.createInvitation(Set.of(Role.STUDENT), 1).getExpiresAt());
        assertEquals(NOW.plusDays(30), service.createInvitation(Set.of(Role.STUDENT), 30).getExpiresAt());
    }

    // Boundaries: 0 days and 31 days (and negatives) are rejected.
    @Test
    void rejectsValidDaysOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> service.createInvitation(Set.of(Role.STUDENT), 0));
        assertThrows(IllegalArgumentException.class, () -> service.createInvitation(Set.of(Role.STUDENT), 31));
        assertThrows(IllegalArgumentException.class, () -> service.createInvitation(Set.of(Role.STUDENT), -1));
    }

    // An invitation must grant at least one role.
    @Test
    void rejectsEmptyOrNullRoles() {
        assertThrows(IllegalArgumentException.class, () -> service.createInvitation(Set.of(), 7));
        assertThrows(IllegalArgumentException.class, () -> service.createInvitation(null, 7));
    }

    // Only admins can create invitations.
    @Test
    void requiresAdmin() {
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.createInvitation(Set.of(Role.STUDENT), 7));
    }
}
