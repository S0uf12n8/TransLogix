package ma.translogix.controller.comptable;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Employe;
import ma.translogix.model.Paie;
import ma.translogix.model.StatutPaie;
import ma.translogix.service.PaieService;
import ma.translogix.service.PaieService.CriterePaie;
import ma.translogix.service.PaieService.SynthesePaie;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * "Paiement des employés": real employee register, entered salaries and computed net amounts.
 * One in-memory service is shared by this page and its dialogs; AppContext stays untouched.
 */
public class PaieController {

    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MM/uuuu", Locale.FRENCH);

    /** Shared by the page, the creation dialog and the payment dialog. */
    private static final PaieService SERVICE = AppContext.paieService();

    static PaieService service() {
        return SERVICE;
    }

    @FXML private Label valeurNet;
    @FXML private Label valeurPaye;
    @FXML private Label valeurImpaye;
    @FXML private Label valeurEmployes;
    @FXML private Label piedNet;
    @FXML private Label piedPaye;
    @FXML private Label piedImpaye;
    @FXML private Label piedEmployes;
    @FXML private Label resumeTable;
    @FXML private Button boutonCreer;
    @FXML private Button boutonValider;
    @FXML private Button boutonPayer;
    @FXML private TextField recherche;
    @FXML private ComboBox<YearMonth> filtrePeriode;
    @FXML private ComboBox<StatutPaie> filtreStatut;
    @FXML private ComboBox<Employe> filtreEmploye;
    @FXML private TableView<Paie> table;
    @FXML private TableColumn<Paie, String> colNumero;
    @FXML private TableColumn<Paie, String> colEmploye;
    @FXML private TableColumn<Paie, String> colPeriode;
    @FXML private TableColumn<Paie, String> colBase;
    @FXML private TableColumn<Paie, String> colPrimes;
    @FXML private TableColumn<Paie, String> colRetenues;
    @FXML private TableColumn<Paie, String> colNet;
    @FXML private TableColumn<Paie, String> colStatut;
    @FXML private TableColumn<Paie, String> colPaiement;

    private final Map<Long, String> nomsEmployes = new HashMap<>();

