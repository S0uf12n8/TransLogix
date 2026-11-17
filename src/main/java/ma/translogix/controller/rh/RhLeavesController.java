package ma.translogix.controller.rh;

import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/** Validation des demandes de conge et suivi des soldes. */
public class RhLeavesController {

    /** Ligne mutable: le statut change quand le RH accorde ou refuse la demande. */
    public static final class Demande {

        private final String employe;
        private final String type;
        private final String periode;
        private final String jours;
        private final StringProperty statut = new SimpleStringProperty();
        private final StringProperty ton = new SimpleStringProperty();
        private final SimpleBooleanProperty traite = new SimpleBooleanProperty(false);
        private boolean approuve;

        Demande(String employe, String type, String periode, String jours, String statut, String ton) {
            this.employe = employe;
            this.type = type;
            this.periode = periode;
            this.jours = jours;
            this.statut.set(statut);
            this.ton.set(ton);
        }

        public String getEmploye() { return employe; }
        public String getType() { return type; }
        public String getPeriode() { return periode; }
        public String getJours() { return jours; }
        public StringProperty statutProperty() { return statut; }
        public StringProperty tonProperty() { return ton; }
        public String getTon() { return ton.get(); }
        public SimpleBooleanProperty traiteProperty() { return traite; }
        public boolean isTraite() { return traite.get(); }
        public boolean isApprouve() { return approuve; }

        void traiter(boolean accorde) {
            approuve = accorde;
            statut.set(accorde ? "Accord\u00e9" : "Refus\u00e9");
            ton.set(accorde ? "green" : "red");
            traite.set(true);
        }
    }

    @FXML private TableView<Demande> table;
    @FXML private TableColumn<Demande, String> colEmploye;
    @FXML private TableColumn<Demande, String> colType;
    @FXML private TableColumn<Demande, String> colPeriode;
    @FXML private TableColumn<Demande, String> colJours;
    @FXML private TableColumn<Demande, String> colStatut;
    @FXML private TableColumn<Demande, String> colAction;

    @FXML
    private void initialize() {
        colEmploye.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmploye()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colPeriode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPeriode()));
        colJours.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJours()));
        colStatut.setCellValueFactory(c -> c.getValue().statutProperty());
        colStatut.setCellFactory(RhCells.statut(Demande::getTon));
        colAction.setCellValueFactory(c -> c.getValue().traiteProperty().asString());
        colAction.setCellFactory(colonne -> new TableCell<>() {
            @Override
            protected void updateItem(String valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Demande demande = getTableView().getItems().get(getIndex());
                if (demande.isTraite()) {
                    Label resultat = new Label(demande.isApprouve() ? "Valid\u00e9 RH" : "Rejet\u00e9 RH");
                    resultat.getStyleClass().add("rh-card-hint");
                    setGraphic(resultat);
                } else {
                    Button accorder = new Button("Accorder");
                    accorder.getStyleClass().add("rh-btn-primary");
                    accorder.setOnAction(e -> traiter(demande, true));
                    Button refuser = new Button("Refuser");
                    refuser.getStyleClass().add("rh-btn-ghost-danger");
                    refuser.setOnAction(e -> traiter(demande, false));
                    setGraphic(new HBox(8, accorder, refuser));
                }
                setText(null);
            }
        });

        table.setItems(FXCollections.observableArrayList(
                new Demande("Youssef Amrani", "Cong\u00e9 Pay\u00e9 Annuel", "25/10/2026 \u2192 02/11/2026",
                        "8 jours", "Approuv\u00e9", "green"),
                new Demande("Karim El Idrissi", "Cong\u00e9 Exceptionnel (Mariage)", "05/11/2026 \u2192 09/11/2026",
                        "4 jours", "En attente", "amber"),
                new Demande("Abderrahim El Amrani", "Maladie (Justifi\u00e9)", "28/10/2026 \u2192 30/10/2026",
                        "2 jours", "En attente", "amber")));
    }

    private void traiter(Demande demande, boolean accorde) {
        demande.traiter(accorde);
        table.refresh();
        RhNotify.toast(accorde ? "Demande de cong\u00e9 approuv\u00e9e." : "Demande de cong\u00e9 refus\u00e9e.");
    }

    @FXML
    private void nouvelleDemande() {
        RhFormDialog.ouvrir(null, "Saisie de Demande de Cong\u00e9",
                "Gestion RH",
                "Enregistrez une absence ou un cong\u00e9 pay\u00e9.",
                List.of(
                        RhFormDialog.Champ.texte("Salari\u00e9 concern\u00e9", "employee", "Ex: Mohammed Bennani"),
                        RhFormDialog.Champ.choix("Type de cong\u00e9", "type", List.of(
                                "Cong\u00e9 Pay\u00e9 Annuel", "Maladie / Accident",
                                "\u00c9v\u00e9nement Familial", "Absence Injustifi\u00e9e")),
                        RhFormDialog.Champ.date("Date de d\u00e9but", "start"),
                        RhFormDialog.Champ.date("Date de fin", "end")))
                .ifPresent(valeurs -> RhNotify.toast("Demande de cong\u00e9 enregistr\u00e9e pour "
                        + valeurs.get("employee") + "."));
    }
}
