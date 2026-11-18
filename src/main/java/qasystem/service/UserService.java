package qasystem.service;

import qasystem.model.Invitation;
import qasystem.model.QASystem;
import qasystem.model.Role;
import qasystem.model.User;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns user behavior: first-user setup, registration, login,
 * role selection, logout. Does NOT own users - it asks QASystem.
 */
public class UserService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30; // matches users.username VARCHAR(30)

    private final QASystem qaSystem;
    private final Clock clock;

    public UserService(QASystem qaSystem, Clock clock) {
        this.qaSystem = qaSystem;
        this.clock = clock;
    }

    // ---------- First user ----------

    public boolean needsFirstUser() {
        return qaSystem.hasNoUsers();
    }

    /**
     * Creates the very first account and makes it an admin.
     * @throws IllegalStateException if any user already exists
     * @throws IllegalArgumentException if the username or password is invalid
     */
    public void registerFirstUser(String username, String password) {
        if (!qaSystem.hasNoUsers()) {
            throw new IllegalStateException("The administrator account already exists.");
        }
        validateUsername(username);
        validatePassword(password);

        User admin = new User(username, password); // hashes the password
        admin.addRole(Role.ADMIN);
        qaSystem.addUser(admin);
    }

    // ---------- Registration ----------

    /**
     * Creates a new account.
     * No invite code -> the user is a STUDENT.
     * With a code    -> the user gets the invitation's roles, and the code is used up.
     * @throws IllegalArgumentException if the code, username, or password is invalid
     */
    public void registerUser(String username, String password, String inviteCode) {
        if (qaSystem.hasNoUsers()) {
            throw new IllegalStateException("The administrator account must be set up first.");
        }

        // 1. Check the invite code (optional)
        Invitation invitation = null;
        if (inviteCode != null && !inviteCode.isBlank()) {
            invitation = qaSystem.findInvitation(inviteCode.trim().toUpperCase());
            if (invitation == null || !invitation.isUsable(LocalDateTime.now(clock))) {
                throw new IllegalArgumentException("Invite code is invalid, expired, or already used.");
            }
        }

        // 2. Validate the account details
        validateUsername(username);
        validatePassword(password);

        // 3. Decide the roles - every user ends up with at least one
        User user = new User(username, password); // hashes the password
        if (invitation == null) {
            user.addRole(Role.STUDENT);
        } else {
            for (Role role : invitation.getRoles()) {
                user.addRole(role);
            }
        }

        // 4 + 5. Save the user AND use up the code as ONE transaction:
        // if either step fails, neither is saved - a failed signup never wastes the code.
        Invitation code = invitation;
        LocalDateTime now = LocalDateTime.now(clock);
        qaSystem.inTransaction(() -> {
            if (!qaSystem.addUser(user)) {
                throw new IllegalArgumentException("That username is already taken.");
            }
            if (code != null && !qaSystem.claimInvitation(code.getCode(), now)) {
                throw new IllegalArgumentException("Invite code is invalid, expired, or already used.");
            }
        });
    }

    // ---------- Login / roles / logout ----------

    /**
     * Logs in if the username exists and the password matches.
     * A user with exactly one role gets that role automatically.
     */
    public boolean login(String username, String password) {
        User user = qaSystem.findUser(username);
        if (user == null || !user.checkPassword(password)) {
            return false;
        }
        qaSystem.setCurrentUser(user);

        if (user.getRoles().size() == 1) {
            Role onlyRole = user.getRoles().iterator().next();
            qaSystem.setCurrentRole(onlyRole);
        }
        return true;
    }

    /** True when someone is logged in but has not picked a role yet. */
    public boolean needsRoleSelection() {
        return qaSystem.getCurrentUser() != null && qaSystem.getCurrentRole() == null;
    }

    /** @throws IllegalArgumentException if the current user does not have this role */
    public void selectRole(Role role) {
        User user = qaSystem.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No user is logged in.");
        }
        if (!user.hasRole(role)) {
            throw new IllegalArgumentException("You do not have the " + role.displayName() + " role.");
        }
        qaSystem.setCurrentRole(role);
    }

    public void logout() {
        qaSystem.clearCurrentUser();
    }

    /**
     * Deletes the logged-in user's account and logs them out.
     * Questions and answers they posted are kept.
     * @throws IllegalStateException if nobody is logged in, or if this is the only admin
     *         (the system always needs at least one admin)
     */
    public void deleteCurrentAccount() {
        User user = qaSystem.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No user is logged in.");
        }
        if (user.hasRole(Role.ADMIN) && qaSystem.countUsersWithRole(Role.ADMIN) == 1) {
            throw new IllegalStateException("You are the only admin, so this account can't be deleted. "
                    + "Make someone else an admin first.");
        }
        qaSystem.removeUser(user.getUsername());
        qaSystem.clearCurrentUser();
    }

    public User getCurrentUser() {
        return qaSystem.getCurrentUser();
    }

    public Role getCurrentRole() {
        return qaSystem.getCurrentRole();
    }

    // ---------- Validation (shared by first user and registration) ----------

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Username must be at least " + MIN_USERNAME_LENGTH + " characters.");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Username must be at most " + MAX_USERNAME_LENGTH + " characters.");
        }
        if (username.contains(" ")) {
            throw new IllegalArgumentException("Username cannot contain spaces.");
        }
    }

    /** The rules live in PasswordPolicy, so the sign-up checklist shows exactly these. */
    private void validatePassword(String password) {
        List<PasswordPolicy.Rule> unmet = PasswordPolicy.unmetRules(password);
        if (!unmet.isEmpty()) {
            throw new IllegalArgumentException(
                "Password needs: " + unmet.get(0).getDescription().toLowerCase() + ".");
        }
    }
}
