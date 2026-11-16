package ma.translogix.controller.rh;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Small reusable dialog that mirrors the "action" modal of the HTML mock-up.
 * Fields are described declaratively and the entered values come back as a map.
 */
public final class RhFormDialog {

    public enum Type { TEXTE, ZONE, DATE, CHOIX }

    public record Champ(String label, String nom, Type type, List<String> options, String prompt, String valeur) {

        public static Champ texte(String label, String nom, String prompt) {
            return new Champ(label, nom, Type.TEXTE, List.of(), prompt, "");
        }

        public static Champ zone(String label, String nom) {
            return new Champ(label, nom, Type.ZONE, List.of(), "", "");
        }

        public static Champ date(String label, String nom) {
            return new Champ(label, nom, Type.DATE, List.of(), "", "");
        }

        public static Champ choix(String label, String nom, List<String> options) {
            return new Champ(label, nom, Type.CHOIX, options, "", options.isEmpty() ? "" : options.get(0));
        }

        public Champ avecValeur(String nouvelleValeur) {
            return new Champ(label, nom, type, options, prompt, nouvelleValeur);
        }
    }

    private RhFormDialog() {
    }

    /**
     * Shows the dialog and blocks until it is closed.
     *
     * @return the entered values, or an empty optional when the user cancelled.
     */
    public static Optional<Map<String, String>> ouvrir(Window proprietaire, String titre, String sousTitre,
                                                       String description, List<Champ> champs) {
        Dialog<ButtonType> dialogue = new Dialog<>();
        if (proprietaire != null) {
            dialogue.initOwner(proprietaire);
        }
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);

        ButtonType enregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);

        var volet = dialogue.getDialogPane();
        volet.getButtonTypes().addAll(enregistrer, ButtonType.CANCEL);
        volet.getStylesheets().add(RhFormDialog.class.getResource("/ma/translogix/css/rh.css").toExternalForm());

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(12);
        grille.setPadding(new Insets(4, 4, 4, 4));

        Label eyebrow = new Label(sousTitre);
        eyebrow.getStyleClass().add("rh-eyebrow");
        Label titreLabel = new Label(titre);
        titreLabel.getStyleClass().add("rh-section-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("rh-muted");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(380);
        grille.add(new javafx.scene.layout.VBox(4, eyebrow, titreLabel, descriptionLabel), 0, 0, 2, 1);

        Map<String, Node> saisies = new LinkedHashMap<>();
        int ligne = 1;
        for (Champ champ : champs) {
            Label etiquette = new Label(champ.label());
            etiquette.getStyleClass().add("rh-kpi-label");
            Node saisie = construire(champ);
            saisies.put(champ.nom(), saisie);
            grille.add(etiquette, 0, ligne);
            grille.add(saisie, 1, ligne);
            ligne++;
        }

        volet.setContent(grille);

        dialogue.setResultConverter(bouton -> bouton);
        Optional<ButtonType> resultat = dialogue.showAndWait();
        if (resultat.isEmpty() || resultat.get() != enregistrer) {
            return Optional.empty();
        }

        Map<String, String> valeurs = new LinkedHashMap<>();
        saisies.forEach((nom, noeud) -> valeurs.put(nom, lire(noeud)));
        return Optional.of(valeurs);
    }

    private static Node construire(Champ champ) {
        switch (champ.type()) {
            case ZONE -> {
                TextArea zone = new TextArea();
                zone.setPrefRowCount(3);
                zone.getStyleClass().add("rh-input");
                zone.setText(champ.valeur());
                return zone;
            }
            case DATE -> {
                DatePicker date = new DatePicker();
                date.getStyleClass().add("rh-input");
                return date;
            }
            case CHOIX -> {
                ComboBox<String> choix = new ComboBox<>();
                choix.getItems().addAll(champ.options());
                choix.setValue(champ.valeur());
                choix.setMaxWidth(Double.MAX_VALUE);
                choix.getStyleClass().add("rh-input");
                return choix;
            }
            default -> {
                TextField texte = new TextField(champ.valeur());
                texte.setPromptText(champ.prompt());
                texte.getStyleClass().add("rh-input");
                return texte;
            }
        }
    }

    private static String lire(Node noeud) {
        if (noeud instanceof TextField texte) {
            return texte.getText();
        }
        if (noeud instanceof TextArea zone) {
            return zone.getText();
        }
        if (noeud instanceof DatePicker date) {
            return date.getValue() == null ? "" : date.getValue().toString();
        }
        if (noeud instanceof ComboBox<?> choix) {
            return choix.getValue() == null ? "" : String.valueOf(choix.getValue());
        }
        return "";
    }
}
