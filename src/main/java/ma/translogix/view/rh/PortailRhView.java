package ma.translogix.view.rh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Espace RH (Portail RH) : version JavaFX programmatique de rh.html.
 * Toute l'interface est construite en Java pur (pas de FXML) et stylée
 * via ma/translogix/css/rh.css.
 */
public class PortailRhView extends StackPane {
    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private static final Map<String, String[]> PAGE_NAMES = new LinkedHashMap<>();
    static {
        PAGE_NAMES.put("rh-dashboard", new String[]{"Vue Synthétique", "Tableau de bord RH"});
        PAGE_NAMES.put("staff-directory", new String[]{"Effectif & Registre du Personnel", "Chauffeurs & Employés"});
        PAGE_NAMES.put("compliance", new String[]{"Sécurité & Réglementation Transport", "Conformité & Permis"});
        PAGE_NAMES.put("leaves", new String[]{"Suivi du Temps de Travail", "Congés & Absences"});
        PAGE_NAMES.put("payroll", new String[]{"Rémunération & Primes Logistiques", "Paie & Prime Trajet"});
        PAGE_NAMES.put("recruitment", new String[]{"Pipeline RH & Intégration", "Recrutement & Onboarding"});
    }

    private final Map<String, HBox> navById = new LinkedHashMap<>();
    private final Map<String, Node> sections = new LinkedHashMap<>();
    private final VBox contentArea = new VBox(22);
    private final Label pageKicker = new Label();
    private final Label pageTitle = new Label();
    private final Label toast = new Label();
    private PauseTransition toastTimer;

