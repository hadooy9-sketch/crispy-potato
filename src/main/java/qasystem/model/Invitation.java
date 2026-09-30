package qasystem.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A one-time code that grants roles to whoever registers with it,
 * valid until a deadline.
 */
public class Invitation {

    private final String code;
    private final EnumSet<Role> roles;
    private final LocalDateTime expiresAt;
    private boolean used;

    public Invitation(String code, Set<Role> roles, LocalDateTime expiresAt) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Invitation code is required.");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Select at least one role for the invitation.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Invitation deadline is required.");
        }
        this.code = code;
        this.roles = EnumSet.copyOf(roles); // defensive copy
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public String getCode() {
        return code;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    /** "now" is passed in so tests can control time. */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsable(LocalDateTime now) {
        return !used && !isExpired(now);
    }

    /** One-time: using a code twice is a bug, so fail loudly. */
    public void markUsed() {
        if (used) {
            throw new IllegalStateException("Invitation " + code + " was already used.");
        }
        used = true;
    }
}
