package ma.translogix.service;

import java.util.List;
import ma.translogix.model.Utilisateur;

/**
 * Builds the reports of the "Rapports & analytics" page.
 * Only the staff report uses real data (users). The other two are DEMO values taken from
 * admine.html until the comptable and dispatcher modules exist.
 */
public class RapportService {
    private final UtilisateurService utilisateurs;

    public RapportService(UtilisateurService utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    /** DEMO: values from the mockup. */
    public Rapport rentabilite() {
        return new Rapport("Rentabilité mensuelle", "rapport-rentabilite-DEMO.csv",
                List.of("Indicateur", "Valeur"),
                List.of(List.of("Chiffre d'affaires (MAD)", "452000"),
                        List.of("Créances clients (MAD)", "89400"),
                        List.of("Variation mensuelle", "14 %")),
                true);
    }

    /** DEMO: values from the mockup. */
    public Rapport gazole() {
        return new Rapport("Consommation de gazole", "rapport-gazole-DEMO.csv",
                List.of("Trajet", "Chauffeur", "Litres", "Montant MAD"),
                List.of(List.of("TRJ-9081", "Mohammed Bennani", "120", "1500")),
                true);
    }

    /** REAL: staff accounts. Licence columns will be added when the RH module exists. */
    public Rapport conformiteRh() {
        List<List<String>> lignes = utilisateurs.lister().stream()
                .map(RapportService::ligne)
                .toList();
        return new Rapport("Conformité RH", "rapport-personnel.csv",
                List.of("Nom", "Prénom", "Email", "Rôle", "Statut"), lignes, false);
    }

    private static List<String> ligne(Utilisateur u) {
        return List.of(u.getNom(), u.getPrenom(), u.getEmail(), u.getRole().getLibelle(),
                u.isActif() ? "Actif" : "Inactif");
    }
}
