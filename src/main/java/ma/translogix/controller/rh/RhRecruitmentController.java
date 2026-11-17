package ma.translogix.controller.rh;

import javafx.fxml.FXML;

/** Pipeline de recrutement et d'integration. */
public class RhRecruitmentController {

    @FXML
    private void publierOffre() {
        RhNotify.toast("Ouverture du formulaire de nouvelle offre d'emploi.");
    }

    @FXML
    private void inviter() {
        RhNotify.toast("Invitation entretien envoy\u00e9e.");
    }

    @FXML
    private void etudier() {
        RhNotify.toast("CV pr\u00e9s\u00e9lectionn\u00e9.");
    }

    @FXML
    private void validerTest() {
        RhNotify.toast("Validation du test pratique.");
    }

    @FXML
    private void integrer() {
        RhNotify.toast("Salari\u00e9 int\u00e9gr\u00e9 \u00e0 la paie.");
    }
}
