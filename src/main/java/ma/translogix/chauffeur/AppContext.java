package ma.translogix.chauffeur;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import ma.translogix.chauffeur.controller.ChatController;
import ma.translogix.chauffeur.controller.ExpensesController;
import ma.translogix.chauffeur.controller.IncidentsController;
import ma.translogix.chauffeur.controller.MissionDetailController;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.model.Notification;
import ma.translogix.chauffeur.model.Rows.ExpenseRow;
import ma.translogix.chauffeur.model.Rows.IncidentRow;
import ma.translogix.chauffeur.util.Ui;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Client;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.StatutMission;
import ma.translogix.model.Vehicule;
import ma.translogix.service.EspaceChauffeurService;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * Bridge between the 12 driver FXML sections and the main TransLogix application.
 * They run inside the main shell only (the standalone "zip" window was removed).
 *
 * The zip version kept every value here as hardcoded demo data. This version keeps the
 * same public API but reads and writes through the main service layer
 * (EspaceChauffeurService -> MissionService -> DAO). Switching the DAOs from InMemory to
 * JDBC will make these screens follow without touching a controller.
 */
public final class AppContext {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    private static final Map<String, String> PAGES = Map.ofEntries(
            Map.entry("driver-home", "chauffeur/dashboard.fxml"),
            Map.entry("driver-missions", "chauffeur/missions.fxml"),
            Map.entry("driver-expenses", "chauffeur/expenses.fxml"),
            Map.entry("driver-profile", "chauffeur/profile.fxml"),
            Map.entry("driver-incidents", "chauffeur/incidents.fxml"),
            Map.entry("driver-notifications", "chauffeur/notifications.fxml"),
            Map.entry("driver-mission-detail", "chauffeur/mission-detail.fxml"),
            Map.entry("driver-pod", "chauffeur/pod.fxml"),
            Map.entry("driver-inspection", "chauffeur/inspection.fxml"),
            Map.entry("driver-chat", "chauffeur/chat.fxml"),
            Map.entry("driver-exceptional", "chauffeur/exceptional.fxml"),
            Map.entry("driver-history", "chauffeur/history.fxml"));

    public static final String[] STEP_LABELS = {"Ordre accepté", "Chargement terminé", "Départ du site de chargement",
            "Arrivée au site de déchargement", "Déchargement terminé", "Preuve de livraison transmise"};

    private static final AppContext INSTANCE = new AppContext();

    public static AppContext get() {
        // The bridge is often created before login (FXML preview). Refresh on first access
        // after a session change so every screen sees the current driver's data.
        INSTANCE.ensureSynced();
        return INSTANCE;
    }

    private void ensureSynced() {
        missions();
    }

    static {
        loadFont("/fonts/fa-solid-900.ttf");
        loadFont("/fonts/fa-regular-400.ttf");
    }

    // Shared observable state used by the sections.
    public final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    public final Map<String, MissionData> missionData = new LinkedHashMap<>();
    /** Receipts and incidents have no service in the main app yet: they stay session-local. */
    public final ObservableList<ExpenseRow> expenseRows = FXCollections.observableArrayList();
    public final ObservableList<IncidentRow> incidentRows = FXCollections.observableArrayList();

    /** Mission ref currently shown by the detail / POD screens. */
    public String selectedMissionRef;

    private final Map<String, int[]> progression = new LinkedHashMap<>();
    private final Map<String, Mission> missionsByRef = new LinkedHashMap<>();
    private Long loadedForUserId;
    private int notifSeq = 1000;

    // Registered page controllers (the shell loads each page once per visit).
    private ExpensesController expenses;
    private IncidentsController incidents;
    private MissionDetailController missionDetail;
    private ChatController chat;
    private Stage modalStage;

    private AppContext() {
        reload();
    }

    // ------------------------------------------------------------------ service bridge

    private static EspaceChauffeurService espace() {
        return ma.translogix.AppContext.espaceChauffeurService();
    }

    /** The employee behind the logged-in account. */
    public Employe chauffeur() {
        return espace().chauffeur();
    }

