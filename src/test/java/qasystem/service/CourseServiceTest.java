package qasystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.model.QASystem;
import qasystem.model.Question;
import qasystem.model.Role;
import qasystem.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CourseService: searching, following, and admin-only course/chapter changes.
 */
class CourseServiceTest {

    private QASystem qa;
    private CourseService service;
    private User user;

    // Each test starts logged in as a user who has every role, acting as ADMIN.
    // Tests that need a different role switch with qa.setCurrentRole(...).
    @BeforeEach
    void setUp() {
        qa = new QASystem();
        service = new CourseService(qa);
        user = new User("alice", "pw");
        user.addRole(Role.ADMIN);
        user.addRole(Role.STUDENT);
        qa.addUser(user);
        qa.setCurrentUser(user);
        qa.setCurrentRole(Role.ADMIN);
    }

    // ---------- getAllCourses / findCourse ----------

    // No courses at the start.
    @Test
    void getAllCoursesEmptyAtStart() {
        assertTrue(service.getAllCourses().isEmpty());
    }

    // Courses are returned in the order they were created.
    @Test
    void getAllCoursesReturnsCreatedCourses() {
        Course a = service.createCourse("MAT 117", "Calculus I");
        Course b = service.createCourse("CSE 360", "Software Engineering");
        assertEquals(List.of(a, b), service.getAllCourses());
    }

    // findCourse accepts the code in any format, because it normalizes first.
    @Test
    void findCourseAcceptsAnyFormat() {
        Course c = service.createCourse("MAT 117", "Calculus I");
        assertSame(c, service.findCourse("mat117"));
        assertSame(c, service.findCourse("  Mat 117 "));
    }

    // Unknown (or null) codes return null.
    @Test
    void findCourseReturnsNullWhenMissing() {
        assertNull(service.findCourse("XYZ 999"));
        assertNull(service.findCourse(null));
    }

    // ---------- searchCourses ----------

    // A null or blank search shows every course.
    @Test
    void searchWithEmptyQueryReturnsAll() {
        service.createCourse("MAT 117", "Calculus I");
        service.createCourse("CSE 360", "Software Engineering");
        assertEquals(2, service.searchCourses(null).size());
        assertEquals(2, service.searchCourses("   ").size());
    }

    // Searching by code works even if the user types it without the space.
    @Test
    void searchMatchesCodeInAnyFormat() {
        Course mat = service.createCourse("MAT 117", "Calculus I");
        service.createCourse("CSE 360", "Software Engineering");
        assertEquals(List.of(mat), service.searchCourses("mat117"));
    }

    // Searching by part of the title is case-insensitive.
    @Test
    void searchMatchesTitleCaseInsensitively() {
        service.createCourse("MAT 117", "Calculus I");
        Course cse = service.createCourse("CSE 360", "Software Engineering");
        assertEquals(List.of(cse), service.searchCourses("SOFTWARE"));
    }

    // A search that matches nothing returns an empty list.
    @Test
    void searchWithNoMatchesReturnsEmpty() {
        service.createCourse("MAT 117", "Calculus I");
        assertTrue(service.searchCourses("biology").isEmpty());
    }

    // ---------- following ----------

    // A logged-in user can follow a course, and it shows up in "My Courses".
    @Test
    void followCourseAddsToFollowedCourses() {
        Course c = service.createCourse("MAT 117", "Calculus I");
        qa.setCurrentRole(Role.STUDENT);
        service.followCourse("mat117");
        assertTrue(service.isFollowing("MAT 117"));
        assertEquals(List.of(c), service.getFollowedCourses());
    }

    // You can't follow a course that doesn't exist.
    @Test
    void followCourseRejectsUnknownCourse() {
        assertThrows(IllegalArgumentException.class, () -> service.followCourse("NOPE 1"));
    }

    // Unfollowing removes it from "My Courses".
    @Test
    void unfollowCourseRemovesIt() {
        service.createCourse("MAT 117", "Calculus I");
        service.followCourse("MAT 117");
        service.unfollowCourse("mat 117");
        assertFalse(service.isFollowing("MAT 117"));
        assertTrue(service.getFollowedCourses().isEmpty());
    }

    // Unfollowing something you never followed is harmless (no exception).
    @Test
    void unfollowCourseNotFollowedIsHarmless() {
        assertDoesNotThrow(() -> service.unfollowCourse("MAT 117"));
    }

    // Followed courses keep the order in which they were followed.
    @Test
    void getFollowedCoursesKeepsFollowOrder() {
        Course a = service.createCourse("MAT 117", "Calculus I");
        Course b = service.createCourse("CSE 360", "SE");
        service.followCourse("CSE 360");
        service.followCourse("MAT 117");
        assertEquals(List.of(b, a), service.getFollowedCourses());
    }

    // Following-related methods need someone logged in; otherwise they throw.
    @Test
    void followingMethodsRequireLogin() {
        service.createCourse("MAT 117", "Calculus I");
        qa.clearCurrentUser();
        assertThrows(IllegalStateException.class, () -> service.followCourse("MAT 117"));
        assertThrows(IllegalStateException.class, () -> service.unfollowCourse("MAT 117"));
        assertThrows(IllegalStateException.class, () -> service.isFollowing("MAT 117"));
        assertThrows(IllegalStateException.class, () -> service.getFollowedCourses());
    }

    // ---------- permission checks ----------

