package qasystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.model.Invitation;
import qasystem.model.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InvitationService: who can invite, and what a code looks like")
class InvitationServiceTest extends ServiceTestBase {

    @Test
    @DisplayName("Only an admin can create invitations")
    void onlyAdminCanInvite() {
        createStudent("sam");
        loginAs("sam", Role.STUDENT);

        assertThrows(IllegalStateException.class,
                () -> invitationService.createInvitation(Set.of(Role.STUDENT), 7));
    }

    @Test
    @DisplayName("An invitation must grant at least one role")
    void needsAtLeastOneRole() {
        loginAs(ADMIN, Role.ADMIN);

        assertThrows(IllegalArgumentException.class,
                () -> invitationService.createInvitation(Set.of(), 7));
    }

    @Test
    @DisplayName("Valid days must be between 1 and 30")
    void validDaysInRange() {
        loginAs(ADMIN, Role.ADMIN);

        assertThrows(IllegalArgumentException.class,
                () -> invitationService.createInvitation(Set.of(Role.STUDENT), 0));
        assertThrows(IllegalArgumentException.class,
                () -> invitationService.createInvitation(Set.of(Role.STUDENT), 31));
    }

    @Test
    @DisplayName("The deadline is exactly 'now + valid days'")
    void deadlineIsNowPlusDays() {
        loginAs(ADMIN, Role.ADMIN);

        Invitation invitation = invitationService.createInvitation(Set.of(Role.STUDENT), 7);

        assertEquals(LocalDateTime.now(clock).plusDays(7), invitation.getExpiresAt());
    }

    @Test
    @DisplayName("Codes are 8 easy-to-read characters and never repeat")
    void codesAreReadableAndUnique() {
        loginAs(ADMIN, Role.ADMIN);
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            String code = invitationService.createInvitation(Set.of(Role.STUDENT), 7).getCode();
            assertTrue(code.matches("[A-HJ-NP-Z2-9]{8}"), "unexpected code format: " + code);
            codes.add(code);
        }

        assertEquals(200, codes.size());
    }
}
