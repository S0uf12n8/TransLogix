package ma.translogix.controller.rh;

import java.util.function.Consumer;

/**
 * Lets the RH sub-screens push a toast message without knowing the layout.
 * {@code RhLayoutController} installs the sink when it initialises.
 */
public final class RhNotify {

    private static Consumer<String> sink = message -> { };

    private RhNotify() {
    }

    public static void setSink(Consumer<String> nouveauSink) {
        sink = nouveauSink == null ? message -> { } : nouveauSink;
    }

    public static void toast(String message) {
        sink.accept(message);
    }
}
