package qasystem.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.Random;

/**
 * Small birds that drift across the background of a screen now and then.
 * Each bird is just a "V" line (two curves) that flaps by squashing up and down.
 * The birds live on their own see-through layer behind everything else,
 * and that layer ignores the mouse, so it never gets in the way of clicks.
 */
public final class Birds {

    /** How often new birds may appear. */
    private static final Duration SPAWN_EVERY = Duration.seconds(5);

    /** How long one bird takes to cross the screen (picked between these two). */
    private static final double MIN_FLIGHT_SECONDS = 12;
    private static final double MAX_FLIGHT_SECONDS = 18;

    /** Bird color: the app's maroon, mostly see-through so it stays in the background. */
    private static final Color BIRD_COLOR = Color.web("#7A1E2C", 0.45);

    private static final Random RANDOM = new Random();

    private Birds() {
        // Only static helpers.
    }

    /**
     * Starts sending birds across the given screen.
     * The birds stop by themselves when the screen is closed.
     */
    public static void addTo(StackPane screen) {
        Pane sky = new Pane();
        sky.setMouseTransparent(true); // clicks pass straight through to the buttons and fields
        sky.setManaged(false);         // the StackPane won't try to center or size it

        // Make the sky layer exactly as big as the screen, and hide anything outside it.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(screen.widthProperty());
        clip.heightProperty().bind(screen.heightProperty());
        sky.setClip(clip);
        screen.widthProperty().addListener((obs, oldW, newW) -> sky.resize(newW.doubleValue(), screen.getHeight()));
        screen.heightProperty().addListener((obs, oldH, newH) -> sky.resize(screen.getWidth(), newH.doubleValue()));

        sky.resize(screen.getWidth(), screen.getHeight());
        screen.getChildren().add(0, sky); // index 0 = drawn first = behind the card

        // Every few seconds, send 1 or 2 birds. The first one comes after 1 second.
        Timeline spawner = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> launchBird(sky)),
                new KeyFrame(SPAWN_EVERY, e -> {
                    int count = 1 + RANDOM.nextInt(2); // 1 or 2
                    for (int i = 0; i < count; i++) {
                        launchBird(sky);
                    }
                }));
        spawner.setCycleCount(Animation.INDEFINITE);
        spawner.play();

        // When the user leaves this screen, it is removed from the window: stop making birds.
        sky.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                spawner.stop();
            }
        });
    }

    /** Creates one bird and flies it from the left edge to the right edge, then removes it. */
    private static void launchBird(Pane sky) {
        double width = sky.getWidth();
        double height = sky.getHeight();
        if (width <= 0 || height <= 0) {
            return; // screen not laid out yet
        }

        double size = 14 + RANDOM.nextDouble() * 8;     // wingspan varies a little
        Path bird = makeBird(size);

        // Fly somewhere in the top 40% of the screen (the sky part of the picture).
        double startY = height * (0.08 + RANDOM.nextDouble() * 0.32);
        bird.setLayoutX(-size * 3);
        bird.setLayoutY(startY);
        sky.getChildren().add(bird);

        // Wings flap: squash the "V" flat and back, over and over.
        ScaleTransition flap = new ScaleTransition(Duration.millis(260 + RANDOM.nextInt(120)), bird);
        flap.setFromY(1.0);
        flap.setToY(0.3);
        flap.setAutoReverse(true);
        flap.setCycleCount(Animation.INDEFINITE);
        flap.play();

        // Glide across, drifting slightly up or down on the way.
        double seconds = MIN_FLIGHT_SECONDS + RANDOM.nextDouble() * (MAX_FLIGHT_SECONDS - MIN_FLIGHT_SECONDS);
        TranslateTransition glide = new TranslateTransition(Duration.seconds(seconds), bird);
        glide.setByX(width + size * 6);
        glide.setByY((RANDOM.nextDouble() - 0.5) * height * 0.15);
        glide.setInterpolator(javafx.animation.Interpolator.LINEAR); // steady speed, like gliding
        glide.setOnFinished(e -> {
            flap.stop();
            sky.getChildren().remove(bird); // clean up so birds don't pile up
        });
        glide.play();
    }

    /** A simple bird: two curved wings joined in the middle, like a soft "V". */
    private static Path makeBird(double size) {
        double half = size / 2;
        Path bird = new Path(
                new MoveTo(-half, 0),
                new QuadCurveTo(-half / 2, -half / 2, 0, half / 4),  // left wing
                new QuadCurveTo(half / 2, -half / 2, half, 0));      // right wing
        bird.setStroke(BIRD_COLOR);
        bird.setStrokeWidth(2);
        bird.setStrokeLineCap(StrokeLineCap.ROUND);
        bird.setFill(null);
        return bird;
    }
}