    /** Missions of the logged-in driver, straight from the main service. */
    public List<Mission> missions() {
        Long userId = currentUserId();
        if (!Objects.equals(userId, loadedForUserId) || missionData.isEmpty()) {
            reload();
        }
        return List.copyOf(missionsByRef.values());
    }

    private static Long currentUserId() {
        try {
            ma.translogix.model.Utilisateur u = Session.utilisateur();
            return u == null ? null : u.getId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<Mission> fetchMissions() {
        try {
            return espace().mesMissions();
        } catch (RuntimeException e) {
            // No logged-in account yet (e.g. FXML preview): show an empty driver space instead of crashing.
            return List.of();
        }
    }

    public Optional<Mission> mission(String ref) {
        return Optional.ofNullable(missionsByRef.get(ref));
    }

    public Optional<Client> client(Long id) {
        return id == null ? Optional.empty()
                : ma.translogix.AppContext.clientService().lister().stream()
                        .filter(c -> id.equals(c.getId())).findFirst();
    }

    public Optional<Vehicule> vehicule(Long id) {
        return id == null ? Optional.empty()
                : ma.translogix.AppContext.vehiculeService().lister().stream()
                        .filter(v -> id.equals(v.getId())).findFirst();
    }

    public String clientName(Mission m) {
        return client(m.getClientId()).map(Client::getNom).orElse("—");
    }

    /** Re-read missions, clients, vehicles and compliance from the main services. */
    public void reload() {
        loadedForUserId = currentUserId();
        List<Mission> missions = fetchMissions();
        Map<String, MissionData> nouveau = new LinkedHashMap<>();
        missionsByRef.clear();
        for (Mission m : missions) {
            String ref = m.getReference();
            missionsByRef.put(ref, m);
            Client client = client(m.getClientId()).orElse(null);
            Vehicule vehicule = vehicule(m.getVehiculeId()).orElse(null);

            int[] progress = progression.computeIfAbsent(ref, k -> new int[]{progressionInitial(m.getStatut())});
            MissionData d = new MissionData(
                    ref,
                    m.getVilleDepart(), m.getVilleArrivee(),
                    libelleStatut(m.getStatut()), classeStatut(m.getStatut()),
                    client == null ? "—" : client.getNom(),
                    client == null || client.getTelephone() == null ? "—" : client.getTelephone(),
                    m.getVilleDepart(), "Site de chargement " + nvl(m.getVilleDepart()),
                    m.getVilleArrivee(), "Site de livraison " + nvl(m.getVilleArrivee()),
                    m.getDateDebut() == null ? Ui.todayShort() : DATE.format(m.getDateDebut()),
                    m.getDateDebut() == null ? Ui.nowTime() : HEURE.format(m.getDateDebut()),
                    m.getDateFin() == null ? "—" : HEURE.format(m.getDateFin()),
                    m.getPoidsTonnes() == null ? "" : m.getPoidsTonnes().stripTrailingZeros().toPlainString() + " T",
                    nvl(m.getCargaison()),
                    progress[0],
                    instructions(m, client, vehicule));
            d.missionId = m.getId();
            d.vehicle = vehicule == null ? "—"
                    : nvl(vehicule.getMarque()) + " " + nvl(vehicule.getModele()) + " · " + vehicule.getMatricule();
            nouveau.put(ref, d);
        }
        missionData.clear();
        missionData.putAll(nouveau);
        if (selectedMissionRef == null || !missionData.containsKey(selectedMissionRef)) {
            selectedMissionRef = missionData.keySet().stream().findFirst().orElse(null);
        }
        rebuildNotifications(missions);
    }

    public void demarrer(String ref) {
        Mission m = missionsByRef.get(ref);
        if (m != null) {
            espace().demarrer(m.getId());
            reload();
        }
    }

    public void livrer(String ref) {
        Mission m = missionsByRef.get(ref);
        if (m != null) {
            espace().livrer(m.getId());
            reload();
        }
    }

    /** Step progress is a view concern; the real status lives in MissionService. */
    public void avancerEtape(String ref) {
        int[] p = progression.get(ref);
        if (p != null && p[0] < STEP_LABELS.length) {
            p[0]++;
            reload();
        }
    }

    // ------------------------------------------------------------------ navigation / UI

    public void register(ExpensesController c) { this.expenses = c; }
    public void register(IncidentsController c) { this.incidents = c; }
    public void register(MissionDetailController c) { this.missionDetail = c; }
    public void register(ChatController c) { this.chat = c; }

    public ExpensesController expenses() { return expenses; }
    public IncidentsController incidents() { return incidents; }

    /** Session-local receipt, visible even if the receipts page has not been opened yet. */
    public void addReceipt(String tripRef, String type, String station, String amount) {
        expenseRows.add(0, new ExpenseRow("REC-" + System.currentTimeMillis(), Ui.firstToken(tripRef), type,
                station, amount + " MAD", "En attente", "amber"));
    }

    public void addIncident(String tripRef, String type, String severity, String location, String attachmentName) {
        String cls = switch (severity) {
            case "Faible" -> "green";
            case "Moyenne" -> "blue";
            case "Élevée" -> "amber";
            case "Critique" -> "red";
            default -> "gray";
        };
        incidentRows.add(0, new IncidentRow("INC-" + System.currentTimeMillis(), Ui.firstToken(tripRef), type,
                severity, cls, location, attachmentName == null ? "—" : attachmentName, "Transmis"));
        addNotification("alert", "Signalement transmis",
                type + " · gravité " + severity + ". Le Dispatching est notifié.", null, null, null);
    }

    public void navigate(String tabId) {
        String page = PAGES.get(tabId);
        if (page == null) {
            return;
        }
        Navigator.ouvrir(page);
    }

    public void toast(String message) {
        Platform.runLater(() -> {
            try {
                Label label = new Label(message);
                label.setWrapText(true);
                label.setStyle("-fx-background-color:#172033; -fx-text-fill:white; -fx-padding:10 14; "
                        + "-fx-background-radius:8; -fx-font-size:12px;");
                StackPane pane = new StackPane(label);
                pane.setStyle("-fx-padding:8;");
                Stage stage = new Stage(StageStyle.TRANSPARENT);
                stage.setScene(new Scene(pane, Color.TRANSPARENT));
                stage.setResizable(false);
                Window owner = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                if (owner != null) {
                    stage.initOwner(owner);
                    stage.setX(owner.getX() + owner.getWidth() - 380);
                    stage.setY(owner.getY() + owner.getHeight() - 130);
                }
                stage.show();
                PauseTransition timer = new PauseTransition(Duration.seconds(3.5));
                timer.setOnFinished(e -> stage.close());
                timer.play();
            } catch (RuntimeException ex) {
                Dialogues.info("Espace chauffeur", message);
            }
        });
    }

    public void openExpenseModal() { openModal("/fxml/modals/ExpenseModal.fxml"); }
    public void openIncidentModal() { openModal("/fxml/modals/IncidentModal.fxml"); }

    private void openModal(String ressource) {
        try {
            closeModal();
            Parent contenu = FXMLLoader.load(getClass().getResource(ressource));
            modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Espace Chauffeur | TransLogix");
            modalStage.setScene(new Scene(contenu));
            modalStage.setResizable(false);
            modalStage.show();
        } catch (IOException e) {
            Dialogues.erreur("Impossible d'ouvrir " + ressource + "\n" + e.getMessage());
        }
    }

    public void closeModal() {
        if (modalStage != null) {
            modalStage.close();
            modalStage = null;
        }
    }

    public void scrollChat() {
        if (chat != null) {
            chat.scrollChat();
        }
    }

    public void openMissionDetail(String ref) {
        if (ref == null || !missionData.containsKey(ref)) {
            toast("Détail non disponible pour cette mission.");
            return;
        }
        selectedMissionRef = ref;
        if (missionDetail != null) {
            missionDetail.renderMissionDetail(ref);
        }
        navigate("driver-mission-detail");
    }

    // ------------------------------------------------------------------ notifications

    public void addNotification(String type, String title, String text, String actionLabel, String ref, String tab) {
        notifications.add(0, new Notification(++notifSeq, type, title, text, "À l'instant", false,
                actionLabel, ref, tab));
    }

    /** The shell has no unread badge for the driver menu yet: nothing to update. */
    public void updateNotifBadges(int unread) {
        // kept so NotificationsController needs no change
    }

    private void rebuildNotifications(List<Mission> missions) {
        List<Notification> serviceNotifs = new ArrayList<>();
        int serviceSeq = 1; // ids below 1000: session notifications use 1001+ and survive a reload
        for (Mission m : missions) {
            String ref = m.getReference();
            if (m.getStatut() == StatutMission.EN_COURS) {
                serviceNotifs.add(new Notification(serviceSeq++, "mission", "Mission en cours",
                        ref + " · " + m.getItineraire() + " · " + clientName(m) + ".", "En cours", false,
                        "Voir la mission", ref, null));
            } else if (m.getStatut() == StatutMission.PLANIFIEE) {
                serviceNotifs.add(new Notification(serviceSeq++, "instruction", "Nouvelle mission assignée",
                        ref + " · " + m.getItineraire() + ". Consultez la fiche et confirmez la prise en compte.",
                        "À venir", false, "Voir la mission", ref, null));
            } else if (m.getStatut() == StatutMission.LIVREE || m.getStatut() == StatutMission.FACTUREE) {
                serviceNotifs.add(new Notification(serviceSeq++, "mission", "Livraison enregistrée",
                        ref + " · " + m.getItineraire() + ".", "Terminé", true,
                        "Voir la mission", ref, null));
            }
        }
        try {
            Employe driver = chauffeur();
            for (DocumentConformite d : ma.translogix.AppContext.conformiteService().documentsDe(driver.getId())) {
                StatutConformite statut = ma.translogix.AppContext.conformiteService().statut(d);
                if (statut == StatutConformite.VALIDE) {
                    continue;
                }
                serviceNotifs.add(new Notification(serviceSeq++, "alert", "Document à renouveler",
                        d.getType().getLibelle() + " · " + statut
                                + (d.getDateExpiration() == null ? "" : " · échéance " + DATE.format(d.getDateExpiration())),
                        "Conformité", false, "Voir mes documents", null, "driver-profile"));
            }
        } catch (RuntimeException ignored) {
            // No employee linked to this account: the missions list already explains it.
        }
        List<Notification> session = notifications.stream().filter(n -> n.id > 1000).toList();
        notifications.setAll(serviceNotifs);
        notifications.addAll(0, session);
    }

    // ------------------------------------------------------------------ helpers

    private static String libelleStatut(StatutMission s) {
        return switch (s) {
            case PLANIFIEE -> "Planifié";
            case EN_COURS -> "En cours";
            case LIVREE, FACTUREE -> "Terminé";
            case ANNULEE -> "Annulée";
        };
    }

    private static String classeStatut(StatutMission s) {
        return switch (s) {
            case PLANIFIEE -> "amber";
            case EN_COURS -> "blue";
            case LIVREE, FACTUREE -> "green";
            case ANNULEE -> "gray";
        };
    }

    private static int progressionInitial(StatutMission s) {
        return switch (s) {
            case EN_COURS -> 3;
            case LIVREE, FACTUREE -> STEP_LABELS.length;
            default -> 0;
        };
    }

    private static String[] instructions(Mission m, Client client, Vehicule vehicule) {
        List<String> liste = new ArrayList<>();
        if (m.getCargaison() != null && !m.getCargaison().isBlank()) {
            liste.add("Cargaison : " + m.getCargaison()
                    + (m.getPoidsTonnes() == null ? "" : " (" + m.getPoidsTonnes() + " T)."));
        }
        liste.add("Client : " + (client == null ? "—" : client.getNom()) + ". Présenter la lettre de voiture signée.");
        if (vehicule != null) {
            liste.add("Véhicule affecté : " + nvl(vehicule.getMarque()) + " " + nvl(vehicule.getModele())
                    + " (" + vehicule.getMatricule() + ").");
        }
        liste.add("Respecter les temps de conduite et de repos, et signaler tout incident au Dispatching.");
        return liste.toArray(new String[0]);
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    private static void loadFont(String path) {
        try (InputStream in = AppContext.class.getResourceAsStream(path)) {
            if (in != null) {
                Font.loadFont(in, 16);
            }
        } catch (IOException ignored) {
            // The UI still works without the icon font.
        }
    }
}