    // Only admins can manage courses.
    @Test
    void canManageCoursesOnlyForAdmin() {
        assertTrue(service.canManageCourses());
        qa.setCurrentRole(Role.STUDENT);
        assertFalse(service.canManageCourses());
        qa.setCurrentRole(null);
        assertFalse(service.canManageCourses());
    }

    // Only students have a "My Courses" list.
    @Test
    void canFollowOnlyForStudent() {
        assertFalse(service.canFollow());
        qa.setCurrentRole(Role.STUDENT);
        assertTrue(service.canFollow());
        qa.setCurrentRole(Role.REVIEWER);
        assertFalse(service.canFollow());
    }

    // ---------- createCourse ----------

    // Creating a course normalizes the code and stores it.
    @Test
    void createCourseNormalizesAndStores() {
        Course c = service.createCourse("cse360", "Software Engineering");
        assertEquals("CSE 360", c.getCode());
        assertSame(c, qa.findCourse("CSE 360"));
    }

    // Non-admins are not allowed to create courses.
    @Test
    void createCourseRequiresAdmin() {
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.createCourse("MAT 117", "Calc"));
        assertTrue(service.getAllCourses().isEmpty());
    }

    // The same course (even written differently) can't be created twice.
    @Test
    void createCourseRejectsDuplicate() {
        service.createCourse("MAT 117", "Calculus I");
        assertThrows(IllegalArgumentException.class, () -> service.createCourse("mat117", "Other"));
    }

    // Invalid code/title are rejected (validation comes from Course).
    @Test
    void createCourseRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> service.createCourse("", "Calc"));
        assertThrows(IllegalArgumentException.class, () -> service.createCourse("MAT 117", ""));
    }

    // ---------- deleteCourse ----------

    // Deleting a course removes it, its questions, and follows of it.
    @Test
    void deleteCourseCascades() {
        service.createCourse("MAT 117", "Calculus I");
        service.followCourse("MAT 117");
        qa.addQuestion(new Question(1, "MAT 117", 1, "Q", "", "alice", LocalDateTime.now()));

        service.deleteCourse("mat117");

        assertNull(service.findCourse("MAT 117"));
        assertNull(qa.findQuestion(1));
        assertFalse(user.isFollowing("MAT 117"));
    }

    // Deleting a course that doesn't exist is an error.
    @Test
    void deleteCourseRejectsUnknownCourse() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteCourse("NOPE 1"));
    }

    // Only admins can delete courses.
    @Test
    void deleteCourseRequiresAdmin() {
        service.createCourse("MAT 117", "Calculus I");
        qa.setCurrentRole(Role.REVIEWER);
        assertThrows(IllegalStateException.class, () -> service.deleteCourse("MAT 117"));
        assertNotNull(service.findCourse("MAT 117"));
    }

    // ---------- addChapter ----------

    // Adding a chapter puts it in the course.
    @Test
    void addChapterAddsToCourse() {
        Course c = service.createCourse("MAT 117", "Calculus I");
        Chapter ch = service.addChapter("mat117", 1, "Limits");
        assertSame(ch, c.findChapter(1));
    }

    // Can't add a chapter to a course that doesn't exist.
    @Test
    void addChapterRejectsUnknownCourse() {
        assertThrows(IllegalArgumentException.class, () -> service.addChapter("NOPE 1", 1, "X"));
    }

    // Can't add two chapters with the same number.
    @Test
    void addChapterRejectsDuplicateNumber() {
        service.createCourse("MAT 117", "Calculus I");
        service.addChapter("MAT 117", 1, "Limits");
        assertThrows(IllegalArgumentException.class, () -> service.addChapter("MAT 117", 1, "Other"));
    }

    // Invalid chapter number or title is rejected (validation comes from Chapter).
    @Test
    void addChapterRejectsInvalidInput() {
        service.createCourse("MAT 117", "Calculus I");
        assertThrows(IllegalArgumentException.class, () -> service.addChapter("MAT 117", 0, "X"));
        assertThrows(IllegalArgumentException.class, () -> service.addChapter("MAT 117", 1, " "));
    }

    // Only admins can add chapters.
    @Test
    void addChapterRequiresAdmin() {
        service.createCourse("MAT 117", "Calculus I");
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.addChapter("MAT 117", 1, "Limits"));
    }

    // ---------- deleteChapter ----------

    // Deleting a chapter removes it and its questions.
    @Test
    void deleteChapterRemovesChapterAndQuestions() {
        Course c = service.createCourse("MAT 117", "Calculus I");
        service.addChapter("MAT 117", 1, "Limits");
        qa.addQuestion(new Question(1, "MAT 117", 1, "Q", "", "alice", LocalDateTime.now()));

        service.deleteChapter("mat117", 1);

        assertNull(c.findChapter(1));
        assertNull(qa.findQuestion(1));
    }

    // Deleting a chapter that doesn't exist is an error.
    @Test
    void deleteChapterRejectsUnknownChapter() {
        service.createCourse("MAT 117", "Calculus I");
        assertThrows(IllegalArgumentException.class, () -> service.deleteChapter("MAT 117", 3));
    }

    // Deleting a chapter of an unknown course is an error.
    @Test
    void deleteChapterRejectsUnknownCourse() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteChapter("NOPE 1", 1));
    }

    // Only admins can delete chapters.
    @Test
    void deleteChapterRequiresAdmin() {
        service.createCourse("MAT 117", "Calculus I");
        service.addChapter("MAT 117", 1, "Limits");
        qa.setCurrentRole(Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> service.deleteChapter("MAT 117", 1));
    }
}
