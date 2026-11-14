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
            case GERANT -> List.of(
                    new Entree("Tableau de bord", "admin/dashboard.fxml"),
                    new Entree("Utilisateurs", "admin/utilisateurs.fxml"));
            default -> List.of(); // other roles: added by their owner
        };
    }

    public static String sousTitre(Role role) {
        return switch (role) {
            case GERANT -> "CENTRE DE CONTR\u00d4LE";
            default -> "TRANSLOGIX";
        };
    }
}
