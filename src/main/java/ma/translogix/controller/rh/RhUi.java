package ma.translogix.controller.rh;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.StatutConformite;
import ma.translogix.util.Formats;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;
import ma.translogix.util.ValidationException;

/**
 * UI helpers of the RH screens. Cell ideas from Majda's RhCells (two-line cell, coloured text, status badge);
 * dialog pattern identical to the comptable module (to be merged into one shared helper later).
 */
final class RhUi {

    /** Message of every Iteration 3 action (leaves, payroll, recruitment, export). */
    static final String PAS_ENCORE_LIVRE = "Module pas encore livré (itération 3).";

    /** A dialog form: saves through the services and throws ValidationException when a rule is broken. */
    interface Formulaire {
        void enregistrer();

        String titre();
    }

    private RhUi() {
    }

    /** The Gérant sees the RH pages read-only (add / edit / deactivate / renew hidden). */
    static boolean lectureSeule() {
        return Session.estGerant();
    }

    static void masquer(Node... noeuds) {
        for (Node n : noeuds) {
            n.setVisible(false);
            n.setManaged(false);
        }
    }

    static void pasEncoreLivre() {
        Dialogues.info("Pas encore livré", PAS_ENCORE_LIVRE);
    }

    /**
     * Opens a dialog form (rh/xxx-dialog.fxml) with Annuler / Confirmer.
     * A ValidationException is shown and keeps the dialog open so the user can fix the field.
     * @return true when the form was saved
     */
    static <C extends Formulaire> boolean ouvrirFormulaire(Node proprietaire, String fxml, Consumer<C> preparer) {
        FXMLLoader loader = new FXMLLoader(RhUi.class.getResource(Navigator.RACINE + fxml));
        DialogPane panneau;
        try {
            panneau = loader.load();
        } catch (IOException e) {
            Dialogues.erreur("Impossible d'ouvrir le formulaire : " + e.getMessage());
            return false;
        }
        C formulaire = loader.getController();
        preparer.accept(formulaire);

        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmer = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        panneau.getButtonTypes().setAll(annuler, confirmer);
        panneau.lookupButton(annuler).getStyleClass().add("btn-secondary");
        Button boutonConfirmer = (Button) panneau.lookupButton(confirmer);
        boutonConfirmer.getStyleClass().add("btn-primary");
        boutonConfirmer.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                formulaire.enregistrer();
            } catch (ValidationException ex) {
                Dialogues.erreur(ex.getMessage());
                e.consume();
            }
        });

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setDialogPane(panneau);
        dialogue.setTitle(formulaire.titre());
        if (proprietaire.getScene() != null) {
            dialogue.initOwner(proprietaire.getScene().getWindow());
        }
        return dialogue.showAndWait().orElse(annuler) == confirmer;
    }

    /** dd/MM/yyyy in a DatePicker, like everywhere else in the app. */
    static void formatDate(DatePicker champ) {
        champ.setPromptText("jj/mm/aaaa");
        champ.setConverter(new StringConverter<>() {
            @Override
            public String toString(java.time.LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public java.time.LocalDate fromString(String texte) {
                return texte == null || texte.isBlank() ? null : Formats.lireDate(texte);
            }
        });
    }

    /**
     * The date of a DatePicker, including text typed but not yet validated with Enter.
     * Falls back to the picker's value when the editor has no text (value set by code before the dialog shows).
     * @return null when the field is empty
     * @throws ValidationException when the typed text is not a dd/MM/yyyy date
     */
    static java.time.LocalDate lireDate(DatePicker champ) {
        String texte = champ.getEditor().getText();
        if (texte == null || texte.isBlank()) {
            return champ.getValue();
        }
        return Formats.lireDate(texte);
    }

    /** Badge class for a compliance status. */
    static String classeStatut(StatutConformite statut) {
        return switch (statut) {
            case EXPIRE -> "badge-red";
            case A_RENOUVELER -> "badge-amber";
            case VALIDE -> "badge-green";
        };
    }

    /** Badge class for a displayed compliance label ("Conforme", "À renouveler", "Expiré", "Non renseigné"). */
    static String classeBadge(String libelle) {
        for (StatutConformite s : StatutConformite.values()) {
            if (s.getLibelle().equals(libelle)) {
                return classeStatut(s);
            }
        }
        return "badge-gray"; // Non renseigné
    }

    /** Text colour class for a compliance status (Majda's coloured-text cells). */
    static String classeTexte(StatutConformite statut) {
        return switch (statut) {
            case EXPIRE -> "cell-red";
            case A_RENOUVELER -> "cell-amber";
            case VALIDE -> "cell-green";
        };
    }

    /** Plain text cell with one of the table text classes (cell-strong, cell-normal). */
    static <S> TableCell<S, String> texte(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : item);
                getStyleClass().removeAll("cell-strong", "cell-normal");
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    /** Majda's coloured-text cell: the row decides the colour class (cell-green, cell-amber, cell-red, cell-orange). */
    static <S> TableCell<S, String> texteColore(Function<S, String> classePour) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                getStyleClass().removeAll("cell-green", "cell-amber", "cell-red", "cell-orange", "cell-normal");
                S ligne = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || item == null || ligne == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add(classePour.apply(ligne));
            }
        };
    }

    /** Majda's two-line cell: bold main line, small grey second line (name + matricule, CIN + CNSS). */
    static <S> TableCell<S, S> doubleLigne(Function<S, String> principale, Function<S, String> secondaire) {
        return new TableCell<>() {
            private final Label titre = new Label();
            private final Label sousTitre = new Label();
            private final VBox boite = new VBox(2, titre, sousTitre);

            {
                titre.getStyleClass().add("cell-strong");
                sousTitre.getStyleClass().add("cell-sub");
            }

            @Override
            protected void updateItem(S ligne, boolean empty) {
                super.updateItem(ligne, empty);
                setText(null);
                if (empty || ligne == null) {
                    setGraphic(null);
                    return;
                }
                titre.setText(principale.apply(ligne));
                sousTitre.setText(secondaire.apply(ligne));
                setGraphic(boite);
            }
        };
    }

    /** Status pill ("● Conforme"); the cell value is the label, classePour gives the badge class. */
    static <S> TableCell<S, String> badge(Function<String, String> classePour) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + item);
                badge.setMinWidth(Region.USE_PREF_SIZE);
                badge.getStyleClass().add(classePour.apply(item));
                setGraphic(badge);
            }
        };
    }

    /** Rounded clip so the tinted circle of a KPI card stays inside it (same as the admin dashboard). */
    static void arrondir(Region carte) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(carte.widthProperty());
        clip.heightProperty().bind(carte.heightProperty());
        carte.setClip(clip);
    }

    /** "—" for an empty value. */
    static String tiret(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }
}
