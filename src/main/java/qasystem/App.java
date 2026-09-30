package qasystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import qasystem.model.QASystem;
import qasystem.service.CourseService;
import qasystem.service.InvitationService;
import qasystem.service.QuestionService;
import qasystem.service.ReviewService;
import qasystem.service.UserService;
import qasystem.util.Navigator;
import qasystem.util.Screen;
import qasystem.util.ViewContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Owns the application lifetime: creates the single QASystem, the services,
 * and the shared ViewContext; builds every controller; switches screens (as the Navigator).
 */
public class App extends Application implements Navigator {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 820;

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
        Clock clock = Clock.systemDefaultZone(); // tests will pass a fixed clock instead

        qaSystem = new QASystem();
        userService = new UserService(qaSystem, clock);
        invitationService = new InvitationService(qaSystem, clock);
        courseService = new CourseService(qaSystem);
        questionService = new QuestionService(qaSystem, clock);
        reviewService = new ReviewService(qaSystem, clock);
        viewContext = new ViewContext();
        registerControllers();

        Screen firstScreen = userService.needsFirstUser() ? Screen.FIRST_USER : Screen.LOGIN;

        scene = new Scene(loadFXML(firstScreen), WINDOW_WIDTH, WINDOW_HEIGHT);
        // One stylesheet for the whole app. It stays on the Scene when goTo() swaps the root.
        scene.getStylesheets().add(App.class.getResource("app.css").toExternalForm());

        stage.setTitle("Q&A System");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
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
            scene.setRoot(loadFXML(screen));
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
        launch();
    }
}
