package ma.translogix.controller.common;

import java.util.List;
import ma.translogix.model.Role;

/**
 * Sidebar entries per role. Each member adds the entries for THEIR role, in their own PR.
 * A button whose FXML file does not exist yet is shown disabled.
 */
public final class NavigationMenu {

    public static final String RACINE = "/ma/translogix/fxml/";

    public record Entree(String libelle, String fxml) {
    }

    private NavigationMenu() {
    }

    public static List<Entree> pour(Role role) {
        return switch (role) {
            // Gerant menu, from admine.html. Entries pointing to other modules turn on
            // automatically when their owners add the FXML file.
            case GERANT -> List.of(
                    new Entree("Tableau de bord", "admin/dashboard.fxml"),
                    new Entree("Utilisateurs", "admin/utilisateurs.fxml"),
                    new Entree("Ressources humaines", "rh/employes.fxml"),
                    new Entree("Comptabilité & finance", "comptable/factures.fxml"),
                    new Entree("Dispatching & flotte", "dispatcher/missions.fxml"),
                    new Entree("Rapports & analytics", "admin/rapports.fxml"),
                    new Entree("Journal d'audit", "admin/audit.fxml"));
            default -> List.of(); // other roles: added by their owner
        };
    }

    public static String sousTitre(Role role) {
        return switch (role) {
            case GERANT -> "ERP LOGISTIQUE · MAROC";
            default -> "TRANSLOGIX";
        };
    }
}
