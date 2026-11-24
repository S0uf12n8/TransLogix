package ma.translogix.controller.chauffeur;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConformite;

/** Section 4 : Permis &amp; Documents. */
public class ProfileController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    @FXML private Label avatarInitials, driverNameLabel, cinLabel, cnssLabel;
    @FXML private Text hireDateText, contractText, depotText;
    @FXML private VBox docsBox;

    @FXML
    private void initialize() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        Employe chauffeur = ctx.chauffeur();
        if (chauffeur == null) {
            avatarInitials.setText("—");
            driverNameLabel.setText("—");
            cinLabel.setText("CIN : —");
            cnssLabel.setText("CNSS : —");
            hireDateText.setText(" —");
            contractText.setText(" —");
            depotText.setText(" —");
            renderDocuments(List.of());
            return;
        }

        avatarInitials.setText(initiales(chauffeur));
        driverNameLabel.setText(nvl(chauffeur.getNomComplet()));
        cinLabel.setText("CIN : " + nvl(chauffeur.getCin()));
        cnssLabel.setText("CNSS : " + nvl(chauffeur.getCnss()));
        hireDateText.setText(chauffeur.getDateEmbauche() == null
                ? " —" : " " + DATE.format(chauffeur.getDateEmbauche()));

        String contrat = "—";
        if (chauffeur.getTypeContrat() != null) {
            contrat = chauffeur.getTypeContrat().getLibelle();
            if (contrat == null || contrat.isBlank()) contrat = chauffeur.getTypeContrat().toString();
        }
        contractText.setText(" " + contrat);
        depotText.setText(" —"); // Employe n'a pas de dépôt de rattachement

        List<DocumentConformite> documents;
        try {
            documents = ma.translogix.AppContext.conformiteService().documentsDe(chauffeur.getId());
        } catch (RuntimeException e) {
            documents = List.of();
        }
        renderDocuments(documents);
    }

    /** Remplace les quatre lignes statiques par une ligne par document réel (ou une ligne vide). */
    private void renderDocuments(List<DocumentConformite> documents) {
        docsBox.getChildren().clear();
        if (documents == null || documents.isEmpty()) {
            Label vide = new Label("Aucun document enregistré");
            vide.getStyleClass().addAll("fs-12", "c-s500");
            HBox.setHgrow(vide, Priority.ALWAYS);
            docsBox.getChildren().add(new HBox(12, vide, Ui.pill("—", "gray")));
            return;
        }
        for (DocumentConformite document : documents) {
            docsBox.getChildren().add(documentRow(document));
        }
    }

    private HBox documentRow(DocumentConformite document) {
        String titre = document.getType() == null ? "Document" : nvl(document.getType().getLibelle());
        if (document.getCategorie() != null && !document.getCategorie().isBlank()) {
            titre += " catégorie " + document.getCategorie();
        }
        String expiration = document.getDateExpiration() == null
                ? "—" : DATE.format(document.getDateExpiration());
        String detail = "N° " + nvl(document.getNumero()) + " · expiration " + expiration;

        StatutConformite statut;
        try {
            statut = ma.translogix.AppContext.conformiteService().statut(document);
        } catch (RuntimeException e) {
            statut = null;   // document sans échéance exploitable
        }
        String cls, libelle, style;
        if (statut == StatutConformite.VALIDE) {
            cls = "green"; libelle = "Valide"; style = "doc-ok";
        } else if (statut == StatutConformite.A_RENOUVELER) {
            cls = "amber"; libelle = "À renouveler"; style = "doc-warn";
        } else if (statut == StatutConformite.EXPIRE) {
            cls = "red"; libelle = "Expiré"; style = "doc-warn";
        } else {
            cls = "gray"; libelle = "—"; style = "doc-ok";
        }

        Label titreLabel = new Label(titre);
        titreLabel.getStyleClass().addAll("fs-12", "fw-b", "c-s800");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().addAll("fs-11", "c-s500", "pt-05");
        VBox texte = new VBox(titreLabel, detailLabel);
        HBox.setHgrow(texte, Priority.ALWAYS);

        HBox row = new HBox(12, texte, Ui.pill(libelle, cls));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(style);
        return row;
    }

    @FXML private void requestAttestation() {
        Employe chauffeur = EspaceChauffeurUi.get().chauffeur();
        String nom = chauffeur == null ? "—" : nvl(chauffeur.getNomComplet());
        EspaceChauffeurUi.get().toast("Demande d'attestation de travail transmise aux RH pour " + nom + ".");
    }

    private static String nvl(String s) { return s == null || s.isBlank() ? "—" : s; }

    private static String initiales(Employe e) {
        String prenom = e.getPrenom() == null ? "" : e.getPrenom().trim();
        String nom = e.getNom() == null ? "" : e.getNom().trim();
        String initiales = (prenom.isEmpty() ? "" : prenom.substring(0, 1))
                + (nom.isEmpty() ? "" : nom.substring(0, 1));
        return initiales.isEmpty() ? "—" : initiales.toUpperCase(Locale.FRENCH);
    }
}
