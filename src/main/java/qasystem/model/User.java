package qasystem.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class User {
    private final String username;
    private String password;
    private final EnumSet<Role> roles;
    private final LinkedHashSet<String> followedCourseCodes; // no duplicates, keeps follow order

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = EnumSet.noneOf(Role.class);
        this.followedCourseCodes = new LinkedHashSet<>();
    }

    public String getUsername() {
        return username;
    }

    // ---------- Password ----------

    public boolean checkPassword(String password) {
        return this.password.equals(password); // this.password is never null
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (checkPassword(oldPassword)) {
            this.password = newPassword;
            return true;
        }
        return false;
    }

    // ---------- Roles ----------

    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean removeRole(Role role) {
        return roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /** Read-only view: callers can look at the roles but cannot change them. */
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    // ---------- Followed courses ----------

    /** Returns false if already following. */
    public boolean followCourse(String courseCode) {
        return followedCourseCodes.add(courseCode);
    }

    /** Returns false if was not following. */
    public boolean unfollowCourse(String courseCode) {
        return followedCourseCodes.remove(courseCode);
    }

    public boolean isFollowing(String courseCode) {
        return followedCourseCodes.contains(courseCode);
    }

    /** Read-only view, in the order the courses were followed. */
    public Set<String> getFollowedCourseCodes() {
        return Collections.unmodifiableSet(followedCourseCodes);
    }
}
