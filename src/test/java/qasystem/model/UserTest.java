package qasystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User: password checks, roles, and followed courses.
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("alice", "Secret#1");
    }

    // The username passed in is stored.
    @Test
    void getUsernameReturnsName() {
        assertEquals("alice", user.getUsername());
    }

    // ---------- password ----------

    // The right password matches.
    @Test
    void checkPasswordAcceptsCorrectPassword() {
        assertTrue(user.checkPassword("Secret#1"));
    }

    // Wrong, differently-cased, and null passwords don't match (and null doesn't crash).
    @Test
    void checkPasswordRejectsWrongPassword() {
        assertFalse(user.checkPassword("wrong"));
        assertFalse(user.checkPassword("secret#1"));
        assertFalse(user.checkPassword(null));
    }

    // changePassword works when the old password is right; the new one then works and the old doesn't.
    @Test
    void changePasswordWithCorrectOldPassword() {
        assertTrue(user.changePassword("Secret#1", "NewPass#2"));
        assertTrue(user.checkPassword("NewPass#2"));
        assertFalse(user.checkPassword("Secret#1"));
    }

    // changePassword fails if the old password is wrong, and the password stays the same.
    @Test
    void changePasswordWithWrongOldPasswordFails() {
        assertFalse(user.changePassword("wrong", "NewPass#2"));
        assertTrue(user.checkPassword("Secret#1"));
    }

    // ---------- roles ----------

    // A new user has no roles.
    @Test
    void newUserHasNoRoles() {
        assertTrue(user.getRoles().isEmpty());
        assertFalse(user.hasRole(Role.STUDENT));
    }

    // Adding a role makes hasRole true; adding it twice keeps only one copy (it's a Set).
    @Test
    void addRoleIsIdempotent() {
        user.addRole(Role.STUDENT);
        user.addRole(Role.STUDENT);
        assertTrue(user.hasRole(Role.STUDENT));
        assertEquals(Set.of(Role.STUDENT), user.getRoles());
    }

    // removeRole returns true if the role was there and false if it wasn't.
    @Test
    void removeRoleReportsWhetherRoleExisted() {
        user.addRole(Role.ADMIN);
        assertTrue(user.removeRole(Role.ADMIN));
        assertFalse(user.hasRole(Role.ADMIN));
        assertFalse(user.removeRole(Role.ADMIN));
    }

    // getRoles is read-only, so roles can only be changed through addRole/removeRole.
    @Test
    void getRolesIsReadOnly() {
        Set<Role> roles = user.getRoles();
        assertThrows(UnsupportedOperationException.class, () -> roles.add(Role.ADMIN));
    }

    // ---------- followed courses ----------

    // Following a course works the first time and returns false the second time.
    @Test
    void followCourseOnlyOnce() {
        assertTrue(user.followCourse("MAT 117"));
        assertFalse(user.followCourse("MAT 117"));
        assertTrue(user.isFollowing("MAT 117"));
    }

    // Unfollowing returns true if you were following, false otherwise.
    @Test
    void unfollowCourseReportsWhetherFollowing() {
        user.followCourse("MAT 117");
        assertTrue(user.unfollowCourse("MAT 117"));
        assertFalse(user.isFollowing("MAT 117"));
        assertFalse(user.unfollowCourse("MAT 117"));
    }

    // Followed courses are listed in the order they were followed.
    @Test
    void getFollowedCourseCodesKeepsFollowOrder() {
        user.followCourse("CSE 360");
        user.followCourse("MAT 117");
        user.followCourse("BIO 100");
        assertEquals(List.of("CSE 360", "MAT 117", "BIO 100"),
                List.copyOf(user.getFollowedCourseCodes()));
    }

    // The followed set is read-only.
    @Test
    void getFollowedCourseCodesIsReadOnly() {
        Set<String> codes = user.getFollowedCourseCodes();
        assertThrows(UnsupportedOperationException.class, () -> codes.add("X 1"));
    }
}
