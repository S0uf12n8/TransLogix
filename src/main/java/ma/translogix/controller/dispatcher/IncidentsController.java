package ma.translogix.controller.dispatcher;

import javafx.fxml.FXML;

/** "Incidents & Retards de Route": Iteration 3 page, Hamid's layout with sample incidents (DÉMO). */
public class IncidentsController {

    /** "Déclarer Incident", "Appeler Assistance", "Résoudre", "Prévenir Client". */
    @FXML
    private void pasEncoreLivre() {
        DispatcherUi.pasEncoreLivre();
    }
}
