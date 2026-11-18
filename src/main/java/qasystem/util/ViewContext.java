package qasystem.util;

/**
 * UI state shared between screens: what the user is currently looking at.
 * Kept separate from QASystem, which holds what the system CONTAINS.
 * The select methods keep the pieces consistent with each other.
 */
public class ViewContext {

    private String selectedCourseCode;
    private Integer selectedChapterNumber; // null = the "Unresolved" tab
    private Long selectedQuestionId;

    /** Opening a course: the previous course's tab and question no longer apply. */
    public void selectCourse(String courseCode) {
        this.selectedCourseCode = courseCode;
        this.selectedChapterNumber = null;
        this.selectedQuestionId = null;
    }

    /** Remembers which tab is open, so coming back reopens it. null = "Unresolved". */
    public void selectChapter(Integer chapterNumber) {
        this.selectedChapterNumber = chapterNumber;
    }

    public void selectQuestion(long questionId) {
        this.selectedQuestionId = questionId;
    }

    public String getSelectedCourseCode() {
        return selectedCourseCode;
    }

    public Integer getSelectedChapterNumber() {
        return selectedChapterNumber;
    }

    public Long getSelectedQuestionId() {
        return selectedQuestionId;
    }
}
