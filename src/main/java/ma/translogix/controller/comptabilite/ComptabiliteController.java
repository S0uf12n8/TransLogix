
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ComptabiliteController {

    private final BorderPane mainLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final Label pageTitle = new Label("Tableau de bord");

    /** Builds the whole layout once (sidebar left, title top, page center) and opens the dashboard. */
    public ComptabiliteController() {

        VBox topBar = new VBox(pageTitle);
        topBar.setPadding(new Insets(20, 28, 20, 28));
        topBar.getStyleClass().add("topbar");

        contentArea.getStyleClass().add("content-area");

        mainLayout.setLeft(createSidebar());
        mainLayout.setTop(topBar);
        mainLayout.setCenter(contentArea);

        showDashboard();
    }

    /** The layout built by the constructor, to put in a Scene. */
    public BorderPane createMainLayout() {
        return mainLayout;
    }

    private VBox createSidebar() {

        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(25, 15, 20, 15));
        sidebar.setPrefWidth(235);
        sidebar.getStyleClass().add("sidebar");

        Label brand = new Label("TRANSLOGIX");
        brand.getStyleClass().add("brand");

        Label subtitle = new Label("ESPACE COMPTABILITÉ");
        subtitle.getStyleClass().add("sidebar-subtitle");

        sidebar.getChildren().addAll(
            brand,
            subtitle,
            createButton("Tableau de bord", this::showDashboard),
            createButton("Facturation / TVA", this::showFacturation),
            createButton("Recouvrement B2B", this::showRecouvrement),
            createButton("Dettes fournisseurs", this::showFournisseurs),
            createButton("Règlements fournisseurs", this::showReglements),
            createButton("Dépenses", this::showDepenses),
            createButton("Paie et primes", this::showPaie),
            createButton("Journal PCGM", this::showJournal),
            createButton("TVA et impôts", this::showTVA),
            createButton("Trésorerie", this::showTresorerie)
        );

        return sidebar;
    }

    private Button createButton(String text, Runnable action) {

        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> action.run());

        return button;
    }

    private void loadPage(String file, String title) {

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(file)
            );

            Parent page = loader.load();

            contentArea.getChildren().setAll(page);
            pageTitle.setText(title);

        } catch (IOException | NullPointerException exception) {

            exception.printStackTrace();

            Label error = new Label(
                "Impossible de charger la page : " + file
            );

            contentArea.getChildren().setAll(error);
        }
    }

    public void showDashboard() {
        loadPage("/ma/translogix/fxml/comptabilite/Dashboard.fxml",
            "Tableau de bord");
    }

    public void showFacturation() {
        loadPage("/ma/translogix/fxml/comptabilite/Facturation.fxml",
            "Facturation");
    }

    public void showRecouvrement() {
        loadPage("/ma/translogix/fxml/comptabilite/Recouvrement.fxml",
            "Recouvrement B2B");
    }

    public void showFournisseurs() {
        loadPage("/ma/translogix/fxml/comptabilite/Fournisseurs.fxml",
            "Dettes fournisseurs");
    }

    public void showReglements() {
        loadPage("/ma/translogix/fxml/comptabilite/Reglements.fxml",
            "Règlements fournisseurs");
    }

    public void showDepenses() {
        loadPage("/ma/translogix/fxml/comptabilite/Depenses.fxml",
            "Dépenses");
    }

    public void showPaie() {
        loadPage("/ma/translogix/fxml/comptabilite/Paie.fxml",
            "Paie et primes");
    }

    public void showJournal() {
        loadPage("/ma/translogix/fxml/comptabilite/Journal.fxml",
            "Journal comptable");
    }

    public void showTVA() {
        loadPage("/ma/translogix/fxml/comptabilite/TVA.fxml",
            "TVA et impôts");
    }

    public void showTresorerie() {
        loadPage("/ma/translogix/fxml/comptabilite/Tresorerie.fxml",
            "Trésorerie");
    }
}
