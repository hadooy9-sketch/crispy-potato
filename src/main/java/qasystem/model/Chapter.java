package qasystem.model;

/**
 * One chapter of a course. Its number is its identity inside the course
 * and never changes, so questions can safely refer to it.
 */
public class Chapter {

    private final int number;
    private final String title;

    public Chapter(int number, String title) {
        if (number < 1) {
            throw new IllegalArgumentException("Chapter number must be 1 or higher.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Chapter title is required.");
        }
        this.number = number;
        this.title = title.trim();
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }
}
