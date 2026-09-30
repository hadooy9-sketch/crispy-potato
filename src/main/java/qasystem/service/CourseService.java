package qasystem.service;

import qasystem.model.Chapter;
import qasystem.model.Course;
import qasystem.model.QASystem;
import qasystem.model.Role;
import qasystem.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns course and chapter behavior.
 * Anyone logged in can read, search, and follow courses; only admins can change them.
 * Deleting cascades to questions and follows - QASystem handles that so it can never be forgotten.
 */
public class CourseService {

    private final QASystem qaSystem;

    public CourseService(QASystem qaSystem) {
        this.qaSystem = qaSystem;
    }

    // =====================================================================
    // Reading and searching (any logged-in user)
    // =====================================================================

    public List<Course> getAllCourses() {
        return qaSystem.getCourses();
    }

    /** Returns the course, or null. Accepts codes in any format ("mat117"). */
    public Course findCourse(String code) {
        return qaSystem.findCourse(Course.normalizeCode(code));
    }

    /**
     * Courses whose code or title matches the query.
     * "mat117" matches "MAT 117"; "calc" matches "Calculus I". Empty query -> all courses.
     */
    public List<Course> searchCourses(String query) {
        if (query == null || query.isBlank()) {
            return getAllCourses();
        }
        String codeQuery = Course.normalizeCode(query);
        String titleQuery = query.trim().toLowerCase();

        List<Course> matches = new ArrayList<>();
        for (Course course : qaSystem.getCourses()) {
            if (course.getCode().contains(codeQuery)
                    || course.getTitle().toLowerCase().contains(titleQuery)) {
                matches.add(course);
            }
        }
        return matches;
    }

    // =====================================================================
    // Following (any logged-in user)
    // =====================================================================

    /** The current user's followed courses, in the order they followed them. */
    public List<Course> getFollowedCourses() {
        List<Course> result = new ArrayList<>();
        for (String code : requireUser().getFollowedCourseCodes()) {
            Course course = qaSystem.findCourse(code);
            if (course != null) { // safety net: QASystem already removes follows of deleted courses
                result.add(course);
            }
        }
        return result;
    }

    public void followCourse(String code) {
        Course course = requireCourse(code);
        requireUser().followCourse(course.getCode());
    }

    public void unfollowCourse(String code) {
        requireUser().unfollowCourse(Course.normalizeCode(code));
    }

    public boolean isFollowing(String code) {
        return requireUser().isFollowing(Course.normalizeCode(code));
    }

    // =====================================================================
    // Permission checks for the UI
    // =====================================================================

    /** Admins see the "+" cards and delete buttons. */
    public boolean canManageCourses() {
        return qaSystem.getCurrentRole() == Role.ADMIN;
    }

    /** Students keep a "My Courses" list. */
    public boolean canFollow() {
        return qaSystem.getCurrentRole() == Role.STUDENT;
    }

    // =====================================================================
    // Courses (admin only)
    // =====================================================================

    public Course createCourse(String code, String title) {
        Access.requireRole(qaSystem, Role.ADMIN, "create courses");
        Course course = new Course(code, title); // validates and normalizes
        if (!qaSystem.addCourse(course)) {
            throw new IllegalArgumentException("Course " + course.getCode() + " already exists.");
        }
        return course;
    }

    /** Deletes the course, its chapters, its questions, and everyone's follow of it. */
    public void deleteCourse(String code) {
        Access.requireRole(qaSystem, Role.ADMIN, "delete courses");
        String normalized = Course.normalizeCode(code);
        if (!qaSystem.removeCourse(normalized)) {
            throw new IllegalArgumentException("Course " + normalized + " does not exist.");
        }
    }

    // =====================================================================
    // Chapters (admin only)
    // =====================================================================

    public Chapter addChapter(String courseCode, int number, String title) {
        Access.requireRole(qaSystem, Role.ADMIN, "add chapters");
        Course course = requireCourse(courseCode);
        Chapter chapter = new Chapter(number, title); // validates
        if (!course.addChapter(chapter)) {
            throw new IllegalArgumentException(
                "Chapter " + number + " already exists in " + course.getCode() + ".");
        }
        return chapter;
    }

    /** Deletes the chapter and all of its questions. */
    public void deleteChapter(String courseCode, int number) {
        Access.requireRole(qaSystem, Role.ADMIN, "delete chapters");
        Course course = requireCourse(courseCode);
        if (!qaSystem.removeChapter(course.getCode(), number)) {
            throw new IllegalArgumentException(
                "Chapter " + number + " does not exist in " + course.getCode() + ".");
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Course requireCourse(String code) {
        Course course = findCourse(code);
        if (course == null) {
            throw new IllegalArgumentException("Course " + Course.normalizeCode(code) + " does not exist.");
        }
        return course;
    }

    private User requireUser() {
        User user = qaSystem.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No user is logged in.");
        }
        return user;
    }
}
