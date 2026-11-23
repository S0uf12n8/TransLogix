package ma.translogix.controller.comptable;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;
import ma.translogix.service.ClientService;
import ma.translogix.service.FactureService;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** "Facturation clients": searchable invoice register; new invoice and payment in dialogs. No rules here. */
public class FacturesController {

    @FXML private Button boutonNouvelle;
    @FXML private TextField recherche;
    @FXML private TableView<Facture> table;
    @FXML private TableColumn<Facture, String> colNumero;
    @FXML private TableColumn<Facture, String> colClient;
    @FXML private TableColumn<Facture, String> colEmission;
    @FXML private TableColumn<Facture, String> colEcheance;
    @FXML private TableColumn<Facture, String> colHt;
    @FXML private TableColumn<Facture, String> colTva;
    @FXML private TableColumn<Facture, String> colTtc;
    @FXML private TableColumn<Facture, String> colStatut;
    @FXML private TableColumn<Facture, Facture> colActions;

    private final FactureService factures = AppContext.factureService();
    private final ClientService clients = AppContext.clientService();
    private final ObservableList<Facture> donnees = FXCollections.observableArrayList();
    private final Map<Long, String> nomsClients = new HashMap<>();

    @FXML
    private void initialize() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNumero.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colClient.setCellValueFactory(c -> new SimpleStringProperty(client(c.getValue())));
        colClient.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEmission.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEmission())));
        colEmission.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEcheance.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEcheance())));
        colEcheance.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colHt.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantHT())));
        colHt.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTva.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantTva())));
        colTva.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTtc.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantTtc())));
        colTtc.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(factures.libelleStatut(c.getValue())));
        colStatut.setCellFactory(col -> ComptableUi.badge(ComptableUi::classeStatut));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button regler = new Button("Régler");
            private final Button annuler = new Button("Annuler");
            private final HBox boutons = new HBox(6, regler, annuler);

            {
                regler.getStyleClass().add("link-orange");
                annuler.getStyleClass().add("link-red");
                for (Button b : new Button[] {regler, annuler}) {
                    b.setMinWidth(Region.USE_PREF_SIZE); // never "Ré..." or "Ann..."
                }
                regler.setOnAction(e -> regler(getItem()));
                annuler.setOnAction(e -> annuler(getItem()));
            }

            @Override
            protected void updateItem(Facture f, boolean empty) {
                super.updateItem(f, empty);
                setText(null);
                // Only an unpaid invoice can be paid or cancelled; the Gérant's view is read-only.
                boolean modifiable = !empty && f != null && f.getStatut() == StatutFacture.EN_ATTENTE
                        && !ComptableUi.lectureSeule();
                regler.setVisible(modifiable);
                regler.setManaged(modifiable);
                annuler.setVisible(modifiable);
                annuler.setManaged(modifiable);
                setGraphic(!empty && modifiable ? boutons : null);
            }
        });
        table.setRowFactory(tv -> ligneAvecMenu());

        FilteredList<Facture> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(f -> Recherche.correspond(requete, f.getNumero(), client(f),
                        f.getReferenceTrajet(), Formats.montant(f.getMontantHT()),
                        Formats.montant(f.getMontantTtc()), factures.libelleStatut(f))));
        SortedList<Facture> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonNouvelle);
        }
        rafraichir();
    }

    /**
     * One context menu per row, built once. The actions are recomputed when the menu opens because the invoice
     * status may have changed since the row was created. Double-click shows the full invoice detail.
     */
    private TableRow<Facture> ligneAvecMenu() {
        TableRow<Facture> ligne = new TableRow<>();
        MenuItem details = new MenuItem("Voir détails");
        MenuItem regler = new MenuItem("Enregistrer un règlement…");
        MenuItem annuler = new MenuItem("Annuler la facture…");
        annuler.getStyleClass().add("menu-danger");
        details.setOnAction(e -> voirDetails(ligne.getItem()));
        regler.setOnAction(e -> regler(ligne.getItem()));
        annuler.setOnAction(e -> annuler(ligne.getItem()));
        ContextMenu menu = new ContextMenu(details, regler, new SeparatorMenuItem(), annuler);

        menu.setOnShowing(e -> {
            Facture f = ligne.getItem();
            boolean modifiable = f != null && f.getStatut() == StatutFacture.EN_ATTENTE
                    && !ComptableUi.lectureSeule();
            regler.setVisible(modifiable);
            annuler.setVisible(modifiable);
            menu.getItems().get(2).setVisible(modifiable); // separator before "Annuler"
        });
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        ligne.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !ligne.isEmpty()) {
                voirDetails(ligne.getItem());
            }
        });
        return ligne;
    }

    /** Full invoice information, including its recorded payments (read-only). */
    private void voirDetails(Facture f) {
        if (f == null) {
            return;
        }
        StringBuilder texte = new StringBuilder()
                .append("Facture ").append(f.getNumero()).append('\n')
                .append("Client : ").append(client(f)).append('\n')
                .append("Mission : ").append(f.getReferenceTrajet() == null ? "—" : f.getReferenceTrajet()).append('\n')
                .append("Émission : ").append(Formats.date(f.getDateEmission())).append('\n')
                .append("Échéance : ").append(Formats.date(f.getDateEcheance())).append('\n')
                .append("Montant HT : ").append(Formats.mad(f.getMontantHT())).append('\n')
                .append("TVA : ").append(Formats.mad(f.getMontantTva())).append('\n')
                .append("Montant TTC : ").append(Formats.mad(f.getMontantTtc())).append('\n')
                .append("Statut : ").append(factures.libelleStatut(f));
        var paiements = factures.paiementsDe(f.getId());
        texte.append("\n\nRèglements enregistrés : ");
        if (paiements.isEmpty()) {
            texte.append("aucun");
        } else {
            paiements.forEach(p -> texte.append("\n• ").append(Formats.date(p.getDate()))
                    .append(" · ").append(p.getMode().getLibelle())
                    .append(" · ").append(p.getReference()));
        }
        Dialogues.info("Détail de la facture " + f.getNumero(), texte.toString());
    }

    /** Asks for confirmation before the irreversible payment recording, then opens the payment dialog. */
    private void regler(Facture facture) {
        if (facture == null || ComptableUi.lectureSeule()) {
            return;
        }
        if (!Dialogues.confirmer("Enregistrer un règlement",
                "Vous allez enregistrer le règlement de la facture " + facture.getNumero() + " ("
                        + Formats.mad(facture.getMontantTtc()) + ").\nCette opération est définitive. Continuer ?")) {
            return;
        }
        if (ComptableUi.<PaiementFormulaireController>ouvrirFormulaire(table,
                PaiementFormulaireController.FXML, f -> f.preparer(facture, client(facture)))) {
            rafraichir();
        }
    }

    /** Cancels an unpaid invoice after explicit confirmation; the invoice stays in the history. */
    private void annuler(Facture facture) {
        if (facture == null || ComptableUi.lectureSeule()) {
            return;
        }
        if (!Dialogues.confirmer("Annuler la facture",
                "Annuler la facture " + facture.getNumero() + " ?\n"
                        + "Elle sera conservée dans l'historique mais ne pourra plus être réglée.")) {
            return;
        }
        try {
            factures.annuler(facture.getId());
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    @FXML
    private void nouvelle() {
        if (ComptableUi.<FactureFormulaireController>ouvrirFormulaire(table, FactureFormulaireController.FXML,
                FactureFormulaireController::preparer)) {
            rafraichir();
        }
    }

    private void rafraichir() {
        nomsClients.clear();
        clients.lister().forEach(c -> nomsClients.put(c.getId(), c.getNom()));
        donnees.setAll(factures.lister());
    }

    private String client(Facture f) {
        return nomsClients.getOrDefault(f.getClientId(), "—");
    }
}
