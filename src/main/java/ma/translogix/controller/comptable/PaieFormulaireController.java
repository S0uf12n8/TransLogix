package ma.translogix.controller.comptable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import ma.translogix.model.Employe;

/** Fields of the "Créer une fiche de paie" dialog. Net is only a preview; the service computes and stores it. */
public class PaieFormulaireController implements ComptableUi.Formulaire {
    static final String FXML = "comptable/paie-dialog.fxml";

    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MM/uuuu", Locale.FRENCH);

    @FXML private Label titre;
    @FXML private ComboBox<Employe> champEmploye;
    @FXML private ComboBox<YearMonth> champPeriode;
    @FXML private TextField champSalaireBase;
    @FXML private TextField champPrimes;
    @FXML private TextField champRetenues;
    @FXML private Label apercuNet;

    @FXML
    private void initialize() {
        champEmploye.setItems(FXCollections.observableArrayList(PaieController.service().employesActifs()));
        champEmploye.setCellFactory(liste -> new CelluleEmploye());
        champEmploye.setButtonCell(new CelluleEmploye());

        YearMonth courant = YearMonth.now();
        List<YearMonth> mois = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            mois.add(courant.minusMonths(i));
        }
        champPeriode.setItems(FXCollections.observableArrayList(mois));
        champPeriode.setValue(courant);
        champPeriode.setCellFactory(liste -> new CelluleMois());
        champPeriode.setButtonCell(new CelluleMois());

        champSalaireBase.textProperty().addListener((obs, ancien, valeur) -> mettreAJourApercu());
        champPrimes.textProperty().addListener((obs, ancien, valeur) -> mettreAJourApercu());
        champRetenues.textProperty().addListener((obs, ancien, valeur) -> mettreAJourApercu());
        mettreAJourApercu();
    }

    void preparer() {
        if (champEmploye.getItems().isEmpty()) {
            champEmploye.setPromptText("Aucun employé actif");
        }
        mettreAJourApercu();
    }

    private void mettreAJourApercu() {
        BigDecimal base = lire(champSalaireBase.getText());
        BigDecimal primes = lire(champPrimes.getText());
        BigDecimal retenues = lire(champRetenues.getText());
        if (base == null || base.signum() <= 0) {
            apercuNet.setText("—");
            return;
        }
        BigDecimal net = base.add(primes == null ? BigDecimal.ZERO : primes)
                .subtract(retenues == null ? BigDecimal.ZERO : retenues);
        apercuNet.setText(net.signum() > 0 ? ma.translogix.util.Formats.mad(net) : "Net invalide");
    }

    @Override
    public void enregistrer() {
        Employe employe = champEmploye.getValue();
        PaieController.service().creer(employe == null ? null : employe.getId(), champPeriode.getValue(),
                lire(champSalaireBase.getText()), lire(champPrimes.getText()), lire(champRetenues.getText()));
    }

    @Override
    public String titre() {
        return titre.getText();
    }

    private static BigDecimal lire(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        return FactureFormulaireController.lireMontant(texte);
    }

    private static final class CelluleEmploye extends ListCell<Employe> {
        @Override
        protected void updateItem(Employe employe, boolean empty) {
            super.updateItem(employe, empty);
            setText(empty || employe == null ? null : employe.getMatricule() + " · " + employe.getNomComplet());
        }
    }

    private static final class CelluleMois extends ListCell<YearMonth> {
        @Override
        protected void updateItem(YearMonth mois, boolean empty) {
            super.updateItem(mois, empty);
            setText(empty || mois == null ? null : MOIS.format(mois));
        }
    }
}
