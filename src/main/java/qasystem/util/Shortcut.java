package qasystem.util;

/**
 * Every keyboard shortcut in the app, in ONE place.
 *
 * This list is only data: the keys to show, a name, a description and who it is for.
 *  - ShortcutManager decides what each key DOES.
 *  - ShortcutHints draws the key badges and the Word-style tooltips.
 *  - The "Keyboard shortcuts" page in the side menu is built from this list,
 *    so adding a shortcut here also adds it to the help page automatically.
 */
public enum Shortcut {

    // ----- Everyone -----
    BACK(Group.EVERYONE, "Q", "Back",
            "Go back to the previous page.", "back-button"),
    HOME(Group.EVERYONE, "H", "Home",
            "Go to your home page from anywhere.", null),
    PREVIOUS_ITEM(Group.EVERYONE, "W", "Move up",
            "Highlight the previous card or question.", null),
    NEXT_ITEM(Group.EVERYONE, "S", "Move down",
            "Highlight the next card or question.", null),
    OPEN_ITEM(Group.EVERYONE, "D / Enter", "Open",
            "Open the highlighted card or question.", null),
    OPEN_CHAPTER(Group.EVERYONE, "1 – 9", "Open chapter",
            "On a course page, open chapter 1 to 9.", null),
    ESCAPE(Group.EVERYONE, "Esc", "Stop typing / close",
            "Leave the text box you are typing in, or close the menu or a form.", null),
    SHOW_SHORTCUTS(Group.EVERYONE, "?", "Keyboard shortcuts",
            "Show this list of shortcuts.", null),

    // ----- Student -----
    ASK(Group.STUDENT, "E", "Ask a question",
            "On a course or chapter page, start a new question.", "shortcut-ask"),

    // ----- Reviewer -----
    NEXT_REVIEW(Group.REVIEWER, "R", "Next review",
            "Open the next question with an answer waiting for your review.", null),

    // ----- Admin -----
    ADD_NEW(Group.ADMIN, "N", "Add new",
            "Add a course on the home page, or a chapter on a course page.", "shortcut-add");

    /** Who a shortcut is for. Used to group the help page. */
    public enum Group {
        EVERYONE("Everyone"),
        STUDENT("Student"),
        REVIEWER("Reviewer"),
        ADMIN("Admin");

        private final String displayName;

        Group(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private final Group group;
    private final String keys;
    private final String title;
    private final String description;
    private final String targetClass;

    /**
     * @param keys        what the user presses, as shown on screen. Keys are separated by
     *                    spaces; "+" and "/" between them are drawn as plain text.
     * @param targetClass style class of the button this key presses, or null if the key
     *                    does something else (moves the highlight, goes home, ...)
     */
    Shortcut(Group group, String keys, String title, String description, String targetClass) {
        this.group = group;
        this.keys = keys;
        this.title = title;
        this.description = description;
        this.targetClass = targetClass;
    }

    public Group group() {
        return group;
    }

    public String keys() {
        return keys;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** The style class that marks this shortcut's button in FXML or code, or null. */
    public String targetClass() {
        return targetClass;
    }
}
