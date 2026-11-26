package ma.translogix;

/**
 * Entry point of the packaged application (jar / Windows app built by jpackage).
 *
 * <p>When the JavaFX jars are on the classpath (not the module path), the JVM refuses to start a main class that
 * extends {@code Application} ("JavaFX runtime components are missing"). This class does not, so it can start
 * the app from a plain classpath. {@code mvnw javafx:run} still uses {@link Main} directly.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
