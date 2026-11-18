package qasystem.controller;

import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Tiny view helpers shared by controllers (DRY).
 * Package-private: only controllers use it.
 */
final class Ui {

    private Ui() {
        // utility class - never instantiated
    }

    /** Hidden nodes should not take up space: visible AND managed. */
    static void show(Node node, boolean visible) {
        if (node.visibleProperty().isBound()) {
            return; // collapseWhenEmpty() already controls this node
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** An empty message label should not leave a gap: it only takes space when it has text. */
    static void collapseWhenEmpty(Label label) {
        label.managedProperty().bind(label.textProperty().isNotEmpty());
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
    }

    /** 1 question, 2 questions. */
    static String count(int n, String singular) {
        return n + " " + (n == 1 ? singular : singular + "s");
    }
}
