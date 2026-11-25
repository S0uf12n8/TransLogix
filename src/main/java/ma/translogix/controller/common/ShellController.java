package ma.translogix.controller.common;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Tooltip;
import ma.translogix.AppContext;
import ma.translogix.Persistance;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * The shared banner: sidebar + top bar. Every role uses it.
 * It is the Navigator host: each page is loaded into the centre area; pages never contain the sidebar.
 */
public class ShellController implements Navigator.Hote {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    @FXML private Label sousTitre;
    @FXML private Label initiales;
    @FXML private Label nomUtilisateur;
    @FXML private Label roleUtilisateur;
    @FXML private Label kicker;
    @FXML private Label titrePage;
    @FXML private Label dateDuJour;
    @FXML private Label metaDemo;
    @FXML private Label modeStockage;
    @FXML private VBox menu;
    @FXML private StackPane contenu;

    private final Map<String, Button> boutons = new HashMap<>();
    private Role role;
    private Button boutonActif;
    /** Controller of the page shown in contenu: asked for its printed version by imprimer(). */
    private Object controleurPage;

    @FXML
    private void initialize() {
        Utilisateur utilisateur = Session.utilisateur();
        role = utilisateur.getRole();

        sousTitre.setText(NavigationMenu.sousTitre(role));
        initiales.setText(initialesDe(utilisateur));
        nomUtilisateur.setText(utilisateur.getNomComplet());
        roleUtilisateur.setText(role.getLibelle());
        dateDuJour.setText(LocalDate.now().format(DATE));
        afficherModeStockage(AppContext.persistance());

        String premiere = null;
        for (NavigationMenu.Entree entree : NavigationMenu.pour(role)) {
            Button bouton = new Button(entree.libelle());
            bouton.getStyleClass().add("nav-item");
            bouton.setMaxWidth(Double.MAX_VALUE);
            if (entree.icone() != null) {
                SVGPath icone = new SVGPath();
                icone.setContent(entree.icone());
                icone.getStyleClass().add("nav-icon");
                bouton.setGraphic(icone);
            }
            bouton.setOnAction(e -> Navigator.ouvrir(entree.fxml()));
            boutons.put(entree.fxml(), bouton);
            menu.getChildren().add(bouton);
            if (premiere == null && Navigator.existe(entree.fxml())) {
                premiere = entree.fxml();
            }
        }

        Navigator.enregistrer(this);
        if (premiere != null) {
            Navigator.ouvrir(premiere);
        }
    }

    // ------------------------------------------------------------- Navigator.Hote

    @Override
    public void afficher(String fxml) {
        URL vue = getClass().getResource(Navigator.RACINE + fxml);
        try {
            FXMLLoader loader = new FXMLLoader(vue);
            Parent page = loader.load();
            contenu.getChildren().setAll(page);
            controleurPage = loader.getController();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Dialogues.erreur("Impossible d'ouvrir " + fxml + "\n" + e.getMessage());
            return;
        }
        Optional<NavigationMenu.Entree> entree = NavigationMenu.entreePour(role, fxml);
        surligner(entree.map(e -> boutons.get(e.fxml())).orElse(null));
        titrePage.setText(entree.map(NavigationMenu.Entree::libelle).orElse(""));
        String texteKicker = entree.map(NavigationMenu.Entree::kicker).orElse(null);
        kicker.setText((texteKicker != null ? texteKicker : role.getLibelle()).toUpperCase(Locale.FRENCH));
        boolean demo = entree.map(NavigationMenu.Entree::demo).orElse(false);
        metaDemo.setVisible(demo);
        metaDemo.setManaged(demo);
    }

    @Override
    public void signalerNonLivre(String fxml) {
        String module = NavigationMenu.entreePour(role, fxml)
                .map(NavigationMenu.Entree::libelle)
                .orElse(fxml);
        Dialogues.info("Module pas encore livré",
                "Module pas encore livré : « " + module + " ».\n"
                        + "Cet écran sera disponible quand son module sera intégré.");
    }

    // ------------------------------------------------------------- top bar & sidebar actions

    /**
     * Prints a real report of the current page, not a screenshot of it: the page's own document when its
     * controller is Imprimable, otherwise its first table (rows as filtered on screen). Pages flagged demo
     * in the menu say so on every printed page.
     */
    @FXML
    private void imprimer() {
        if (contenu.getChildren().isEmpty()) {
            Dialogues.info("Imprimer", "Aucune page à imprimer.");
            return;
        }
        Optional<ImpressionRapport.Document> document;
        if (controleurPage instanceof Imprimable imprimable) {
            document = Optional.of(imprimable.documentAImprimer());
        } else {
            boolean demo = metaDemo.isVisible();
            document = RapportDepuisTable.depuisPage(contenu.getChildren().get(0), titrePage.getText())
                    .map(r -> new ImpressionRapport.Document(r, List.of(), demo));
        }
        if (document.isEmpty()) {
            Dialogues.info("Imprimer", "Cette page n'a pas de tableau à imprimer.\n"
                    + "Les rapports détaillés sont dans « Rapports & analytics ».");
            return;
        }
        ImpressionRapport.imprimer(document.get(), contenu.getScene().getWindow());
    }

    @FXML
    private void deconnexion() {
        if (!Dialogues.confirmer("Déconnexion", "Voulez-vous vous déconnecter ?")) {
            return;
        }
        Navigator.liberer(this);
        Session.fermer();
        try {
            Parent login = FXMLLoader.load(getClass().getResource(Navigator.RACINE + "auth/login.fxml"));
            contenu.getScene().setRoot(login);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'écran de connexion", e);
        }
    }

    private void surligner(Button bouton) {
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("active");
        }
        if (bouton != null) {
            bouton.getStyleClass().add("active");
        }
        boutonActif = bouton;
    }

    /** Sidebar line saying where the data is kept; orange when nothing is saved. */
    private void afficherModeStockage(Persistance.Etat etat) {
        modeStockage.setText(etat.libelle());
        Tooltip aide = new Tooltip(etat.detail());
        aide.setWrapText(true);
        aide.setMaxWidth(320);
        modeStockage.setTooltip(aide);
        if (!etat.enregistre()) {
            modeStockage.getStyleClass().add("storage-mode-warning");
        }
    }

    private static String initialesDe(Utilisateur utilisateur) {
        String p = utilisateur.getPrenom();
        String n = utilisateur.getNom();
        String i = (p == null || p.isBlank() ? "" : p.substring(0, 1))
                + (n == null || n.isBlank() ? "" : n.substring(0, 1));
        return i.toUpperCase(Locale.FRENCH);
    }
}
