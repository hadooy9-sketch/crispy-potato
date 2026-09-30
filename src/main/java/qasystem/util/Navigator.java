package qasystem.util;

/**
 * Lets controllers switch screens without depending on App.
 * App implements this; controllers only see this small interface.
 */
public interface Navigator {
    void goTo(Screen screen);
}
