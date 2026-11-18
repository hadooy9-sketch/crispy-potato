package qasystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.model.Role;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CourseService: courses, chapters, following, and cascading deletes")
class CourseServiceTest extends ServiceTestBase {

    private static List<Integer> chapterNumbers(Course course) {
        List<Integer> numbers = new ArrayList<>();
        for (Chapter chapter : course.getChapters()) {
            numbers.add(chapter.getNumber());
        }
        return numbers;
    }

    // ---------- Courses ----------

    @Test
    @DisplayName("Course codes are normalized, so the same course cannot be added twice")
    void codesAreNormalized() {
        loginAs(ADMIN, Role.ADMIN);

        Course course = courseService.createCourse("mat117", "Calculus I");

        assertEquals("MAT 117", course.getCode());
        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse("  Mat   117 ", "Duplicate"));
    }

    @Test
    @DisplayName("Only an admin can create courses")
    void onlyAdminCreatesCourses() {
        createStudent("sam");
        loginAs("sam", Role.STUDENT);

        assertThrows(IllegalStateException.class, () -> courseService.createCourse("MAT 117", "Calculus I"));
    }

    @Test
    @DisplayName("Search matches codes in any format and titles in any case")
    void searchCourses() {
        createCourse("MAT 117", "Calculus I");
        createCourse("MAT 275", "Differential Equations");
        loginAs(ADMIN, Role.ADMIN);

        assertEquals(1, courseService.searchCourses("calc").size());
        assertEquals(1, courseService.searchCourses("mat275").size());
        assertEquals(2, courseService.searchCourses("  ").size());
        assertTrue(courseService.searchCourses("physics").isEmpty());
    }

    // ---------- Chapters ----------

    @Test
    @DisplayName("Chapters stay sorted and keep their numbers when one is deleted")
    void chaptersSortedAndStable() {
        createCourse("MAT 117", "Calculus I", 5, 1, 3);
        loginAs(ADMIN, Role.ADMIN);
        Course course = courseService.findCourse("MAT 117");

        assertEquals(List.of(1, 3, 5), chapterNumbers(course));
        assertEquals(6, course.nextChapterNumber());

        courseService.deleteChapter("MAT 117", 3);

        assertEquals(List.of(1, 5), chapterNumbers(courseService.findCourse("MAT 117"))); // re-read
    }

    @Test
    @DisplayName("A chapter number cannot be used twice in one course")
    void duplicateChapterRejected() {
        createCourse("MAT 117", "Calculus I", 1);
        loginAs(ADMIN, Role.ADMIN);

        assertThrows(IllegalArgumentException.class, () -> courseService.addChapter("MAT 117", 1, "Again"));
    }

    // ---------- Cascades ----------

    @Test
    @DisplayName("Deleting a course deletes its questions and everyone's follow of it")
    void deleteCourseCascades() {
        createCourse("MAT 117", "Calculus I", 1);
        createStudent("sam");
        loginAs("sam", Role.STUDENT);
        courseService.followCourse("MAT 117");
        questionService.askQuestion("MAT 117", 1, "What is a limit?", "");

        loginAs(ADMIN, Role.ADMIN);
        courseService.deleteCourse("MAT 117");

        assertTrue(questionService.getUnresolvedQuestions().isEmpty());
        loginAs("sam", Role.STUDENT);
        assertTrue(courseService.getFollowedCourses().isEmpty());
        assertFalse(courseService.isFollowing("MAT 117"));
    }

    @Test
    @DisplayName("Deleting a chapter deletes only that chapter's questions")
    void deleteChapterCascades() {
        createCourse("MAT 117", "Calculus I", 1, 2);
        createStudent("sam");
        loginAs("sam", Role.STUDENT);
        questionService.askQuestion("MAT 117", 1, "What is a limit?", "");
        questionService.askQuestion("MAT 117", 2, "What is a derivative?", "");

        loginAs(ADMIN, Role.ADMIN);
        courseService.deleteChapter("MAT 117", 1);

        assertTrue(questionService.getQuestions("MAT 117", 1).isEmpty());
        assertEquals(1, questionService.getQuestions("MAT 117", 2).size());
    }

    // ---------- Following ----------

    @Test
    @DisplayName("Followed courses keep the order they were followed in")
    void followOrder() {
        createCourse("MAT 117", "Calculus I");
        createCourse("MAT 275", "Differential Equations");
        createStudent("sam");
        loginAs("sam", Role.STUDENT);

        courseService.followCourse("mat275");
        courseService.followCourse("MAT 117");
        courseService.followCourse("MAT 275"); // already following: no duplicate

        List<Course> followed = courseService.getFollowedCourses();
        assertEquals(2, followed.size());
        assertEquals("MAT 275", followed.get(0).getCode());
        assertEquals("MAT 117", followed.get(1).getCode());
    }
}
