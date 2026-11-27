package ma.translogix.controller.dispatcher;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.dao.DonneesException;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.util.Formats;
import ma.translogix.util.MessagesErreur;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;
import ma.translogix.util.ValidationException;

/**
 * UI helpers of the Dispatcher screens, following the RhUi pattern (two-line cell, badge, link buttons,
 * dialog with Annuler / Confirmer). To be merged with RhUi / ComptableUi into one shared helper later.
 */
final class DispatcherUi {

    /** Message of every Iteration 3 action (incidents, fuel vouchers, export). */
    static final String PAS_ENCORE_LIVRE = "Module pas encore livré (itération 3).";

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    /** A dialog form: saves through the services and throws ValidationException when a rule is broken. */
    interface Formulaire {
        void enregistrer();

        String titre();
    }

    /** One link button of an ACTIONS cell. */
    record Lien(String texte, String styleClass, Runnable action) {
    }

    private DispatcherUi() {
    }

    /** The Gérant sees the dispatcher pages read-only (create / edit / status / cancel hidden). */
    static boolean lectureSeule() {
        return Session.estGerant();
    }

    static void masquer(Node... noeuds) {
        for (Node n : noeuds) {
            n.setVisible(false);
            n.setManaged(false);
        }
    }

    /** Chart nodes are created lazily by JavaFX: run the action as soon as the node exists. */
    static void quandAffiche(ReadOnlyObjectProperty<Node> propriete, Consumer<Node> action) {
        if (propriete.get() != null) {
            action.accept(propriete.get());
            return;
        }
        propriete.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Node> obs, Node ancien, Node noeud) {
                if (noeud != null) {
                    propriete.removeListener(this);
                    action.accept(noeud);
                }
            }
        });
    }

    static void pasEncoreLivre() {
        Dialogues.info("Pas encore livré", PAS_ENCORE_LIVRE);
    }

    /**
     * Opens a dialog form (dispatcher/xxx-dialog.fxml) with Annuler / Confirmer.
     * A broken rule or a database error is shown as a clear French message and keeps the dialog open.
     * @return true when the form was saved
     */
    static <C extends Formulaire> boolean ouvrirFormulaire(Node proprietaire, String fxml, Consumer<C> preparer) {
        FXMLLoader loader = new FXMLLoader(DispatcherUi.class.getResource(Navigator.RACINE + fxml));
        DialogPane panneau;
        try {
            panneau = loader.load();
        } catch (IOException e) {
            // FXMLLoader wraps a failure of the form's initialize() in an IOException; keep the DB message.
            String message = MessagesErreur.pourUtilisateur(e);
            Dialogues.erreur(MessagesErreur.MESSAGE_INATTENDU.equals(message)
                    ? "Impossible d'ouvrir le formulaire : " + e.getMessage() : message);
            return false;
        } catch (DonneesException e) {
            Dialogues.erreur(MessagesErreur.pourUtilisateur(e));
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
            } catch (ValidationException | DonneesException ex) {
                Dialogues.erreur(MessagesErreur.pourUtilisateur(ex));
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

    /**
     * Runs a Dispatcher service call. A broken rule (ValidationException) or a database problem
     * (DonneesException) is shown as a clear French message; no SQL and no stack trace reach the screen.
     * @return true when the call worked
     */
    static boolean executer(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ValidationException | DonneesException e) {
            Dialogues.erreur(MessagesErreur.pourUtilisateur(e));
            return false;
        }
    }

    /**
     * Same handling for a service read: on failure, show the message once and return a safe empty value
     * so the screen can stay coherent instead of showing stale or faked rows.
     */
    static <T> T charger(Supplier<T> lecture, T valeurVide) {
        try {
            return lecture.get();
        } catch (ValidationException | DonneesException e) {
            Dialogues.erreur(MessagesErreur.pourUtilisateur(e));
            return valeurVide;
        }
    }

    /** dd/MM/yyyy in a DatePicker, like everywhere else in the app. */
    static void formatDate(DatePicker champ) {
        champ.setPromptText("jj/mm/aaaa");
        champ.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String texte) {
                return texte == null || texte.isBlank() ? null : Formats.lireDate(texte);
            }
        });
    }

    /**
     * Date (DatePicker, typed text included) + time (HH:mm text field) of a dialog.
     * @param quoi "de départ" / "d'arrivée", used in the error messages
     * @return null when both fields are empty
     * @throws ValidationException when the date or the time is not valid
     */
    static LocalDateTime lireDateHeure(DatePicker date, TextField heure, String quoi) {
        String texteDate = date.getEditor().getText();
        LocalDate jour = texteDate == null || texteDate.isBlank() ? date.getValue() : Formats.lireDate(texteDate);
        String texteHeure = heure.getText() == null ? "" : heure.getText().trim();
        if (jour == null && texteHeure.isEmpty()) {
            return null;
        }
        if (jour == null) {
            throw new ValidationException("La date " + quoi + " est obligatoire (format jj/mm/aaaa).");
        }
        if (texteHeure.isEmpty()) {
            throw new ValidationException("L'heure " + quoi + " est obligatoire (format HH:mm).");
        }
        try {
            return jour.atTime(LocalTime.parse(texteHeure.length() == 4 ? "0" + texteHeure : texteHeure, HEURE));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Heure " + quoi + " invalide : « " + texteHeure + " » (format HH:mm).");
        }
    }

    static String heure(LocalDateTime moment) {
        return HEURE.format(moment);
    }

    /** "Aujourd'hui 18:30", "Demain 09:00", "Hier 16:15", otherwise "20/11/2026 09:00". */
    static String momentRelatif(LocalDateTime moment, LocalDate aujourdhui) {
        LocalDate jour = moment.toLocalDate();
        String prefixe;
        if (jour.equals(aujourdhui)) {
            prefixe = "Aujourd'hui";
        } else if (jour.equals(aujourdhui.plusDays(1))) {
            prefixe = "Demain";
        } else if (jour.equals(aujourdhui.minusDays(1))) {
            prefixe = "Hier";
        } else {
            prefixe = Formats.date(jour);
        }
        return prefixe + " " + heure(moment);
    }

    /** Badge colours of dispatcher.html: planned blue, in progress amber, delivered green, invoiced purple. */
    static String classeStatut(StatutMission statut) {
        return switch (statut) {
            case PLANIFIEE -> "badge-blue";
            case EN_COURS -> "badge-amber";
            case LIVREE -> "badge-green";
            case FACTUREE -> "badge-purple";
            case ANNULEE -> "badge-gray";
        };
    }

    static String classeStatut(StatutVehicule statut) {
        return switch (statut) {
            case EN_SERVICE -> "badge-green";
            case MAINTENANCE -> "badge-amber";
            case HORS_SERVICE -> "badge-red";
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

    /** Two-line cell: bold main line, small grey second line (driver + truck, route + client). */
    static <S> TableCell<S, S> doubleLigne(Function<S, String> principale, Function<S, String> secondaire) {
        return new TableCell<>() {
            private final Label titre = new Label();
            private final Label sousTitre = new Label();
            private final VBox boite = new VBox(2, titre, sousTitre);

            {
                titre.getStyleClass().add("cell-strong");
                sousTitre.getStyleClass().add("cell-sub");
                boite.setAlignment(Pos.CENTER_LEFT);
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

    /** Status pill ("● En cours"); the row gives the label and the badge class. */
    static <S> TableCell<S, S> badge(Function<S, String> libelle, Function<S, String> classe) {
        return new TableCell<>() {
            @Override
            protected void updateItem(S ligne, boolean empty) {
                super.updateItem(ligne, empty);
                setText(null);
                if (empty || ligne == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + libelle.apply(ligne));
                badge.setMinWidth(Region.USE_PREF_SIZE);
                badge.getStyleClass().add(classe.apply(ligne));
                setGraphic(badge);
            }
        };
    }

    /** ACTIONS cell: the link buttons the row allows (none in the Gérant's read-only view). */
    static <S> TableCell<S, S> liens(Function<S, List<Lien>> liensPour) {
        return new TableCell<>() {
            private final HBox boite = new HBox(12);

            {
                boite.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(S ligne, boolean empty) {
                super.updateItem(ligne, empty);
                setText(null);
                if (empty || ligne == null || lectureSeule()) {
                    setGraphic(null);
                    return;
                }
                boite.getChildren().clear();
                for (Lien l : liensPour.apply(ligne)) {
                    Button b = new Button(l.texte());
                    b.getStyleClass().add(l.styleClass());
                    b.setMinWidth(Region.USE_PREF_SIZE);
                    b.setOnAction(e -> l.action().run());
                    boite.getChildren().add(b);
                }
                setGraphic(boite.getChildren().isEmpty() ? null : boite);
            }
        };
    }

    /**
     * A tonnage typed as "24" or "24,5".
     * @return null when the field is empty (the service then gives its own message)
     * @throws ValidationException when the text is not a number
     */
    static java.math.BigDecimal lireTonnes(TextField champ, String quoi) {
        String texte = champ.getText() == null ? "" : champ.getText().trim().replace(" ", "").replace(',', '.');
        if (texte.isEmpty()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(texte);
        } catch (NumberFormatException e) {
            throw new ValidationException(quoi + " invalide : « " + champ.getText().trim() + " » (ex : 24 ou 24,5).");
        }
    }

    /** "24 t" / "24,5 t". */
    static String tonnes(java.math.BigDecimal valeur) {
        return valeur == null ? "—" : valeur.stripTrailingZeros().toPlainString().replace('.', ',') + " t";
    }
}
