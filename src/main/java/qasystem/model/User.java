package qasystem.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import qasystem.util.PasswordHasher;

/**
 * An account. Stores a password HASH, never the password itself.
 *
 * Like every model object loaded from the database, a User is a snapshot:
 * changing it does not change the database. Changes go through QASystem.
 */
public class User {

    private final String username;
    private final String passwordHash;
    private final EnumSet<Role> roles;

    /** A brand-new account: hashes the password right away. */
    public User(String username, String password) {
        this(username, PasswordHasher.hash(password), EnumSet.noneOf(Role.class));
    }

    private User(String username, String passwordHash, Set<Role> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    }

    /** Rebuilds a saved account. Package-private: only QASystem loads users. */
    static User fromStorage(String username, String passwordHash, Set<Role> roles) {
        return new User(username, passwordHash, roles);
    }

    public String getUsername() {
        return username;
    }

    /** Package-private: only QASystem needs it, to save the user. */
    String getPasswordHash() {
        return passwordHash;
    }

    public boolean checkPassword(String password) {
        return PasswordHasher.matches(password, passwordHash);
    }

    // ---------- Roles ----------

    /** Used while creating a new account, before it is saved. */
    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /** Read-only view: callers can look at the roles but cannot change them. */
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
