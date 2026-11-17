package ma.translogix.controller.common;

import java.util.List;
import java.util.Optional;
import ma.translogix.model.Role;
import ma.translogix.util.Navigator;

/**
 * Sidebar entries per role. Each member adds the entries for THEIR role, in their own PR.
 * An entry whose FXML file does not exist yet stays clickable: Navigator shows "Module pas encore livré".
 */
public final class NavigationMenu {

    public static final String RACINE = Navigator.RACINE;

    /**
     * One sidebar entry.
     * @param kicker small text above the page title in the top bar (null: the role name)
     * @param icone  SVG path (24x24 box) drawn left of the label (null: no icon)
     * @param demo   true when the page shows demo values: the top bar then says so
     */
    public record Entree(String libelle, String fxml, String kicker, String icone, boolean demo) {
        /** Short form for members who do not need kicker, icon or demo flag. */
        public Entree(String libelle, String fxml) {
            this(libelle, fxml, null, null, false);
        }
    }

    // Simplified 24x24 drawings of the Font Awesome icons used in admine.html.
    static final String ICONE_TABLEAU = "M11 3v10h10A10 10 0 1 1 11 3z M13 1a10 10 0 0 1 10 10H13z";
    static final String ICONE_UTILISATEUR = "M12 2a5 5 0 1 1 0 10a5 5 0 1 1 0-10z M3 22c0-5 4-8 9-8s9 3 9 8z";
    static final String ICONE_EQUIPE = "M8 3a4 4 0 1 1 0 8a4 4 0 1 1 0-8z M1 21c0-4 3-7 7-7s7 3 7 7z "
            + "M17 5a3.5 3.5 0 1 1 0 7a3.5 3.5 0 1 1 0-7z M16.5 14c4 0 6.5 3 6.5 7h-6c0-2.6-1-4.8-2.8-6.4z";
    static final String ICONE_PORTEFEUILLE = "M2 7a3 3 0 0 1 3-3h14v3z M2 8h20v12H5a3 3 0 0 1-3-3z M16 12v4h4v-4z";
    static final String ICONE_CAMION = "M1 5h13v10H1z M14 8h4l4 4v3h-8z M3 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0z "
            + "M15 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0z";
    static final String ICONE_COLONNES = "M3 3h2v16h16v2H3z M7 11h3v6H7z M12 7h3v10h-3z M17 13h3v4h-3z";
    static final String ICONE_BOUCLIER = "M12 2l9 3.5v6c0 5.2-3.8 9.3-9 10.5c-5.2-1.2-9-5.3-9-10.5v-6z";
    // RH icons (rh.html): id card, calendar, wallet, user-plus. Holes are drawn anticlockwise (non-zero fill).
    static final String ICONE_CARTE = "M2 5h20v14H2z M5 8v8h6V8z M13 9v2h6V9z M13 13v2h5v-2z";
    static final String ICONE_CALENDRIER = "M3 5h18v16H3z M5 10v9h14v-9z M7 2h2v4H7z M15 2h2v4h-2z M7 12h3v3H7z";
    static final String ICONE_RECRUTEMENT = "M9 3a4 4 0 1 1 0 8a4 4 0 1 1 0-8z M2 21c0-4 3-7 7-7s7 3 7 7z "
            + "M18 7h2v3h3v2h-3v3h-2v-3h-3v-2h3z";

    private NavigationMenu() {
    }

    public static List<Entree> pour(Role role) {
        return switch (role) {
            // Gerant menu, from admine.html. Entries pointing to other modules open
            // automatically when their owners add the FXML file.
            case GERANT -> List.of(
                    new Entree("Tableau de bord", "admin/dashboard.fxml", "Vue générale", ICONE_TABLEAU, true),
                    new Entree("Utilisateurs", "admin/utilisateurs.fxml", "Administration", ICONE_UTILISATEUR, false),
                    new Entree("Ressources humaines", "rh/employes.fxml", "Équipe & conformité", ICONE_EQUIPE, false),
                    new Entree("Comptabilité & finance", "comptable/factures.fxml", "Suivi financier",
                            ICONE_PORTEFEUILLE, false),
                    new Entree("Dispatching & flotte", "dispatcher/missions.fxml", "Opérations logistiques",
                            ICONE_CAMION, false),
                    new Entree("Rapports & analytics", "admin/rapports.fxml", "Pilotage & indicateurs",
                            ICONE_COLONNES, false),
                    new Entree("Journal d'audit", "admin/audit.fxml", "Sécurité & traçabilité", ICONE_BOUCLIER, false));
            // RH menu, in rh.html order. The last three pages are Iteration 3: DÉMO data, actions not delivered.
            case RH -> List.of(
                    new Entree("Tableau de bord RH", "rh/dashboard.fxml", "Vue synthétique", ICONE_TABLEAU, false),
                    new Entree("Chauffeurs & employés", "rh/employes.fxml", "Effectif & registre du personnel",
                            ICONE_CARTE, false),
                    new Entree("Conformité & permis", "rh/conformite.fxml", "Sécurité & réglementation transport",
                            ICONE_BOUCLIER, false),
                    new Entree("Congés & absences", "rh/conges.fxml", "Suivi du temps de travail",
                            ICONE_CALENDRIER, true),
                    new Entree("Paie & prime trajet", "rh/primes.fxml", "Rémunération & primes logistiques",
                            ICONE_PORTEFEUILLE, true),
                    new Entree("Recrutement & onboarding", "rh/recrutement.fxml", "Pipeline RH & intégration",
                            ICONE_RECRUTEMENT, true));
            default -> List.of(); // other roles: added by their owner
        };
    }

    /**
     * The sidebar entry to highlight for a page: the entry with that exact file,
     * otherwise the first entry in the same folder (rh/conformite.fxml -> "Ressources humaines").
     */
    public static Optional<Entree> entreePour(Role role, String fxml) {
        List<Entree> entrees = pour(role);
        Optional<Entree> exacte = entrees.stream().filter(e -> e.fxml().equals(fxml)).findFirst();
        if (exacte.isPresent()) {
            return exacte;
        }
        String dossier = dossier(fxml);
        if (dossier.isEmpty()) {
            return Optional.empty();
        }
        return entrees.stream().filter(e -> dossier(e.fxml()).equals(dossier)).findFirst();
    }

    public static String sousTitre(Role role) {
        return switch (role) {
            case GERANT -> "ERP LOGISTIQUE · MAROC";
            case RH -> "PORTAIL RH";
            default -> "TRANSLOGIX";
        };
    }

    private static String dossier(String fxml) {
        int slash = fxml == null ? -1 : fxml.lastIndexOf('/');
        return slash < 0 ? "" : fxml.substring(0, slash);
    }
}
