package ma.translogix;

import java.time.Clock;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.AuditService;
import ma.translogix.service.RapportService;
import ma.translogix.service.UtilisateurService;
import ma.translogix.util.Session;

/**
 * Builds the services once for the whole app.
 * When the JDBC DAOs exist, swap InMemory... for Jdbc... here and nowhere else.
 */
public final class AppContext {
    private static final AuditService AUDIT_SERVICE =
            new AuditService(new InMemoryAuditDao(), Clock.systemDefaultZone(), AppContext::acteurCourant);
    private static final UtilisateurService UTILISATEUR_SERVICE =
            new UtilisateurService(new InMemoryUtilisateurDao(), AUDIT_SERVICE);
    private static final RapportService RAPPORT_SERVICE = new RapportService(UTILISATEUR_SERVICE);

    /** False while the seed runs. This class loads lazily, often after login, so the session alone is not enough. */
    private static boolean demarrageTermine;

    static {
        // Dev seed data only: one manager account so the screens are not empty.
        // It is a real creation, so the audit log shows it once, done by "Système".
        UTILISATEUR_SERVICE.creer("Principal", "Admin", "admin@translogix.ma", "admin1234", Role.GERANT);
        demarrageTermine = true;
    }

    private AppContext() {
    }

    public static UtilisateurService utilisateurService() {
        return UTILISATEUR_SERVICE;
    }

    public static AuditService auditService() {
        return AUDIT_SERVICE;
    }

    public static RapportService rapportService() {
        return RAPPORT_SERVICE;
    }

    /** Who the audit log names: the logged-in user, or "Système" for actions done at startup. */
    private static String acteurCourant() {
        if (!demarrageTermine || !Session.estConnecte()) {
            return "Système";
        }
        Utilisateur u = Session.utilisateur();
        return u.getNomComplet() + " (" + u.getRole().getLibelle() + ")";
    }
}
