package ma.translogix.controller.rh;

import javafx.fxml.FXML;

/**
 * "Recrutement & Onboarding": Iteration 3. Majda's kanban with fictional candidates is shown as a DÉMO;
 * every button ("Publier une offre", "Inviter", "Étudier", "Valider Test", "Intégrer") only says
 * the module is not delivered yet.
 */
public class RecrutementController {

    @FXML
    private void pasEncoreLivre() {
        RhUi.pasEncoreLivre();
    }
}
