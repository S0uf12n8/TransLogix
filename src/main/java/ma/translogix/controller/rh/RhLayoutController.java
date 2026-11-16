package ma.translogix.controller.rh;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Shell of the "Espace RH": dark side menu on the left, topbar and swappable content. */
public class RhLayoutController {

    private static final Map<String, String[]> PAGES = Map.of(
            "dashboard", new String[]{"Vue Synth\u00e9tique", "Tableau de bord RH"},
            "staff", new String[]{"Effectif & Registre du Personnel", "Chauffeurs & Employ\u00e9s"},
            "compliance", new String[]{"S\u00e9curit\u00e9 & R\u00e9glementation Transport", "Conformit\u00e9 & Permis"},
            "leaves", new String[]{"Suivi du Temps de Travail", "Cong\u00e9s & Absences"},
            "payroll", new String[]{"R\u00e9mun\u00e9ration & Primes Logistiques", "Paie & Prime Trajet"},
            "recruitment", new String[]{"Pipeline RH & Int\u00e9gration", "Recrutement & Onboarding"});

    @FXML private StackPane contenu;
    @FXML private ScrollPane scroll;
    @FXML private Label pageKicker;
    @FXML private Label pageTitle;
    @FXML private Label currentDate;
    @FXML private Label toast;
    @FXML private Button navDashboard;
    @FXML private Button navPersonnel;
    @FXML private Button navConformite;
    @FXML private Button navConges;
    @FXML private Button navPaie;
    @FXML private Button navRecrutement;

    private PauseTransition delaiToast;

    @FXML
    private void initialize() {
        currentDate.setText(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH).format(LocalDate.now()));
        RhNotify.setSink(this::notifier);
        afficherTableauDeBord();
    }

    @FXML
    private void afficherTableauDeBord() {
        afficher("dashboard", "rh-dashboard.fxml", navDashboard);
    }

    @FXML
    private void afficherPersonnel() {
        afficher("staff", "rh-staff.fxml", navPersonnel);
    }

    @FXML
    private void afficherConformite() {
        afficher("compliance", "rh-compliance.fxml", navConformite);
    }

    @FXML
    private void afficherConges() {
        afficher("leaves", "rh-leaves.fxml", navConges);
    }

    @FXML
    private void afficherPaie() {
        afficher("payroll", "rh-payroll.fxml", navPaie);
    }

    @FXML
    private void afficherRecrutement() {
        afficher("recruitment", "rh-recruitment.fxml", navRecrutement);
    }

    @FXML
    private void ajouterSalarie() {
        RhFormDialog.ouvrir(fenetre(), "Nouveau Salari\u00e9",
                "Gestion RH",
                "Saisissez les informations administratives du nouveau collaborateur.",
                List.of(
                        RhFormDialog.Champ.texte("Nom & Pr\u00e9nom", "fullname", "Ex: Amine El Idrissi"),
                        RhFormDialog.Champ.texte("CIN", "cin", "Ex: BE-998822"),
                        RhFormDialog.Champ.texte("Num\u00e9ro CNSS", "cnss", "Ex: 198273645"),
                        RhFormDialog.Champ.choix("Poste Occup\u00e9", "role", List.of(
                                "Chauffeur Routier (EC)", "Chauffeur Poids Lourds (C)",
                                "Agent de D\u00e9p\u00f4t", "M\u00e9canicien Atelier", "Gestionnaire Administration")),
                        RhFormDialog.Champ.choix("Type de Contrat", "contract", List.of("CDI", "CDD", "P\u00e9riode d'essai"))))
                .ifPresent(valeurs -> notifier("Salari\u00e9 " + valeurs.get("fullname") + " cr\u00e9\u00e9 avec succ\u00e8s."));
    }

    @FXML
    private void imprimer() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            notifier("Aucune imprimante disponible.");
            return;
        }
        if (job.showPrintDialog(fenetre()) && job.printPage(contenu)) {
            job.endJob();
            notifier("Impression lanc\u00e9e.");
        } else {
            job.endJob();
        }
    }

    @FXML
    private void deconnexion() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "D\u00e9connexion du portail RH ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("D\u00e9connexion");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            notifier("Session RH ferm\u00e9e.");
        }
    }

    private void afficher(String cle, String fichier, Button boutonActif) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource("/ma/translogix/fxml/rh/" + fichier));
            contenu.getChildren().setAll(vue);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + fichier, e);
        }
        String[] page = PAGES.get(cle);
        pageKicker.setText(page[0]);
        pageTitle.setText(page[1]);
        activer(boutonActif);
        if (scroll != null) {
            scroll.setVvalue(0);
        }
    }

    private void activer(Button boutonActif) {
        for (Button bouton : List.of(navDashboard, navPersonnel, navConformite, navConges, navPaie, navRecrutement)) {
            bouton.getStyleClass().remove("active");
        }
        if (boutonActif != null && !boutonActif.getStyleClass().contains("active")) {
            boutonActif.getStyleClass().add("active");
        }
    }

    private void notifier(String message) {
        toast.setText(message);
        toast.setVisible(true);
        toast.setManaged(true);
        if (delaiToast != null) {
            delaiToast.stop();
        }
        delaiToast = new PauseTransition(Duration.seconds(3.5));
        delaiToast.setOnFinished(e -> {
            toast.setVisible(false);
            toast.setManaged(false);
        });
        delaiToast.play();
    }

    private javafx.stage.Window fenetre() {
        return toast.getScene() == null ? null : toast.getScene().getWindow();
    }
}
