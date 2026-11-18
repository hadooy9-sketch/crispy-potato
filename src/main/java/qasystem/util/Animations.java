package qasystem.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Small, gentle animations used across the app.
 * Keeping them here (instead of inside each controller) means every screen
 * moves the same way, and the timing can be tuned in one place.
 * Uses only javafx.animation, which ships with JavaFX - no extra dependencies.
 */
public final class Animations {

    /** How long a normal screen switch takes. Long enough to look smooth, short enough not to feel slow. */
    private static final Duration SWITCH_TIME = Duration.millis(450);

    /** The first screen gets a longer, softer entrance. */
    private static final Duration ENTRANCE_TIME = Duration.millis(900);

    /** How far (in pixels) the first screen drifts up while it appears. */
    private static final double ENTRANCE_RISE = 16;

    /** How long a button takes to grow or shrink when the mouse moves over it. */
    private static final Duration HOVER_TIME = Duration.millis(150);

    /** How big a hovered button gets: 1.04 means 4% bigger. */
    private static final double HOVER_SCALE = 1.04;

    private Animations() {
        // Only static helpers; nobody needs to create an Animations object.
    }

    /** Fades a node from invisible to fully visible. Used when switching screens. */
    public static void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(SWITCH_TIME, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        // EASE_BOTH starts slowly, speeds up, then slows down at the end,
        // which looks much softer than changing at a constant speed.
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.play();
    }

    /**
     * Fades a node in while it drifts up a little. Used once, when the app first opens,
     * so the login screen eases into view instead of popping up.
     */
    public static void entrance(Node node) {
        FadeTransition fade = new FadeTransition(ENTRANCE_TIME, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition rise = new TranslateTransition(ENTRANCE_TIME, node);
        rise.setFromY(ENTRANCE_RISE);
        rise.setToY(0);

        // ParallelTransition runs both at the same time.
        ParallelTransition both = new ParallelTransition(fade, rise);
        both.setInterpolator(Interpolator.EASE_BOTH);
        both.play();
    }

    /**
     * Makes every button in the scene grow slightly while the mouse is over it.
     * This listens on the whole Scene, so it also works for buttons on screens
     * loaded later and buttons that controllers create in code. Call it once.
     */
    public static void enableButtonHover(Scene scene) {
        // The "_TARGET" versions of these events pass through the scene for every node,
        // so the scene can see when the mouse enters or leaves any button.
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, event -> {
            if (event.getTarget() instanceof Button) {
                scaleTo((Button) event.getTarget(), HOVER_SCALE);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> {
            if (event.getTarget() instanceof Button) {
                scaleTo((Button) event.getTarget(), 1.0);
            }
        });
    }

    /** Smoothly grows or shrinks a node to the given size (1.0 = normal size). */
    private static void scaleTo(Node node, double scale) {
        ScaleTransition grow = new ScaleTransition(HOVER_TIME, node);
        grow.setToX(scale);
        grow.setToY(scale);
        grow.setInterpolator(Interpolator.EASE_BOTH);
        grow.play();
    }
}
