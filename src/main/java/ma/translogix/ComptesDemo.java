package ma.translogix;

import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.service.UtilisateurService;

/**
 * Demo accounts, one per role (DEV PASSWORDS, never in production).
 * They are created only when there is no account at all: at every start in memory mode, and only on the very first
 * start in MySQL mode. After that the database is the truth: a demo account the Gérant changed, deactivated or
 * deleted is never recreated or reset, and a second start never hits the UNIQUE email.
 */
final class ComptesDemo {

    /** The driver account linked to the employee Mohammed Bennani by the RH seed. */
    static final String EMAIL_CHAUFFEUR = "m.bennani@translogix.ma";

    private ComptesDemo() {
    }

    /** @return true when the accounts were created (empty base), false when accounts already existed */
    static boolean semer(UtilisateurDao dao, UtilisateurService service) {
        if (!dao.findAll().isEmpty()) {
            return false;
        }
        // Real creations through the service: hashed passwords, and the audit log shows them once, by "Système".
        service.creer("Principal", "Admin", "admin@translogix.ma", "admin1234", Role.GERANT);
        service.creer("Comptable", "Demo", "comptable@translogix.ma", "comptable1234", Role.COMPTABLE);
        service.creer("RH", "Demo", "rh@translogix.ma", "rh123456", Role.RH);
        service.creer("Dispatcher", "Demo", "dispatcher@translogix.ma", "dispatcher1234", Role.DISPATCHER);
        service.creer("Bennani", "Mohammed", EMAIL_CHAUFFEUR, "chauffeur123", Role.CHAUFFEUR);
        return true;
    }
}
