package ma.translogix.chauffeur.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.util.Ui;

/** Coquille de l'application : barre latérale, en-tête, navigation entre les 12 sections, toast et modales. */
public class MainController {
    @FXML private StackPane root, modalOverlay, modalHost, toastLayer, sectionHost;
    @FXML private HBox toast;
    @FXML private Label toastLabel, pageKicker, pageTitle, currentDate, navNotifBadge, driverExpenseBadge;
    @FXML private Circle notifDot;
    @FXML private ScrollPane contentScroll;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navHome, navMissions, navExpenses, navProfile, navIncidents, navNotifications,
            navMissionDetail, navPod, navInspection, navChat, navExceptional, navHistory;
    // racines des sections incluses (fx:include)
    @FXML private Node dashboard, missions, expenses, profile, incidents, notifications, missionDetail, pod,
            inspection, chat, exceptional, history;
    // contrôleurs des sections incluses
    @FXML private ExpensesController expensesController;
    @FXML private IncidentsController incidentsController;
    @FXML private MissionDetailController missionDetailController;
    @FXML private ChatController chatController;

    private final Map<String, Node> sections = new HashMap<>();
    private final Map<String, ToggleButton> navButtons = new HashMap<>();
    private final Map<String, String[]> pageNames = new HashMap<>();
    private final PauseTransition toastTimer = new PauseTransition(Duration.millis(3500));

