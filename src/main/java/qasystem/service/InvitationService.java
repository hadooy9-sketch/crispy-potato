package qasystem.service;

import qasystem.model.Invitation;
import qasystem.model.QASystem;
import qasystem.model.Role;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Owns invitation behavior: only admins create them,
 * and every code is random, unique, and time-limited.
 */
public class InvitationService {

    // No 0/O or 1/I, so codes are easy to read and type.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_VALID_DAYS = 30;

    private final QASystem qaSystem;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public InvitationService(QASystem qaSystem, Clock clock) {
        this.qaSystem = qaSystem;
        this.clock = clock;
    }

    /**
     * Creates and stores a new invitation.
     * @throws IllegalStateException if the current user is not acting as admin
     * @throws IllegalArgumentException if no roles are chosen or the days are out of range
     */
    public Invitation createInvitation(Set<Role> roles, int validDays) {
        Access.requireRole(qaSystem, Role.ADMIN, "create invitations");
        if (validDays < 1 || validDays > MAX_VALID_DAYS) {
            throw new IllegalArgumentException(
                "An invitation must be valid for 1 to " + MAX_VALID_DAYS + " days.");
        }

        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(validDays);
        Invitation invitation = new Invitation(generateUniqueCode(), roles, expiresAt);
        qaSystem.addInvitation(invitation);
        return invitation;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (qaSystem.findInvitation(code) != null); // extremely rare
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
