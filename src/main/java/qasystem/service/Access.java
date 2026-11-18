package qasystem.service;

import qasystem.model.QASystem;
import qasystem.model.Role;

/**
 * Shared authorization check for services (DRY: the rule lives in one place).
 * Package-private: only classes in qasystem.service can use it.
 */
final class Access {

    private Access() {
        // utility class - never instantiated
    }

    /** @throws IllegalStateException if the current user is not acting in this role */
    static void requireRole(QASystem qaSystem, Role role, String action) {
        if (qaSystem.getCurrentRole() != role) {
            throw new IllegalStateException(
                "You must be acting as " + role.displayName() + " to " + action + ".");
        }
    }
}
