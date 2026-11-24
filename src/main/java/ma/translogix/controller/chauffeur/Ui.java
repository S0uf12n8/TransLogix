package ma.translogix.controller.chauffeur;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.UnaryOperator;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** Petits utilitaires d'interface partagés par les contrôleurs. */
public final class Ui {
    private Ui() {}

    public static final String[] PILL_CLASSES = {"green", "amber", "red", "blue", "purple", "gray"};

    /** Heure courante HH:mm (nowTime() du HTML). */
    public static String nowTime() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")); }

    /** Date courante jj/mm/aaaa (todayShort()). */
    public static String todayShort() { return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }

    /** Date complète en français (Intl dateStyle:'full'). */
    public static String todayFull() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
    }

    /** Crée une pastille de statut (classe .status + couleur). */
    public static Label pill(String text, String cls) {
        Label l = new Label();
        l.getStyleClass().add("status");
        l.setMinWidth(Region.USE_PREF_SIZE);
        setPill(l, cls, text);
        return l;
    }

    /** Met à jour texte + couleur d'une pastille existante. */
    public static void setPill(Label l, String cls, String text) {
        l.getStyleClass().removeIf(c -> c.startsWith("status-"));
        l.getStyleClass().add("status-" + cls);
        l.setText(text);
        if (!(l.getGraphic() instanceof Circle)) {
            Circle dot = new Circle(3);
            dot.getStyleClass().add("status-dot");
            l.setGraphic(dot);
        }
    }

    /** Puce de liste (équivalent de ul.list-disc). */
    public static HBox bullet(String text, String... textClasses) {
        Label b = new Label("•");
        b.setMinWidth(16);
        b.setPrefWidth(16);
        b.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        b.setPadding(new javafx.geometry.Insets(0, 0, 0, 4));
        Label t = new Label(text);
        t.setWrapText(true);
        for (String c : textClasses) { b.getStyleClass().add(c); t.getStyleClass().add(c); }
        HBox.setHgrow(t, javafx.scene.layout.Priority.ALWAYS);
        return new HBox(b, t);
    }

    /** Coin arrondi pour reproduire overflow:hidden + border-radius. */
    public static void clipRounded(Region r, double radius) {
        Rectangle c = new Rectangle();
        c.setArcWidth(radius * 2);
        c.setArcHeight(radius * 2);
        c.widthProperty().bind(r.widthProperty());
        c.heightProperty().bind(r.heightProperty());
        r.setClip(c);
    }

    /** Équivalent d'un champ type="number" : n'accepte que chiffres, signe et point. */
    public static void numberOnly(TextField f) {
        UnaryOperator<TextFormatter.Change> filter = ch -> ch.getControlNewText().matches("-?\\d*\\.?\\d*") ? ch : null;
        f.setTextFormatter(new TextFormatter<>(filter));
    }

    /** Bulle de validation (équivalent de la bulle native des formulaires HTML5). Retourne false. */
    public static boolean invalid(Node c, String message) {
        Tooltip tip = new Tooltip(message);
        tip.getStyleClass().add("validation-tip");
        tip.setStyle("-fx-background-color: white; -fx-text-fill: #172033; -fx-font-size: 12px; -fx-background-radius: 4; "
                + "-fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-padding: 6 10 6 10;");
        c.getStyleClass().add("invalid");
        c.requestFocus();
        javafx.geometry.Bounds b = c.localToScreen(c.getBoundsInLocal());
        if (b != null) tip.show(c, b.getMinX(), b.getMaxY() + 4);
        PauseTransition t = new PauseTransition(Duration.seconds(2.5));
        t.setOnFinished(e -> { tip.hide(); c.getStyleClass().remove("invalid"); });
        t.play();
        return false;
    }

    public static boolean requireText(TextField f) {
        if (f.getText() == null || f.getText().isEmpty()) return invalid(f, "Veuillez renseigner ce champ.");
        return true;
    }

    /** Champ numérique obligatoire ; min = null si pas de minimum. */
    public static boolean requireNumber(TextField f, Double min) {
        if (!requireText(f)) return false;
        String s = f.getText();
        if (!s.matches("-?\\d+")) return invalid(f, "Veuillez saisir une valeur valide.");
        if (min != null && Double.parseDouble(s) < min)
            return invalid(f, "La valeur doit être supérieure ou égale à " + min.intValue() + ".");
        return true;
    }

    /** Premier mot d'une chaîne (split(' ')[0]). */
    public static String firstToken(String s) { return s == null ? "" : s.split(" ")[0]; }

    public static void setVisibleManaged(Node n, boolean v) { n.setVisible(v); n.setManaged(v); }
}