    @FXML
    private void initialize() {
        AppContext ctx = AppContext.get();
        ctx.register(this);
        ctx.register(expensesController);
        ctx.register(incidentsController);
        ctx.register(missionDetailController);
        ctx.register(chatController);

        String[][] defs = {
            {"driver-home", "COCKPIT CHAUFFEUR", "Tableau de bord"},
            {"driver-missions", "CARNET DE ROUTE", "Mes Ordres de Transport"},
            {"driver-expenses", "FRAIS DE ROUTE & JUSTIFICATIFS", "Saisie des Reçus & Carburant"},
            {"driver-profile", "DOSSIER ADMINISTRATIF", "Permis & Documents"},
            {"driver-incidents", "URGENCE & ASSISTANCE ROUTIÈRE", "Incidents & Support"},
            {"driver-notifications", "CENTRE DE MESSAGES", "Notifications"},
            {"driver-mission-detail", "FICHE MISSION", "Détail & Suivi de mission"},
            {"driver-pod", "LIVRAISON", "Preuve de livraison"},
            {"driver-inspection", "SÉCURITÉ VÉHICULE", "Inspection du camion"},
            {"driver-chat", "COMMUNICATION", "Messagerie Dispatching"},
            {"driver-exceptional", "IMPOSSIBILITÉ JUSTIFIÉE", "Intervention exceptionnelle"},
            {"driver-history", "ARCHIVES", "Historique des missions"}};
        Node[] nodes = {dashboard, missions, expenses, profile, incidents, notifications, missionDetail, pod, inspection, chat, exceptional, history};
        ToggleButton[] btns = {navHome, navMissions, navExpenses, navProfile, navIncidents, navNotifications, navMissionDetail,
                navPod, navInspection, navChat, navExceptional, navHistory};
        for (int i = 0; i < defs.length; i++) {
            sections.put(defs[i][0], nodes[i]);
            navButtons.put(defs[i][0], btns[i]);
            pageNames.put(defs[i][0], new String[]{defs[i][1], defs[i][2]});
        }
        sections.values().forEach(n -> Ui.setVisibleManaged(n, false));
        Ui.setVisibleManaged(dashboard, true);

        currentDate.setText(Ui.todayFull());
        toastTimer.setOnFinished(e -> toast.setVisible(false));

        // modale : largeur min(92 %, 480 px), hauteur max = fenêtre - 48 px
        modalHost.prefWidthProperty().bind(Bindings.min(480, modalOverlay.widthProperty().multiply(0.92)));
        modalHost.maxHeightProperty().bind(modalOverlay.heightProperty().subtract(48));
        modalOverlay.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) closeModal(); });

        setUnread((int) ctx.notifications.stream().filter(n -> !n.read).count());
    }

    // ------------------------------------------------------------------ navigation
    @FXML
    private void onNavClick(ActionEvent e) {
        Object id = ((Node) e.getSource()).getUserData();
        if (id != null) switchTab(id.toString());
    }

    @FXML private void openNotificationsTab() { switchTab("driver-notifications"); }

    public void switchTab(String tabId) {
        Node target = sections.get(tabId);
        if (target == null) return;
        sections.values().forEach(n -> Ui.setVisibleManaged(n, false));
        Ui.setVisibleManaged(target, true);
        navButtons.get(tabId).setSelected(true);
        pageKicker.setText(pageNames.get(tabId)[0]);
        pageTitle.setText(pageNames.get(tabId)[1]);
        contentScroll.setVvalue(0);

        // animation « fade » d'origine : opacité .5 → 1 et translation 3px → 0 en 180 ms
        FadeTransition ft = new FadeTransition(Duration.millis(180), target);
        ft.setFromValue(0.5); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), target);
        tt.setFromY(3); tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        if ("driver-chat".equals(tabId)) chatController.scrollChat();
    }

    // ------------------------------------------------------------------ toast
    public void showToast(String message) {
        toastLabel.setText(message);
        toast.setVisible(true);
        toastTimer.stop();
        toastTimer.playFromStart();
    }

    // ------------------------------------------------------------------ modales
    @FXML public void openExpenseModal() { openModal("/fxml/modals/ExpenseModal.fxml"); }
    public void openIncidentModal() { openModal("/fxml/modals/IncidentModal.fxml"); }

    private void openModal(String fxml) {
        try {
            Region content = FXMLLoader.load(MainController.class.getResource(fxml));
            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.getStyleClass().add("modal-scroll");
            sp.prefHeightProperty().bind(content.heightProperty());
            Ui.clipRounded(sp, 16);
            modalHost.getChildren().setAll(sp);
            modalOverlay.setVisible(true);
            modalOverlay.requestFocus();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de charger " + fxml, ex);
        }
    }

    public void closeModal() {
        modalOverlay.setVisible(false);
        modalHost.getChildren().clear();
    }

    // ------------------------------------------------------------------ badges notifications
    public void setUnread(int unread) {
        notifDot.setVisible(unread != 0);
        navNotifBadge.setText(String.valueOf(unread));
        Ui.setVisibleManaged(navNotifBadge, unread != 0);
    }

    // ------------------------------------------------------------------ actions de l'en-tête / barre latérale
    @FXML
    private void exportDriverReport() {
        AppContext ctx = AppContext.get();
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
        List<String[]> lignes = new ArrayList<>();
        lignes.add(new String[]{"Date", "Mission", "Itineraire", "Kilometres", "Frais Generes MAD"});
        for (var mission : ctx.missions()) {
            lignes.add(new String[]{
                    mission.getDateDebut() == null ? "—" : date.format(mission.getDateDebut()),
                    mission.getReference(),
                    mission.getItineraire(),
                    "—", // kilométrage non porté par Mission
                    "—"  // frais non rattachés à la mission dans le service
            });
        }
        String[][] rows = lignes.toArray(new String[0][]);
        StringBuilder csv = new StringBuilder("\uFEFF");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) csv.append("\r\n");
            for (int j = 0; j < rows[i].length; j++) {
                if (j > 0) csv.append(';');
                csv.append('"').append(rows[i][j]).append('"');
            }
        }
        FileChooser fc = new FileChooser();
        String nom = ctx.chauffeur() == null ? "chauffeur" : ctx.chauffeur().getNom();
        fc.setInitialFileName("releve-chauffeur-" + (nom == null ? "chauffeur" : nom.toLowerCase(Locale.ROOT))
                .replaceAll("[^a-z0-9]+", "-") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File f = fc.showSaveDialog(root.getScene().getWindow());
        if (f == null) return;
        try {
            Files.write(f.toPath(), csv.toString().getBytes(StandardCharsets.UTF_8));
            showToast("Relevé chauffeur CSV téléchargé.");
        } catch (IOException ex) {
            showToast("Échec de l'export du relevé : " + ex.getMessage());
        }
    }

    @FXML
    private void logout() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Marquer la fin de service chauffeur ?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        a.setTitle("Espace Chauffeur | Translogix");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) showToast("Session chauffeur clôturée.");
    }
}