    public PortailRhView() {
        getStyleClass().add("rh-root");
        getStylesheets().add(PortailRhView.class.getResource("/ma/translogix/css/rh.css").toExternalForm());

        HBox shell = new HBox();
        shell.setMaxWidth(Double.MAX_VALUE);
        shell.setMaxHeight(Double.MAX_VALUE);

        VBox sidebar = buildSidebar();
        VBox main = buildMain();
        HBox.setHgrow(main, Priority.ALWAYS);
        main.setMaxWidth(Double.MAX_VALUE);
        shell.getChildren().addAll(sidebar, main);

        toast.getStyleClass().add("toast");
        toast.setVisible(false);
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 24, 24, 0));

        getChildren().addAll(shell, toast);
        switchTab("rh-dashboard");
    }
    private VBox buildSidebar() {
        VBox side = new VBox();
        side.getStyleClass().add("rh-sidebar");
        side.setPrefWidth(258);
        side.setMinWidth(258);
        side.setMaxWidth(258);
        side.setMaxHeight(Double.MAX_VALUE);

        HBox brand = new HBox(12);
        brand.getStyleClass().add("sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(2,
                label("Portal RH", "sidebar-brand-title"),
                label("TRANSLOGIX · MAROC", "sidebar-brand-sub"));
        brand.getChildren().addAll(label("👥", "brand-logo"), titles);

        HBox userCard = new HBox(10);
        userCard.getStyleClass().add("sidebar-user-card");
        userCard.setAlignment(Pos.CENTER_LEFT);
        VBox userText = new VBox(2,
                label("Mme. Chraïbi", "user-name"),
                label("Responsable RH", "user-role"));
        HBox.setHgrow(userText, Priority.ALWAYS);
        Region online = new Region();
        online.getStyleClass().add("online-dot");
        userCard.getChildren().addAll(label("MC", "user-avatar"), userText, online);
        VBox userWrap = new VBox(userCard);
        userWrap.getStyleClass().add("sidebar-user-wrap");

        VBox nav = new VBox(4);
        nav.getStyleClass().add("sidebar-nav");
        nav.getChildren().add(label("Capital Humain", "eyebrow"));
        nav.getChildren().add(navItem("rh-dashboard", "📊", "Tableau de bord RH", null, null, true));
        nav.getChildren().add(navItem("staff-directory", "🪪", "Chauffeurs & Employés", null, null, false));
        nav.getChildren().add(navItem("compliance", "🛡️", "Conformité & Permis", "3", "nav-badge-red", false));
        nav.getChildren().add(navItem("leaves", "📅", "Congés & Absences", "2", "nav-badge-orange", false));
        nav.getChildren().add(navItem("payroll", "💰", "Paie & Prime Trajet", null, null, false));
        nav.getChildren().add(navItem("recruitment", "➕", "Recrutement & Onboarding", null, null, false));

        Region navSpacer = new Region();
        VBox.setVgrow(navSpacer, Priority.ALWAYS);

        VBox regBox = new VBox(6);
        regBox.getStyleClass().add("reg-box");
        regBox.getChildren().addAll(
                label("✅", "reg-icon"),
                label("Suivi Réglementaire", "reg-title"),
                label("Contrôles CNSS, permis C/EC et FIMC/FCO à jour pour l'ensemble de la flotte.", "reg-text"));
        regBox.setMaxWidth(Double.MAX_VALUE);
        VBox regWrap = new VBox(regBox);
        regWrap.setPadding(new Insets(8, 12, 12, 12));

        nav.getChildren().addAll(navSpacer, regWrap);
        VBox.setVgrow(nav, Priority.ALWAYS);

        HBox logout = new HBox(10);
        logout.getStyleClass().addAll("nav-item", "nav-logout");
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.getChildren().addAll(label("↪", "nav-icon"), label("Déconnexion", "nav-text"));
        logout.setOnMouseClicked(e -> {
            if (confirm("Déconnexion du portail RH ?")) {
                showToast("Session RH fermée.");
            }
        });

        VBox footer = new VBox(8, logout, label("Portal RH v2.0 · Démo", "sidebar-version"));
        footer.getStyleClass().add("sidebar-footer");

        side.getChildren().addAll(brand, userWrap, nav, footer);
        return side;
    }

    private HBox navItem(String tabId, String icon, String text, String badge, String badgeClass, boolean active) {
        HBox item = new HBox(10);
        item.getStyleClass().add("nav-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(label(icon, "nav-icon"), label(text, "nav-text"));
        if (badge != null) {
            item.getChildren().addAll(spacer(), label(badge, "nav-badge", badgeClass));
        }
        if (active) {
            item.getStyleClass().add("active");
        }
        item.setOnMouseClicked(e -> switchTab(tabId));
        navById.put(tabId, item);
        return item;
    }

    private void switchTab(String tabId) {
        for (HBox item : navById.values()) {
            item.getStyleClass().remove("active");
        }
        HBox item = navById.get(tabId);
        if (item != null && !item.getStyleClass().contains("active")) {
            item.getStyleClass().add("active");
        }
        Node section = sections.get(tabId);
        if (section != null) {
            contentArea.getChildren().setAll(section);
        }
        String[] names = PAGE_NAMES.get(tabId);
        if (names != null) {
            pageKicker.setText(names[0]);
            pageTitle.setText(names[1]);
        }
    }
    private VBox buildMain() {
        VBox main = new VBox();
        main.getStyleClass().add("main-area");
        main.setMaxWidth(Double.MAX_VALUE);
        main.setMaxHeight(Double.MAX_VALUE);

        pageKicker.getStyleClass().add("eyebrow");
        pageTitle.getStyleClass().add("page-title");
        VBox titleBox = new VBox(2, pageKicker, pageTitle);

        String dateText = LocalDate.now().format(DATE_FR);
        dateText = Character.toUpperCase(dateText.charAt(0)) + dateText.substring(1);
        VBox dateBox = new VBox(2,
                label(dateText, "topbar-date"),
                label("Espace Responsable RH", "topbar-sub"));
        dateBox.setAlignment(Pos.CENTER_RIGHT);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().addAll(
                dateBox,
                btnPrimary("＋ Ajouter un salarié", this::openEmployeeModal),
                btnSecondary("🖨 Imprimer", this::imprimer),
                iconButton("🔔", () -> showToast("Alertes RH : 2 permis à renouveler + 1 visite médicale.")));

        HBox topbar = new HBox(12);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getChildren().addAll(titleBox, spacer(), actions);

        contentArea.setFillWidth(true);
        VBox pageContainer = new VBox();
        pageContainer.setPadding(new Insets(24));
        pageContainer.getChildren().add(contentArea);

        Label footer = label("© 2026 Translogix ERP · Espace RH · Démonstration sans persistance serveur.", "footer");
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);
        pageContainer.getChildren().add(footer);

        ScrollPane scroll = new ScrollPane(pageContainer);
        scroll.getStyleClass().add("content-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        sections.put("rh-dashboard", dashboardSection());
        sections.put("staff-directory", staffSection());
        sections.put("compliance", complianceSection());
        sections.put("leaves", leavesSection());
        sections.put("payroll", payrollSection());
        sections.put("recruitment", recruitmentSection());

        main.getChildren().addAll(topbar, scroll);
        return main;
    }
    private VBox dashboardSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Gestion des ressources humaines",
                "Bienvenue, Mme. Chraïbi 👋",
                "Aperçu global de l'effectif, de la conformité et des demandes RH.",
                btnSecondary("📄 Exporter Rapport RH", this::exportHRReport)));

        HBox kpis = new HBox(16);
        Node[] cards = {
                kpiCard("Effectif Total", "42", "salariés", "👥", "orange", "＋ +3 recrutements ce mois", "foot-green", null),
                kpiCard("Chauffeurs Actifs", "28", "/ 30", "🪪", "blue", "2 chauffeurs en congé", "foot-muted", 0.93),
                kpiCard("Alertes Conformité", "3", "dossiers", "⚠️", "red", "⏱ 2 Permis + 1 Visite Médicale", "foot-red", null),
                kpiCard("Congés en Attente", "2", "demandes", "📅", "green", "⌛ À valider sous 48h", "foot-amber", null)
        };
        for (Node card : cards) {
            HBox.setHgrow(card, Priority.ALWAYS);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
            kpis.getChildren().add(card);
        }
        sec.getChildren().add(kpis);

        VBox tablePanel = panel();
        DataTable drivers = new DataTable(
                new String[]{"Employé", "Poste / Catégorie", "Permis / FIMC", "Visite Médicale", "Statut"},
                new double[]{24, 26, 20, 18, 12});
        drivers.addRow(label("Mohammed Bennani", "cell-strong"), label("Chauffeur Poids Lourds (EC)", "cell"),
                label("PRM-883920 (2028)", "cell"), label("Conforme (11/2027)", "cell-green"), status("En Mission", "green"));
        drivers.addRow(label("Abderrahim El Amrani", "cell-strong"), label("Chauffeur Routier (C)", "cell"),
                label("PRM-112938 (11/2026)", "cell"), label("Échéance 15 jours", "cell-amber"), status("Disponible", "blue"));
        drivers.addRow(label("Youssef Amrani", "cell-strong"), label("Chauffeur Super Lourds (EC)", "cell"),
                label("PRM-445201 (2027)", "cell"), label("Conforme (04/2027)", "cell-green"), status("En Congé", "amber"));
        tablePanel.getChildren().addAll(
                panelHeader("Chauffeurs & Conducteurs - Statut Rapide", "Derniers mouvements et états de service",
                        btnSecondary("Voir l'annuaire complet →", () -> switchTab("staff-directory"))),
                drivers);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        tablePanel.setMaxWidth(Double.MAX_VALUE);

        VBox quick = panel();
        quick.setSpacing(12);
        quick.setMinWidth(330);
        quick.setPrefWidth(330);
        quick.setMaxWidth(330);
        HBox quickHead = new HBox(10, label("Action Rapide RH", "panel-title"), spacer(), countBadge("Aujourd'hui", null));
        quickHead.setAlignment(Pos.CENTER_LEFT);
        quick.getChildren().addAll(quickHead,
                quickAction("📅", "Saisir une demande de congé", "Saisie rapide pour un salarié.", "orange", this::openLeaveModal),
                quickAction("🪪", "Renouvellement Permis / FIMC", "Mettre à jour une visite médicale ou carte FCO.", "blue", () -> openRenewLicenseModal("")),
                quickAction("🧮", "Calculateur Prime Route", "Générer les indemnités de déplacement.", "purple", () -> switchTab("payroll")));

        HBox row = new HBox(18, tablePanel, quick);
        sec.getChildren().add(row);
        return sec;
    }
    private VBox staffSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Effectif & Registre du Personnel",
                "Chauffeurs & Employés",
                "Annuaire complet, contrats, matricules CNSS et affectations.",
                btnPrimary("➕ Ajouter un employé", this::openEmployeeModal)));

        HBox stats = new HBox(16,
                statPanel("Chauffeurs PL / SPL", "30", null),
                statPanel("Personnel Administratif & Dépôt", "12", "text-blue"),
                statPanel("Contrats CDI / CDD", "38 CDI / 4 CDD", "text-green"));
        for (Node n : stats.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        sec.getChildren().add(stats);

        VBox tablePanel = panel();
        TextField search = searchField("Rechercher un salarié...");
        DataTable t = new DataTable(
                new String[]{"Matricule & Nom", "CIN & CNSS", "Poste", "Permis / Catégorie", "Embauche", "Statut", "Action"},
                new double[]{20, 17, 16, 18, 12, 9, 8});
        search.textProperty().addListener((obs, old, val) -> t.filter(val));

        t.addRow(twoLine("Mohammed Bennani", "MAT-2021-04"), twoLine("BE-889021", "CNSS: 198273645"),
                label("Chauffeur Routier", "cell"), permCell("EC", "PRM-883920"), label("15/03/2021", "cell"),
                status("Actif", "green"), linkButton("Dossier", () -> showToast("Affichage du dossier salarié de M. Bennani.")));
        t.addRow(twoLine("Abderrahim El Amrani", "MAT-2022-12"), twoLine("A-443219", "CNSS: 209384712"),
                label("Chauffeur Poids Lourds", "cell"), permCell("C", "PRM-112938"), label("01/06/2022", "cell"),
                status("Actif", "green"), linkButton("Dossier", () -> showToast("Affichage du dossier salarié de M. El Amrani.")));
        t.addRow(twoLine("Youssef Amrani", "MAT-2020-02"), twoLine("CD-110293", "CNSS: 102938475"),
                label("Chauffeur Super Lourds", "cell"), permCell("EC", "PRM-445201"), label("10/01/2020", "cell"),
                status("En Congé", "amber"), linkButton("Dossier", () -> showToast("Affichage du dossier salarié de M. Amrani.")));
        t.addRow(twoLine("Karima Bouazzaoui", "MAT-2023-08"), twoLine("BK-667123", "CNSS: 334928104"),
                label("Assistante Exploitation", "cell"), permCell("B", "PRM-992011"), label("01/02/2023", "cell"),
                status("Actif", "green"), linkButton("Dossier", () -> showToast("Affichage du dossier salarié de Mme. Bouazzaoui.")));

        tablePanel.getChildren().addAll(
                panelHeader("Registre du personnel", "Recherchez un salarié par nom, CIN, N° CNSS ou rôle.", search),
                t);
        sec.getChildren().add(tablePanel);
        return sec;
    }

    private VBox complianceSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Sécurité & Réglementation Transport",
                "Conformité, Permis & Visites Médicales",
                "Suivi strict des échéances de permis de conduire, carte FIMC/FCO et examens médicaux.",
                btnPrimary("🔄 Mettre à jour document", () -> openRenewLicenseModal(""))));

        VBox alert = panel();
        alert.getStyleClass().add("alert-panel");
        HBox alertRow = new HBox(14);
        alertRow.setAlignment(Pos.TOP_LEFT);
        VBox alertText = new VBox(4,
                label("Alerte Échéances Permis & Visites Médicales", "alert-title"),
                label("3 éléments nécessitent une action urgente avant suspension de la conduite.", "alert-text"));
        alertRow.getChildren().addAll(label("⚠️", "alert-icon"), alertText);
        alert.getChildren().add(alertRow);
        sec.getChildren().add(alert);

        VBox tablePanel = panel();
        TextField search = searchField("Filtrer par chauffeur...");
        DataTable t = new DataTable(
                new String[]{"Chauffeur", "N° Permis", "Catégorie", "Expiration Permis", "Carte FIMC/FCO", "Visite Médicale", "Statut Global", "Action"},
                new double[]{16, 12, 9, 17, 15, 14, 10, 7});
        search.textProperty().addListener((obs, old, val) -> t.filter(val));

        t.addRow(label("Abderrahim El Amrani", "cell-strong"), label("PRM-112938", "cell"), label("C / EC", "cell"),
                label("04/11/2026 (Expire bientôt)", "cell-red"), label("Valide (2027)", "cell"),
                label("15 jours restants", "cell-amber"), status("Urgent", "red"),
                smallButton("Renouveler", "btn-primary", () -> openRenewLicenseModal("Abderrahim El Amrani")));
        t.addRow(label("Mohammed Bennani", "cell-strong"), label("PRM-883920", "cell"), label("EC", "cell"),
                label("12/10/2028", "cell"), label("Valide (2028)", "cell"), label("11/2027", "cell"),
                status("Conforme", "green"),
                smallButton("Vérifier", "btn-secondary", () -> showToast("Dossier conforme.")));
        t.addRow(label("Youssef Amrani", "cell-strong"), label("PRM-445201", "cell"), label("EC", "cell"),
                label("21/08/2027", "cell"), label("À renouveler (30j)", "cell-amber"), label("04/2027", "cell"),
                status("À suivre", "amber"),
                smallButton("Renouveler", "btn-primary", () -> openRenewLicenseModal("Youssef Amrani")));

        tablePanel.getChildren().addAll(
                panelHeader("Registre de Conformité des Conducteurs", "Validité des pièces légales obligatoires au Maroc.", search),
                t);
        sec.getChildren().add(tablePanel);
        return sec;
    }
    private VBox leavesSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Suivi du Temps de Travail",
                "Congés & Absences",
                "Validation des demandes de congé payé, congés maladie et absences.",
                btnPrimary("➕ Nouvelle demande", this::openLeaveModal)));

        VBox tablePanel = panel();
        DataTable t = new DataTable(
                new String[]{"Employé", "Type", "Période", "Jours", "Statut", "Action"},
                new double[]{18, 24, 24, 10, 12, 12});
        t.addRow(label("Youssef Amrani", "cell-strong"), label("Congé Payé Annuel", "cell"),
                label("25/10/2026 → 02/11/2026", "cell"), label("8 jours", "cell"),
                status("Approuvé", "green"), label("Validé", "cell-sub"));
        addPendingLeave(t, "Karim El Idrissi", "Congé Exceptionnel (Mariage)", "05/11/2026 → 09/11/2026", "4 jours");
        addPendingLeave(t, "Abderrahim El Amrani", "Maladie (Justifié)", "28/10/2026 → 30/10/2026", "2 jours");
        tablePanel.getChildren().addAll(panelHeader("Demandes Récentes", "Validation des départs en congé", null), t);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        tablePanel.setMaxWidth(Double.MAX_VALUE);

        VBox balance = panel();
        balance.setSpacing(14);
        balance.setMinWidth(330);
        balance.setPrefWidth(330);
        balance.setMaxWidth(330);
        balance.getChildren().addAll(
                label("Solde des Congés (Moyenne)", "panel-title"),
                balanceRow("Chauffeurs PL", "18 j. / 21 j. restants", 0.85),
                balanceRow("Administration", "14 j. / 18 j. restants", 0.70),
                balanceRow("Équipe Atelier", "12 j. / 18 j. restants", 0.65));

        HBox row = new HBox(18, tablePanel, balance);
        sec.getChildren().add(row);
        return sec;
    }

    private void addPendingLeave(DataTable table, String name, String type, String period, String days) {
        HBox pill = status("En attente", "amber");
        HBox action = new HBox(8);
        action.setAlignment(Pos.CENTER_LEFT);
        Button approve = smallButton("Accorder", "btn-primary", () -> {
            setStatus(pill, "Accordé", "green");
            action.getChildren().setAll(label("Validé RH", "cell-sub"));
            showToast("Demande de congé approuvée.");
        });
        Button reject = smallButton("Refuser", "btn-danger", () -> {
            setStatus(pill, "Refusé", "red");
            action.getChildren().setAll(label("Rejeté RH", "cell-sub"));
            showToast("Demande de congé refusée.");
        });
        action.getChildren().addAll(approve, reject);
        table.addRow(label(name, "cell-strong"), label(type, "cell"), label(period, "cell"),
                label(days, "cell"), pill, action);
    }

    private VBox payrollSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Rémunération & Primes Logistiques",
                "Paie & Primes de Route",
                "Calcul des indemnités kilométriques, primes de trajet et variables du mois.",
                btnPrimary("🧮 Calculer les Primes", () -> showToast("Calcul global des primes du mois lancé."))));

        HBox stats = new HBox(16,
                statPanel("Masse Salariale Estimée", "285 000 MAD", null),
                statPanel("Total Primes de Route (Mois)", "48 500 MAD", "text-orange"),
                statPanel("Jours Travaillés Flotte", "620 Journées", "text-green"));
        for (Node n : stats.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        sec.getChildren().add(stats);

        VBox tablePanel = panel();
        TextField search = searchField("Rechercher chauffeur...");
        DataTable t = new DataTable(
                new String[]{"Chauffeur", "Missions Ce Mois", "Distance Totale", "Salaire de Base", "Prime de Route (MAD)", "Net Estimé", "Action"},
                new double[]{18, 14, 13, 13, 17, 14, 11});
        search.textProperty().addListener((obs, old, val) -> t.filter(val));

        t.addRow(label("Mohammed Bennani", "cell-strong"), label("14 trajets", "cell"), label("4 200 km", "cell"),
                label("5 500 MAD", "cell"), label("3 150 MAD", "cell-orange"), label("8 650 MAD", "cell-strong"),
                linkButton("Détail", () -> showToast("Fiche de calcul générée pour M. Bennani.")));
        t.addRow(label("Abderrahim El Amrani", "cell-strong"), label("18 trajets", "cell"), label("5 100 km", "cell"),
                label("5 200 MAD", "cell"), label("3 825 MAD", "cell-orange"), label("9 025 MAD", "cell-strong"),
                linkButton("Détail", () -> showToast("Fiche de calcul générée pour M. El Amrani.")));
        t.addRow(label("Youssef Amrani", "cell-strong"), label("10 trajets", "cell"), label("3 100 km", "cell"),
                label("5 500 MAD", "cell"), label("2 325 MAD", "cell-orange"), label("7 825 MAD", "cell-strong"),
                linkButton("Détail", () -> showToast("Fiche de calcul générée pour M. Amrani.")));

        tablePanel.getChildren().addAll(
                panelHeader("Décompte des Primes de Trajet par Chauffeur",
                        "Calcul basé sur la distance parcourue et le nombre de trajets accomplis.", search),
                t);
        sec.getChildren().add(tablePanel);
        return sec;
    }

    private VBox recruitmentSection() {
        VBox sec = section();
        sec.getChildren().add(headerRow(
                "Pipeline RH & Intégration",
                "Recrutement & Onboarding",
                "Gestion des candidatures de chauffeurs et parcours d'intégration.",
                btnPrimary("➕ Publier une offre", () -> showToast("Ouverture du formulaire de nouvelle offre d'emploi."))));

        VBox col1 = recruitmentColumn("Candidatures Reçues", "4", null,
                candidate("Amine El Idrissi", "Permis EC · 8 ans d'expérience PL", "Entretien à fixer", "blue",
                        "Inviter", null, "Invitation entretien envoyée."),
                candidate("Othmane Naciri", "Permis C · Transport National", "CV Reçu", "gray",
                        "Étudier", null, "CV présélectionné."));
        VBox col2 = recruitmentColumn("En Test Conduite", "1", "amber",
                candidate("Hassan Mansouri", "Test pratique sur Volvo FH à Casablanca", "Test en cours", "amber",
                        "Valider Test", "amber", "Validation du test pratique."));
        VBox col3 = recruitmentColumn("Onboarding & Visite Médicale", "1", "green",
                candidate("Tariq Kasmi", "Signature contrat CDI + Visite d'embauche", "Finalisation", "green",
                        "Intégrer", "green", "Salarié intégré à la paie."));

        HBox cols = new HBox(18, col1, col2, col3);
        for (Node n : cols.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        sec.getChildren().add(cols);
        return sec;
    }
    // ------------------------------------------------------------------
    // Small UI builders
    // ------------------------------------------------------------------

    private static VBox section() {
        VBox box = new VBox(22);
        box.getStyleClass().add("section");
        return box;
    }

    private HBox headerRow(String kicker, String title, String subtitle, Node action) {
        VBox left = new VBox(4,
                label(kicker, "eyebrow"),
                label(title, "section-title"),
                label(subtitle, "muted-sub"));
        HBox row = new HBox(12);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.getChildren().addAll(left, spacer());
        if (action != null) {
            row.getChildren().add(action);
        }
        return row;
    }

    private static VBox panel() {
        VBox box = new VBox();
        box.getStyleClass().add("panel");
        return box;
    }

    private static HBox panelHeader(String title, String subtitle, Node trailing) {
        HBox head = new HBox(12);
        head.getStyleClass().add("panel-header");
        head.setAlignment(Pos.CENTER_LEFT);
        head.getChildren().addAll(new VBox(3, label(title, "panel-title"), label(subtitle, "panel-sub")), spacer());
        if (trailing != null) {
            head.getChildren().add(trailing);
        }
        return head;
    }

    private VBox kpiCard(String title, String value, String unit, String icon, String tint,
                         String footnote, String footClass, Double progressValue) {
        VBox card = panel();
        card.getStyleClass().add("kpi");
        card.setSpacing(10);

        HBox valueRow = new HBox(5, label(value, "kpi-value"), label(unit, "kpi-unit"));
        valueRow.setAlignment(Pos.BASELINE_LEFT);
        VBox left = new VBox(8, label(title, "kpi-title"), valueRow);

        HBox top = new HBox(left, spacer(), label(icon, "kpi-icon", "kpi-icon-" + tint));
        card.getChildren().add(top);
        if (progressValue != null) {
            card.getChildren().add(progress(progressValue));
        }
        card.getChildren().add(label(footnote, "kpi-foot", footClass));
        return card;
    }

    private HBox quickAction(String icon, String title, String subtitle, String kind, Runnable action) {
        HBox box = new HBox(12);
        box.getStyleClass().addAll("quick-action", "quick-" + kind);
        box.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(3, label(title, "quick-title"), label(subtitle, "quick-sub"));
        HBox.setHgrow(text, Priority.ALWAYS);
        box.getChildren().addAll(label(icon, "quick-icon", "quick-icon-" + kind), text, label("›", "quick-chevron"));
        box.setOnMouseClicked(e -> action.run());
        return box;
    }

    private VBox statPanel(String title, String value, String valueClass) {
        VBox p = panel();
        p.setSpacing(8);
        Label v = label(value, "stat-value");
        if (valueClass != null) {
            v.getStyleClass().add(valueClass);
        }
        p.getChildren().addAll(label(title, "stat-label"), v);
        return p;
    }

    private VBox balanceRow(String name, String value, double percent) {
        HBox head = new HBox(label(name, "cell-strong"), spacer(), label(value, "cell-sub"));
        head.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, head, progress(percent));
    }

    private static ProgressBar progress(double percent) {
        ProgressBar bar = new ProgressBar(percent);
        bar.getStyleClass().add("progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        return bar;
    }

    private VBox recruitmentColumn(String title, String count, String kind, Node... cards) {
        VBox col = panel();
        col.setSpacing(12);
        HBox head = new HBox(10, label(title, "column-title"), spacer(), countBadge(count, kind));
        head.getStyleClass().add("panel-header");
        head.setAlignment(Pos.CENTER_LEFT);
        col.getChildren().add(head);
        col.getChildren().addAll(cards);
        return col;
    }

    private VBox candidate(String name, String info, String statusText, String statusKind,
                           String actionText, String cardKind, String toastMessage) {
        VBox card = new VBox(8);
        card.getStyleClass().add("candidate-card");
        if (cardKind != null) {
            card.getStyleClass().add("candidate-card-" + cardKind);
        }
        HBox foot = new HBox(10, status(statusText, statusKind), spacer(), linkButton(actionText, () -> showToast(toastMessage)));
        foot.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(label(name, "candidate-name"), label(info, "candidate-info"), foot);
        return card;
    }

    private static Label countBadge(String text, String kind) {
        Label badge = label(text, "count-badge");
        if (kind != null) {
            badge.getStyleClass().add("count-badge-" + kind);
        }
        return badge;
    }

    private static TextField searchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("field");
        field.setPrefWidth(240);
        field.setMaxWidth(280);
        return field;
    }

    private Button btnPrimary(String text, Runnable action) {
        return styledButton(text, "btn-primary", action);
    }

    private Button btnSecondary(String text, Runnable action) {
        return styledButton(text, "btn-secondary", action);
    }

    private Button iconButton(String icon, Runnable action) {
        return styledButton(icon, "icon-button", action);
    }

    private Button linkButton(String text, Runnable action) {
        return styledButton(text, "btn-ghost", action);
    }

    private Button smallButton(String text, String styleClass, Runnable action) {
        Button button = styledButton(text, styleClass, action);
        button.setStyle("-fx-padding: 4 10; -fx-font-size: 11;");
        return button;
    }

    private Button styledButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private static Label label(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    private static VBox twoLine(String main, String sub) {
        return new VBox(2, label(main, "cell-strong"), label(sub, "cell-sub"));
    }

    private static HBox permCell(String category, String number) {
        HBox box = new HBox(4, label(category, "cell-strong"), label("· " + number, "cell"));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static HBox status(String text, String kind) {
        HBox pill = new HBox(6);
        pill.getStyleClass().addAll("status", "status-" + kind);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setMaxWidth(Region.USE_PREF_SIZE);
        Region dot = new Region();
        dot.getStyleClass().addAll("status-dot", "dot-" + kind);
        pill.getChildren().addAll(dot, label(text, "status-text"));
        return pill;
    }

    private static void setStatus(HBox pill, String text, String kind) {
        pill.getStyleClass().removeIf(c -> c.startsWith("status-"));
        pill.getStyleClass().add("status-" + kind);
        Region dot = (Region) pill.getChildren().get(0);
        dot.getStyleClass().removeIf(c -> c.startsWith("dot-"));
        dot.getStyleClass().add("dot-" + kind);
        ((Label) pill.getChildren().get(1)).setText(text);
    }
    // ------------------------------------------------------------------
    // Modal, toast, actions
    // ------------------------------------------------------------------

    private void openEmployeeModal() {
        openModal("Nouveau Salarié",
                "Saisissez les informations administratives du nouveau collaborateur.",
                values -> showToast("Salarié " + values.get("fullname") + " créé avec succès."),
                new Field("Nom & Prénom", "fullname", "text", null, "Ex: Amine El Idrissi", null),
                new Field("CIN", "cin", "text", null, "Ex: BE-998822", null),
                new Field("Numéro CNSS", "cnss", "text", null, "Ex: 198273645", null),
                new Field("Poste Occupé", "role", "select",
                        List.of("Chauffeur Routier (EC)", "Chauffeur Poids Lourds (C)", "Agent de Dépôt",
                                "Mécanicien Atelier", "Gestionnaire Administration"), null, null),
                new Field("Type de Contrat", "contract", "select",
                        List.of("CDI", "CDD", "Période d'essai"), null, null));
    }

    private void openLeaveModal() {
        openModal("Saisie de Demande de Congé",
                "Enregistrez une absence ou un congé payé.",
                values -> showToast("Demande de congé enregistrée pour " + values.get("employee") + "."),
                new Field("Salarié concerné", "employee", "text", null, "Ex: Mohammed Bennani", null),
                new Field("Type de congé", "type", "select",
                        List.of("Congé Payé Annuel", "Maladie / Accident", "Evénement Familial",
                                "Absence Injustifiée"), null, null),
                new Field("Date de début", "start", "date", null, null, null),
                new Field("Date de fin", "end", "date", null, null, null));
    }

    private void openRenewLicenseModal(String presetDriver) {
        openModal("Renouvellement Document / Permis",
                "Saisissez la nouvelle date d'échéance du permis ou de la visite médicale.",
                values -> showToast("Mise à jour effectuée : " + values.get("docType") + " pour " + values.get("driver") + "."),
                new Field("Chauffeur", "driver", "text", null, "Nom du chauffeur", presetDriver),
                new Field("Document renouvelé", "docType", "select",
                        List.of("Permis de Conduire (C/EC)", "Carte FIMC/FCO", "Visite Médicale Obligatoire"), null, null),
                new Field("Nouvelle Date d'expiration", "expDate", "date", null, null, null));
    }

    private void openModal(String title, String description, Consumer<Map<String, String>> onSubmit, Field... fields) {
        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox form = new VBox(16);
        form.getStyleClass().add("modal-root");
        form.setPrefWidth(460);

        VBox head = new VBox(6,
                label("Gestion RH", "eyebrow"),
                label(title, "modal-title"),
                label(description, "modal-desc"));

        VBox fieldsBox = new VBox(14);
        Map<String, Control> inputs = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (Field field : fields) {
            VBox wrap = new VBox(6, label(field.label(), "field-label"));
            Control input;
            if (field.options() != null) {
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll(field.options());
                combo.getSelectionModel().selectFirst();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.getStyleClass().add("field");
                input = combo;
            } else if ("textarea".equals(field.type())) {
                TextArea area = new TextArea();
                area.setPrefRowCount(3);
                area.setPromptText(field.placeholder() == null ? "" : field.placeholder());
                area.getStyleClass().add("field");
                input = area;
            } else if ("date".equals(field.type())) {
                DatePicker picker = new DatePicker();
                picker.setMaxWidth(Double.MAX_VALUE);
                picker.getStyleClass().add("field");
                input = picker;
            } else {
                TextField text = new TextField();
                text.setPromptText(field.placeholder() == null ? "" : field.placeholder());
                if (field.value() != null) {
                    text.setText(field.value());
                }
                text.getStyleClass().add("field");
                input = text;
            }
            wrap.getChildren().add(input);
            fieldsBox.getChildren().add(wrap);
            inputs.put(field.name(), input);
            labels.put(field.name(), field.label());
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button save = btnPrimary("Enregistrer", () -> {
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, Control> entry : inputs.entrySet()) {
                String value = readValue(entry.getValue());
                if (value == null || value.isBlank()) {
                    showToast("Veuillez renseigner : " + labels.get(entry.getKey()));
                    return;
                }
                values.put(entry.getKey(), value);
            }
            onSubmit.accept(values);
            dialog.close();
        });
        actions.getChildren().addAll(btnSecondary("Annuler", dialog::close), save);

        form.getChildren().addAll(head, fieldsBox, actions);
        dialog.setScene(new Scene(form));
        dialog.sizeToScene();
        dialog.showAndWait();
    }

    private static String readValue(Control control) {
        if (control instanceof TextInputControl text) {
            return text.getText();
        }
        if (control instanceof ComboBox<?> combo) {
            return combo.getValue() == null ? "" : String.valueOf(combo.getValue());
        }
        if (control instanceof DatePicker picker) {
            return picker.getValue() == null ? "" : picker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "";
    }

    private void showToast(String message) {
        toast.setText(message);
        toast.setVisible(true);
        if (toastTimer != null) {
            toastTimer.stop();
        }
        toastTimer = new PauseTransition(Duration.seconds(3.5));
        toastTimer.setOnFinished(e -> toast.setVisible(false));
        toastTimer.playFromStart();
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    private void exportHRReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport RH");
        chooser.setInitialFileName("rapport-conformite-rh.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        Window owner = getScene() == null ? null : getScene().getWindow();
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        String[][] rows = {
                {"Matricule", "Nom", "CIN", "Poste", "Statut Permis", "Visite Medicale"},
                {"MAT-2021-04", "Mohammed Bennani", "BE-889021", "Chauffeur Routier", "Valide 2028", "Conforme"},
                {"MAT-2022-12", "Abderrahim El Amrani", "A-443219", "Chauffeur PL", "Expire Bientot", "Echeance 15j"}
        };
        StringBuilder csv = new StringBuilder("\uFEFF");
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows[i].length; j++) {
                if (j > 0) {
                    csv.append(';');
                }
                csv.append('"').append(rows[i][j]).append('"');
            }
            if (i < rows.length - 1) {
                csv.append("\r\n");
            }
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            showToast("Export CSV du rapport RH généré.");
        } catch (IOException ex) {
            showToast("Échec de l'export : " + ex.getMessage());
        }
    }

    private void imprimer() {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job == null) {
                showToast("Aucune imprimante n'est disponible.");
                return;
            }
            Window owner = getScene() == null ? null : getScene().getWindow();
            if (!job.showPrintDialog(owner)) {
                return;
            }
            WritableImage image = snapshot(null, null);
            ImageView view = new ImageView(image);
            view.setFitWidth(job.getJobSettings().getPageLayout().getPrintableWidth());
            view.setPreserveRatio(true);
            if (job.printPage(view)) {
                job.endJob();
            }
            showToast("Impression envoyée.");
        } catch (RuntimeException ex) {
            showToast("Impression impossible.");
        }
    }
    // ------------------------------------------------------------------
    // Custom table + helpers
    // ------------------------------------------------------------------

    private static GridPane gridOf(double[] widths) {
        GridPane grid = new GridPane();
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private static void collectText(Node node, StringBuilder builder) {
        if (node instanceof Labeled labeled && labeled.getText() != null) {
            builder.append(labeled.getText()).append(' ');
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectText(child, builder);
            }
        }
    }

    /** Lightweight table built from a GridPane, with simple client-side filtering. */
    private static final class DataTable extends VBox {
        private final double[] widths;
        private final List<GridPane> rows = new ArrayList<>();

        DataTable(String[] headers, double[] widths) {
            this.widths = widths;
            getStyleClass().add("data-table");
            GridPane head = gridOf(widths);
            head.getStyleClass().add("table-head");
            for (int i = 0; i < headers.length; i++) {
                head.add(label(headers[i], "th"), i, 0);
            }
            getChildren().add(head);
        }

        void addRow(Node... cells) {
            GridPane row = gridOf(widths);
            row.getStyleClass().add("tr");
            for (int i = 0; i < cells.length; i++) {
                Node cell = cells[i];
                GridPane.setValignment(cell, VPos.CENTER);
                GridPane.setHalignment(cell, HPos.LEFT);
                if (cell instanceof Region region && !cell.getStyleClass().contains("status")) {
                    region.setMaxWidth(Double.MAX_VALUE);
                }
                row.add(cell, i, 0);
            }
            rows.add(row);
            getChildren().add(row);
        }

        void filter(String query) {
            String needle = query == null ? "" : query.trim().toLowerCase(Locale.FRENCH);
            for (GridPane row : rows) {
                StringBuilder builder = new StringBuilder();
                collectText(row, builder);
                boolean visible = needle.isEmpty()
                        || builder.toString().toLowerCase(Locale.FRENCH).contains(needle);
                row.setVisible(visible);
                row.setManaged(visible);
            }
        }
    }

    /** Description of one field rendered inside the generic modal. */
    private record Field(String label, String name, String type, List<String> options,
                         String placeholder, String value) {
    }
}
