package qasystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import qasystem.controller.AskQuestionController;
import qasystem.controller.ChapterViewController;
import qasystem.controller.CoursesHomeController;
import qasystem.controller.CourseViewController;
import qasystem.controller.FirstUserController;
import qasystem.controller.InvitationsController;
import qasystem.controller.LoginController;
import qasystem.controller.QuestionDetailController;
import qasystem.controller.RegisterController;
import qasystem.controller.ReviewerController;
import qasystem.controller.RoleSelectController;
import qasystem.data.DataAccessException;
import qasystem.data.Database;
import qasystem.model.QASystem;
import qasystem.service.CourseService;
import qasystem.service.InvitationService;
import qasystem.service.QuestionService;
import qasystem.service.ReviewService;
import qasystem.service.UserService;
import qasystem.util.Animations;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Owns the application lifetime: opens the database, creates the QASystem,
 * the services and the shared ViewContext; builds every controller;
 * switches screens (as the Navigator); closes the database on exit.
 */
public class App extends Application implements Navigator {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 820;

    /** H2's error code for "this database file is already open in another program". */
    private static final int H2_DATABASE_ALREADY_OPEN = 90020;

    private Database database;
    private QASystem qaSystem;
    private UserService userService;
    private InvitationService invitationService;
    private CourseService courseService;
    private QuestionService questionService;
    private ReviewService reviewService;
    private ViewContext viewContext;
    private Scene scene;

    /** Controller class -> how to build it. One line per controller. */
    private final Map<Class<?>, Supplier<Object>> controllerFactories = new HashMap<>();

    @Override
    public void start(Stage stage) throws IOException {
        // Started with "--dev"? Skip login, and use the separate dev database.
        boolean devMode = getParameters().getRaw().contains(DevMode.ARGUMENT);

        try {
            database = Database.open(devMode ? DevMode.DATABASE_NAME : "qasystem");
        } catch (DataAccessException e) {
            showStartupError(e);
            return;
        }

        Clock clock = Clock.systemDefaultZone(); // tests will pass a fixed clock instead

        qaSystem = new QASystem(database);
        userService = new UserService(qaSystem, clock);
        invitationService = new InvitationService(qaSystem, clock);
        courseService = new CourseService(qaSystem);
        questionService = new QuestionService(qaSystem, clock);
        reviewService = new ReviewService(qaSystem, clock);
        viewContext = new ViewContext();
        registerControllers();

        Screen firstScreen;
        if (devMode) {
            DevMode.logInDevUser(qaSystem, userService);
            firstScreen = Screen.ROLE_SELECT;
        } else {
            firstScreen = userService.needsFirstUser() ? Screen.FIRST_USER : Screen.LOGIN;
        }

        loadFonts(); // must run before the stylesheet refers to the font names
        scene = new Scene(loadFXML(firstScreen), WINDOW_WIDTH, WINDOW_HEIGHT);
        // One stylesheet for the whole app. It stays on the Scene when goTo() swaps the root.
        scene.getStylesheets().add(App.class.getResource("app.css").toExternalForm());
        // While a screen fades in, the window color behind it shows through. Matching it to the
        // app's pastel orange background (-app-bg in app.css) stops the fade looking like a white flash.
        scene.setFill(Color.web("#FFD7B0"));
        // Buttons grow a little when the mouse is over them (works on every screen).
        Animations.enableButtonHover(scene);

        stage.setTitle(devMode ? "Q&A System  [DEV MODE]" : "Q&A System");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();

        // Ease the first screen (usually the login screen) into view.
        Animations.entrance(scene.getRoot());
    }

    /** Font files bundled in /qasystem/fonts. CSS can only use a font after it is loaded. */
    private static final String[] FONT_FILES = {
            "DMSerifDisplay-Regular.ttf", // headings: "DM Serif Display"
            "DMSerifDisplay-Italic.ttf",
            "DMSans-Regular.ttf",          // body text: "DM Sans"
            "DMSans-Bold.ttf",
            "DMMono-Regular.ttf"           // codes: "DM Mono"
    };

    private void loadFonts() {
        for (String file : FONT_FILES) {
            try (InputStream in = App.class.getResourceAsStream("fonts/" + file)) {
                if (in == null || Font.loadFont(in, 14) == null) {
                    System.err.println("Could not load font: " + file);
                }
            } catch (IOException e) {
                System.err.println("Could not load font: " + file + " (" + e.getMessage() + ")");
            }
        }
    }

    /** Called by JavaFX when the window closes: release the database file. */
    @Override
    public void stop() {
        if (database != null) {
            database.close();
        }
    }

    /** The database could not be opened: explain why, then quit. */
    private void showStartupError(DataAccessException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Q&A System");
        alert.setHeaderText("The database could not be opened.");
        alert.setContentText(e.getErrorCode() == H2_DATABASE_ALREADY_OPEN
                ? "The Q&A System is probably already open in another window. Close it and try again."
                : e.getMessage());
        alert.showAndWait();
        Platform.exit();
    }

    private void registerControllers() {
        controllerFactories.put(LoginController.class,
                () -> new LoginController(userService, this));
        controllerFactories.put(FirstUserController.class,
                () -> new FirstUserController(userService, this));
        controllerFactories.put(RegisterController.class,
                () -> new RegisterController(userService, this));
        controllerFactories.put(RoleSelectController.class,
                () -> new RoleSelectController(userService, this));
        controllerFactories.put(CoursesHomeController.class,
                () -> new CoursesHomeController(userService, courseService, questionService, viewContext, this));
        controllerFactories.put(InvitationsController.class,
                () -> new InvitationsController(invitationService, this));
        controllerFactories.put(CourseViewController.class,
                () -> new CourseViewController(userService, courseService, questionService, viewContext, this));
        controllerFactories.put(ChapterViewController.class,
                () -> new ChapterViewController(courseService, questionService, viewContext, this));
        controllerFactories.put(AskQuestionController.class,
                () -> new AskQuestionController(courseService, questionService, viewContext, this));
        controllerFactories.put(QuestionDetailController.class,
                () -> new QuestionDetailController(questionService, reviewService, viewContext, this));
        controllerFactories.put(ReviewerController.class,
                () -> new ReviewerController(userService, courseService, questionService, reviewService, viewContext, this));
    }

    /** Navigator: replace what the window shows. */
    @Override
    public void goTo(Screen screen) {
        try {
            Parent root = loadFXML(screen);
            scene.setRoot(root);
            // Fade the new screen in so the switch feels smooth instead of sudden.
            Animations.fadeIn(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load screen " + screen, e);
        }
    }

    private Parent loadFXML(Screen screen) throws IOException {
        URL url = App.class.getResource(screen.getFxmlFile());
        if (url == null) {
            throw new IOException("FXML file not found: " + screen.getFxmlFile());
        }
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(this::createController);
        return loader.load();
    }

    /** Called by FXMLLoader when it reads fx:controller. */
    private Object createController(Class<?> type) {
        Supplier<Object> factory = controllerFactories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No controller registered for " + type.getName());
        }
        return factory.get();
    }

    public static void main(String[] args) {
        launch(args); // pass the arguments on, so getParameters() can see "--dev"
    }
}
