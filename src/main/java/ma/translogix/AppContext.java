package ma.translogix;

import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.service.UtilisateurService;

/**
 * Builds the services once for the whole app.
 * When the JDBC DAOs exist, swap InMemory... for Jdbc... here and nowhere else.
 */
public final class AppContext {
    private static final UtilisateurService UTILISATEUR_SERVICE =
            new UtilisateurService(new InMemoryUtilisateurDao());

    static {
        // Dev seed data only: one manager account so the screens are not empty.
        UTILISATEUR_SERVICE.creer("Principal", "Admin", "admin@translogix.ma", "admin1234", Role.GERANT);
    }

    private AppContext() {
    }

    public static UtilisateurService utilisateurService() {
        return UTILISATEUR_SERVICE;
    }
}