    @FXML
    private void initialize() {
        configurerColonnes();
        configurerFiltres();
        table.setRowFactory(tv -> ligneAvecMenu());
        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonCreer, boutonValider, boutonPayer);
        }
        rafraichir();
    }

    private void configurerColonnes() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNumero.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colEmploye.setCellValueFactory(c -> new SimpleStringProperty(
                nomsEmployes.getOrDefault(c.getValue().getEmployeId(), "—")));
        colEmploye.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colPeriode.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPeriode() == null ? "—" : MOIS.format(c.getValue().getPeriode())));
        colPeriode.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getSalaireBase())));
        colBase.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colPrimes.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getPrimes())));
        colPrimes.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colRetenues.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getDeductions())));
        colRetenues.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colNet.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getNet())));
        colNet.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatut() == null ? "—" : c.getValue().getStatut().getLibelle()));
        colStatut.setCellFactory(col -> ComptableUi.badge(PaieController::classeStatut));
        colPaiement.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDatePaiement() == null ? "—" : Formats.date(c.getValue().getDatePaiement())));
        colPaiement.setCellFactory(col -> ComptableUi.texte("cell-normal"));
    }

    private void configurerFiltres() {
        filtrePeriode.setItems(FXCollections.observableArrayList(SERVICE.lister().stream()
                .map(Paie::getPeriode).filter(java.util.Objects::nonNull).distinct().toList()));
        filtrePeriode.setPromptText("Tous les mois");
        filtreStatut.setItems(FXCollections.observableArrayList(StatutPaie.values()));
        filtreStatut.setPromptText("Tous les statuts");
        filtreEmploye.setItems(FXCollections.observableArrayList(SERVICE.employesActifs()));
        filtreEmploye.setPromptText("Tous les employés");

        recherche.textProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtrePeriode.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtreStatut.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtreEmploye.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtreEmploye.setCellFactory(liste -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Employe employe, boolean empty) {
                super.updateItem(employe, empty);
                setText(empty || employe == null ? null : employe.getNomComplet());
            }
        });
        filtreEmploye.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Employe employe, boolean empty) {
                super.updateItem(employe, empty);
                setText(empty || employe == null ? null : employe.getNomComplet());
            }
        });
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtrePeriode.setValue(null);
        filtreStatut.setValue(null);
        filtreEmploye.setValue(null);
        rafraichir();
    }

    @FXML
    private void rafraichir() {
        YearMonth periodeChoisie = filtrePeriode.getValue();
        filtrePeriode.setItems(FXCollections.observableArrayList(SERVICE.lister().stream()
                .map(Paie::getPeriode).filter(java.util.Objects::nonNull).distinct().toList()));
        if (periodeChoisie != null) {
            filtrePeriode.setValue(periodeChoisie);
        }
        nomsEmployes.clear();
        for (Employe employe : AppContext.employeService().lister()) {
            nomsEmployes.put(employe.getId(), employe.getNomComplet());
        }
        CriterePaie critere = new CriterePaie(recherche.getText(), filtrePeriode.getValue(),
                filtreStatut.getValue(), filtreEmploye.getValue() == null ? null : filtreEmploye.getValue().getId());
        try {
            List<Paie> fiches = SERVICE.rechercher(critere);
            table.setItems(FXCollections.observableArrayList(fiches));
            afficherSynthese(SERVICE.synthese(fiches));
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void afficherSynthese(SynthesePaie s) {
        valeurNet.setText(Formats.montant(s.totalNet()));
        valeurPaye.setText(Formats.montant(s.totalPaye()));
        valeurImpaye.setText(Formats.montant(s.totalImpaye()));
        valeurEmployes.setText(Integer.toString(s.nombreEmployes()));
        piedNet.setText(s.nombreFiches() + (s.nombreFiches() > 1 ? " fiches affichées" : " fiche affichée"));
        piedPaye.setText(s.nombrePayees() + (s.nombrePayees() > 1 ? " salaires réglés" : " salaire réglé"));
        piedImpaye.setText("en attente ou à régler");
        piedEmployes.setText("employés concernés");
        resumeTable.setText(s.nombreFiches() + (s.nombreFiches() > 1 ? " fiches · " : " fiche · ")
                + Formats.mad(s.totalNet()) + " net");
    }

    private static String classeStatut(String libelle) {
        return switch (libelle) {
            case "Payée" -> "badge-green";
            case "En attente" -> "badge-amber";
            default -> "badge-blue"; // Validée
        };
    }

    @FXML
    private void creer() {
        if (ComptableUi.lectureSeule()) {
            return;
        }
        if (ComptableUi.<PaieFormulaireController>ouvrirFormulaire(table, PaieFormulaireController.FXML,
                PaieFormulaireController::preparer)) {
            rafraichir();
            Dialogues.info("Fiche créée", "La fiche de paie a été enregistrée en attente de validation.");
        }
    }

    @FXML
    private void valider() {
        Paie paie = selection();
        if (paie == null || ComptableUi.lectureSeule()) {
            return;
        }
        if (!Dialogues.confirmer("Valider la fiche de paie",
                "Valider la fiche " + paie.getNumero() + " pour un net de " + Formats.mad(paie.getNet()) + " ?")) {
            return;
        }
        try {
            SERVICE.valider(paie.getId());
            rafraichir();
            Dialogues.info("Fiche validée", "La fiche " + paie.getNumero() + " peut maintenant être réglée.");
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    @FXML
    private void payer() {
        Paie paie = selection();
        if (paie == null || ComptableUi.lectureSeule()) {
            return;
        }
        if (!Dialogues.confirmer("Enregistrer le paiement",
                "Enregistrer le règlement du salaire de " + Formats.mad(paie.getNet())
                        + " pour la fiche " + paie.getNumero() + " ?")) {
            return;
        }
        if (ComptableUi.<PaiePaiementFormulaireController>ouvrirFormulaire(table,
                PaiePaiementFormulaireController.FXML, f -> f.preparer(paie))) {
            rafraichir();
        }
    }

    @FXML
    private void afficherDetails() {
        Paie paie = selection();
        if (paie == null) {
            return;
        }
        String employe = nomsEmployes.getOrDefault(paie.getEmployeId(), "—");
        String detail = "Fiche : " + paie.getNumero()
                + "\nEmployé : " + employe
                + "\nMois : " + (paie.getPeriode() == null ? "—" : MOIS.format(paie.getPeriode()))
                + "\nSalaire de base : " + Formats.mad(paie.getSalaireBase())
                + "\nPrimes : " + Formats.mad(paie.getPrimes())
                + "\nRetenues : " + Formats.mad(paie.getDeductions())
                + "\nNet à payer : " + Formats.mad(paie.getNet())
                + "\nStatut : " + paie.getStatut().getLibelle()
                + (paie.getDatePaiement() == null ? ""
                        : "\nPaiement : " + Formats.date(paie.getDatePaiement())
                                + " · " + (paie.getModePaiement() == null ? "—" : paie.getModePaiement().getLibelle())
                                + " · " + (paie.getReference() == null ? "—" : paie.getReference()));
        Dialogues.info("Détail de la fiche de paie", detail);
    }

    private Paie selection() {
        Paie paie = table.getSelectionModel().getSelectedItem();
        if (paie == null) {
            Dialogues.info("Aucune sélection", "Choisissez d'abord une fiche de paie dans le tableau.");
        }
        return paie;
    }

    private TableRow<Paie> ligneAvecMenu() {
        TableRow<Paie> ligne = new TableRow<>();
        MenuItem details = new MenuItem("Voir le détail");
        MenuItem valider = new MenuItem("Valider la fiche");
        MenuItem payer = new MenuItem("Enregistrer le paiement");
        details.setOnAction(e -> afficherDetails(ligne.getItem()));
        valider.setOnAction(e -> valider(ligne.getItem()));
        payer.setOnAction(e -> payer(ligne.getItem()));
        ContextMenu menu = new ContextMenu(details, valider, payer);
        menu.setOnShowing(e -> {
            valider.setVisible(!ComptableUi.lectureSeule());
            payer.setVisible(!ComptableUi.lectureSeule());
            Paie paie = ligne.getItem();
            valider.setDisable(paie == null || paie.getStatut() != StatutPaie.EN_ATTENTE);
            payer.setDisable(paie == null || paie.getStatut() != StatutPaie.VALIDEE);
        });
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        return ligne;
    }

    private void afficherDetails(Paie paie) {
        if (paie == null) {
            return;
        }
        table.getSelectionModel().select(paie);
        afficherDetails();
    }

    private void valider(Paie paie) {
        if (paie == null) {
            return;
        }
        table.getSelectionModel().select(paie);
        valider();
    }

    private void payer(Paie paie) {
        if (paie == null) {
            return;
        }
        table.getSelectionModel().select(paie);
        payer();
    }
}
