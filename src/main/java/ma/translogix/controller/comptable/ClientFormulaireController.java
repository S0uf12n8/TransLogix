package ma.translogix.controller.comptable;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.Client;
import ma.translogix.service.ClientService;
import ma.translogix.util.ValidationException;

/** Fields of the client dialog. Rules stay in ClientService. */
public class ClientFormulaireController implements ComptableUi.Formulaire {
    static final String FXML = "comptable/client-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private TextField champNom;
    @FXML private TextField champIce;
    @FXML private TextField champTelephone;
    @FXML private TextField champEmail;
    @FXML private TextField champDelai;
    @FXML private TextField champAdresse;

    private final ClientService service = AppContext.clientService();
    /** null when creating a new client. */
    private Client edite;

    /** Fills the form for an edit, or prepares a creation (client == null). */
    void preparer(Client client) {
        edite = client;
        if (client == null) {
            champDelai.setText(String.valueOf(Client.DELAI_PAR_DEFAUT));
            return;
        }
        titre.setText("Modifier " + client.getNom());
        description.setText("Modifiez les coordonnées ou les conditions de règlement.");
        champNom.setText(client.getNom());
        champIce.setText(client.getIce());
        champTelephone.setText(client.getTelephone());
        champEmail.setText(client.getEmail());
        champDelai.setText(String.valueOf(client.getDelaiPaiementJours()));
        champAdresse.setText(client.getAdresse());
    }

    @Override
    public void enregistrer() {
        int delai = lireDelai();
        if (edite == null) {
            service.creer(champNom.getText(), champIce.getText(), champTelephone.getText(),
                    champEmail.getText(), champAdresse.getText(), delai);
        } else {
            service.modifier(edite.getId(), champNom.getText(), champIce.getText(), champTelephone.getText(),
                    champEmail.getText(), champAdresse.getText(), delai);
        }
    }

    @Override
    public String titre() {
        return titre.getText();
    }

    private int lireDelai() {
        String texte = champDelai.getText() == null ? "" : champDelai.getText().trim();
        if (texte.isEmpty()) {
            return Client.DELAI_PAR_DEFAUT;
        }
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            throw new ValidationException("Le délai de paiement doit être un nombre entier de jours.");
        }
    }
}
