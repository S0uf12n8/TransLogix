package ma.translogix.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ma.translogix.AppContext;

/** Simple counters for iteration 2. Charts come in iteration 3. */
public class DashboardController {

    @FXML
    private Label nbUtilisateurs;

    @FXML
    private void initialize() {
        nbUtilisateurs.setText(String.valueOf(AppContext.utilisateurService().lister().size()));
    }
}
