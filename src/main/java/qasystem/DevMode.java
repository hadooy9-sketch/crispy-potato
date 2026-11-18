package qasystem;

import qasystem.model.QASystem;
import qasystem.model.Role;
import qasystem.model.User;
import qasystem.service.UserService;

/**
 * Developer shortcut: start the app already logged in, skipping the login screen.
 * Only used when the app is started with the "--dev" argument.
 * Dev mode uses its OWN database file, so testing never touches your real data.
 * Package-private: only App can use it.
 */
final class DevMode {

    static final String ARGUMENT = "--dev";
    static final String DATABASE_NAME = "qasystem-dev";
    static final String USERNAME = "dev";
    static final String PASSWORD = "Dev#12345"; // meets the password rules, so you can also log in by hand

    private DevMode() {
        // utility class - never instantiated
    }

    /** Creates a user with every role (only the first time) and logs them in. */
    static void logInDevUser(QASystem qaSystem, UserService userService) {
        if (qaSystem.findUser(USERNAME) == null) {
            User dev = new User(USERNAME, PASSWORD);
            for (Role role : Role.values()) {
                dev.addRole(role);
            }
            qaSystem.addUser(dev);
        }
        userService.login(USERNAME, PASSWORD);
    }
}
