package ma.translogix.controller.comptable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.Client;
import ma.translogix.model.Mission;
import ma.translogix.service.FactureService;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * Fields of the "Créer une facture" dialog, with a live VAT / TTC preview. Rules stay in FactureService.
 * The invoice is for a delivered mission without invoice ([C3]); the client is the mission's.
 */
public class FactureFormulaireController implements ComptableUi.Formulaire {
    static final String FXML = "comptable/facture-dialog.fxml";

    /** One option of "Conditions de règlement": its label and payment delay in days. */
    record Condition(String libelle, int jours) {
        @Override
        public String toString() {
            return libelle;
        }
    }

    private static final List<Condition> CONDITIONS = List.of(
            new Condition("Paiement à 30 jours", 30),
            new Condition("Paiement à 60 jours", 60),
            new Condition("Comptant à la livraison", 0));

    @FXML private Label titre;
    @FXML private ComboBox<Mission> champMission;
    @FXML private Label clientMission;
    @FXML private TextField champMontant;
    @FXML private ComboBox<Condition> champConditions;
    @FXML private Label apercuTva;
    @FXML private Label apercuTtc;
    @FXML private Label apercuEcheance;

    private final FactureService factures = AppContext.factureService();

    @FXML
    private void initialize() {
        champMission.setItems(FXCollections.observableArrayList(factures.missionsAFacturer()));
        champMission.setCellFactory(liste -> new CelluleMission());
        champMission.setButtonCell(new CelluleMission());
        champConditions.setItems(FXCollections.observableArrayList(CONDITIONS));
        champConditions.setValue(CONDITIONS.get(0));
        champMission.valueProperty().addListener((obs, ancien, mission) -> {
            Client client = mission == null ? null : client(mission);
            clientMission.setText("Client : " + (client == null ? "—" : client.getNom()));
            preselectionnerCondition(client);
        });
        champMontant.textProperty().addListener((obs, ancien, texte) -> mettreAJourApercu());
        champConditions.valueProperty().addListener((obs, ancien, condition) -> mettreAJourApercu());
    }

    /** Called by ComptableUi before the dialog opens. Nothing to prefill for a new invoice. */
    void preparer() {
        if (champMission.getItems().isEmpty()) {
            champMission.setPromptText("Aucune mission livrée à facturer");
        }
        mettreAJourApercu();
    }

    private static Client client(Mission mission) {
        return AppContext.clientService().lister().stream()
                .filter(c -> c.getId().equals(mission.getClientId())).findFirst().orElse(null);
    }

    /** "TRJ-9082 · Atlas Prime Transports · Agadir → Marrakech". */
    private static String libelle(Mission mission) {
        Client client = client(mission);
        return mission.getReference() + " · " + (client == null ? "?" : client.getNom()) + " · "
                + mission.getItineraire();
    }

    private static final class CelluleMission extends ListCell<Mission> {
        @Override
        protected void updateItem(Mission mission, boolean empty) {
            super.updateItem(mission, empty);
            setText(empty || mission == null ? null : libelle(mission));
        }
    }

    /** The client's own delay is pre-selected; a delay outside 30/60/0 gets its own option. */
    private void preselectionnerCondition(Client client) {
        if (client == null) {
            return;
        }
        List<Condition> options = new ArrayList<>(CONDITIONS);
        Condition duClient = options.stream().filter(c -> c.jours() == client.getDelaiPaiementJours())
                .findFirst().orElse(null);
        if (duClient == null) {
            duClient = new Condition("Délai du client : " + client.getDelaiPaiementJours() + " jours",
                    client.getDelaiPaiementJours());
            options.add(0, duClient);
        }
        champConditions.setItems(FXCollections.observableArrayList(options));
        champConditions.setValue(duClient);
    }

    private void mettreAJourApercu() {
        BigDecimal ht = lireMontant(champMontant.getText());
        boolean valide = ht != null && ht.signum() > 0;
        apercuTva.setText(valide ? Formats.mad(FactureService.calculerTva(ht)) : "—");
        apercuTtc.setText(valide ? Formats.mad(FactureService.calculerTtc(ht)) : "—");
        Condition condition = champConditions.getValue();
        apercuEcheance.setText(condition == null ? "—"
                : Formats.date(LocalDate.now().plusDays(condition.jours())));
    }

    @Override
    public void enregistrer() {
        Mission mission = champMission.getValue();
        BigDecimal ht = lireMontant(champMontant.getText());
        if (ht == null) {
            throw new ValidationException("Montant HT invalide (exemple : 20000 ou 20 000,50).");
        }
        Condition condition = champConditions.getValue();
        factures.creer(mission == null ? null : mission.getId(), ht, condition == null ? null : condition.jours());
    }

    @Override
    public String titre() {
        return titre.getText();
    }

    /** Accepts "20000", "20 000,50" or "20000.50"; null when the text is not a number. */
    static BigDecimal lireMontant(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String normalise = texte.replace(" ", "").replace(" ", "").replace(" ", "").replace(',', '.');
        try {
            return new BigDecimal(normalise);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
