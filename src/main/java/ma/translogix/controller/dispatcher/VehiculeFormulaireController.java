package ma.translogix.controller.dispatcher;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.Vehicule;
import ma.translogix.service.VehiculeService;

/** Fields of the vehicle dialog. Rules stay in VehiculeService. */
public class VehiculeFormulaireController implements DispatcherUi.Formulaire {
    static final String FXML = "dispatcher/vehicule-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private TextField champMatricule;
    @FXML private TextField champCapacite;
    @FXML private TextField champMarque;
    @FXML private TextField champModele;

    private final VehiculeService service = AppContext.vehiculeService();
    /** null when creating a new vehicle. */
    private Vehicule edite;

    /** Fills the form for an edit, or prepares a creation (vehicule == null). */
    void preparer(Vehicule vehicule) {
        edite = vehicule;
        if (vehicule == null) {
            return;
        }
        titre.setText("Modifier " + vehicule.getMatricule());
        description.setText("Le statut se change depuis le tableau (« Changer statut »).");
        champMatricule.setText(vehicule.getMatricule());
        champCapacite.setText(vehicule.getCapaciteTonnes() == null ? ""
                : vehicule.getCapaciteTonnes().stripTrailingZeros().toPlainString().replace('.', ','));
        champMarque.setText(vehicule.getMarque());
        champModele.setText(vehicule.getModele());
    }

    @Override
    public void enregistrer() {
        var capacite = DispatcherUi.lireTonnes(champCapacite, "Capacité");
        if (edite == null) {
            service.creer(champMatricule.getText(), champMarque.getText(), champModele.getText(), capacite);
        } else {
            service.modifier(edite.getId(), champMatricule.getText(), champMarque.getText(), champModele.getText(),
                    capacite);
        }
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
