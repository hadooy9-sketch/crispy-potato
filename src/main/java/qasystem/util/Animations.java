package qasystem.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
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

    /** How long a short message stays fully visible before it fades away. */
    private static final Duration MESSAGE_HOLD_TIME = Duration.millis(1600);

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

    /** Fades a node out, then runs {@code then} (for example: remove it). */
    public static void fadeOut(Node node, Runnable then) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.setOnFinished(event -> then.run());
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

    /**
     * Fades out {@code hide} (for example a form card), fades a short message in where it was,
     * holds it for a moment, fades it out, and then runs {@code then} (for example: go to the next screen).
     * Used to say "Admin account created, welcome!" before the login screen.
     */
    public static void showMessageThen(StackPane parent, Node hide, String text, Runnable then) {
        hide.setDisable(true); // stop double clicks while the message plays

        Label message = new Label(text);
        message.getStyleClass().add("welcome-message");
        message.setOpacity(0);
        parent.getChildren().add(message); // a StackPane centers it automatically

        FadeTransition hideCard = new FadeTransition(Duration.millis(300), hide);
        hideCard.setToValue(0);

        FadeTransition messageIn = new FadeTransition(Duration.millis(500), message);
        messageIn.setToValue(1);

        PauseTransition hold = new PauseTransition(MESSAGE_HOLD_TIME);

        FadeTransition messageOut = new FadeTransition(Duration.millis(500), message);
        messageOut.setToValue(0);

        // SequentialTransition plays its steps one after another.
        SequentialTransition steps = new SequentialTransition(hideCard, messageIn, hold, messageOut);
        steps.setOnFinished(event -> then.run());
        steps.play();
    }

    /**
     * Smoothly grows or shrinks a node to the given size (1.0 = normal size).
     * Each button keeps ONE animation and reuses it. Starting a new animation every time
     * the mouse moved in or out left several running at once, fighting each other,
     * which made buttons shake and the app feel laggy.
     */
    private static void scaleTo(Node node, double scale) {
        ScaleTransition grow = (ScaleTransition) node.getProperties().get(HOVER_KEY);
        if (grow == null) {
            grow = new ScaleTransition(HOVER_TIME, node);
            grow.setInterpolator(Interpolator.EASE_BOTH);
            node.getProperties().put(HOVER_KEY, grow);
        }
        grow.stop(); // stop the old animation, then continue from wherever it got to
        grow.setFromX(node.getScaleX());
        grow.setFromY(node.getScaleY());
        grow.setToX(scale);
        grow.setToY(scale);
        grow.play();
    }

    /** The key under which each button remembers its hover animation. */
    private static final String HOVER_KEY = "qasystem.hoverAnimation";
}
