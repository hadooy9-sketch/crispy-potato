package qasystem.service;

import org.junit.jupiter.api.Test;

import qasystem.model.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService.deleteCurrentAccount() (the "Delete account" button in the menu).
 * Uses the team's ServiceTestBase: a fresh in-memory database and an "admin" user for every test.
 */
class DeleteAccountTest extends ServiceTestBase {

    // A student can delete their own account; they are logged out and can't log in again.
    @Test
    void studentCanDeleteOwnAccount() {
        createStudent("alice");
        loginAs("alice", Role.STUDENT);

        userService.deleteCurrentAccount();

        assertNull(userService.getCurrentUser());
        assertNull(qaSystem.findUser("alice"));
        assertFalse(userService.login("alice", PASSWORD));
    }

    // Someone who posted keeps their posts, but the account is closed and can't log in.
    @Test
    void accountWithPostsIsClosedAndPostsStay() {
        createCourse("CSE 360", "Software Engineering", 1);
        createStudent("bob");
        loginAs("bob", Role.STUDENT);
        long id = questionService.askQuestion("CSE 360", 1, "Why test?", "Just wondering").getId();

        userService.deleteCurrentAccount();

        assertNull(userService.getCurrentUser());
        assertFalse(userService.login("bob", PASSWORD));
        assertEquals("bob", questionService.findQuestion(id).getAuthor());
    }

    // The only admin can't delete themselves, so the system never ends up without an admin.
    @Test
    void onlyAdminCannotDeleteThemselves() {
        loginAs(ADMIN, Role.ADMIN);

        assertThrows(IllegalStateException.class, () -> userService.deleteCurrentAccount());
        assertNotNull(qaSystem.findUser(ADMIN));
        assertNotNull(userService.getCurrentUser());
    }

    // With a second admin around, an admin may delete their account.
    @Test
    void adminCanDeleteWhenAnotherAdminExists() {
        createUser("boss2", Role.ADMIN);
        loginAs(ADMIN, Role.ADMIN);

        userService.deleteCurrentAccount();

        assertNull(qaSystem.findUser(ADMIN));
        assertNotNull(qaSystem.findUser("boss2"));
    }

    // Deleting needs someone to be logged in.
    @Test
    void deleteWithoutLoginFails() {
        userService.logout();
        assertThrows(IllegalStateException.class, () -> userService.deleteCurrentAccount());
    }
}
