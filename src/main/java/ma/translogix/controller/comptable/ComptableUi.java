package ma.translogix.controller.comptable;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;
import ma.translogix.util.ValidationException;

/** Small UI helpers shared by the comptable screens (same look as the admin screens). */
final class ComptableUi {

    /** A dialog form: saves through the services and throws ValidationException when a rule is broken. */
    interface Formulaire {
        void enregistrer();

        String titre();
    }

    private ComptableUi() {
    }

    /** The Gérant sees the comptable pages read-only (create / edit / pay buttons hidden). */
    static boolean lectureSeule() {
        return Session.estGerant();
    }

    static void masquer(Node... noeuds) {
        for (Node n : noeuds) {
            n.setVisible(false);
            n.setManaged(false);
        }
    }

    /**
     * Opens a dialog form (comptable/xxx-dialog.fxml) with Annuler / Confirmer.
     * A ValidationException is shown and keeps the dialog open so the user can fix the field.
     * @return true when the form was saved
     */
    static <C extends Formulaire> boolean ouvrirFormulaire(Node proprietaire, String fxml, Consumer<C> preparer) {
        FXMLLoader loader = new FXMLLoader(ComptableUi.class.getResource(Navigator.RACINE + fxml));
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

    /** Badge class for a displayed invoice status (see FactureService.libelleStatut). */
    static String classeStatut(String libelle) {
        return switch (libelle) {
            case "Payée" -> "badge-green";
            case "En retard" -> "badge-red";
            case "Annulée" -> "badge-gray";
            default -> "badge-blue"; // En attente
        };
    }

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
                badge.setMinWidth(Region.USE_PREF_SIZE); // a badge is never cut ("30 j...")
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
}
