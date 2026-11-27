package ma.translogix.controller.comptable;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import ma.translogix.model.Depense;
import ma.translogix.service.DepenseService;
import ma.translogix.util.Formats;

/** Reason for refusing an expense. The reason is mandatory and kept in the audit trail. */
public class DepenseRefusController implements ComptableUi.Formulaire {

    static final String FXML = "comptable/depense-refus-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label resume;
    @FXML private TextArea champMotif;

    private DepenseService service;
    private Depense depense;

    void preparer(DepenseService service, Depense depense) {
        this.service = service;
        this.depense = depense;
        resume.setText(depense.getStation() + " · " + Formats.mad(depense.getMontant()));
    }

    @Override
    public void enregistrer() {
        service.refuser(depense.getId(), champMotif.getText());
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
