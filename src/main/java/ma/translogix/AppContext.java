package ma.translogix;

import java.time.Clock;
import java.time.LocalDate;
import ma.translogix.dao.EmployeDao;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.Role;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDocument;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.AuditService;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.DashboardService;
import ma.translogix.service.DemoDashboardService;
import ma.translogix.service.EmployeService;
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
    private static final DashboardService DASHBOARD_SERVICE =
            new DemoDashboardService(AUDIT_SERVICE, Clock.systemDefaultZone());

    // RH module ([M2], [M4]): in memory until the JDBC DAOs of [M6] exist.
    private static final EmployeDao EMPLOYE_DAO = new InMemoryEmployeDao();
    private static final EmployeService EMPLOYE_SERVICE = new EmployeService(EMPLOYE_DAO, Clock.systemDefaultZone());
    private static final ConformiteService CONFORMITE_SERVICE =
            new ConformiteService(new InMemoryDocumentConformiteDao(), EMPLOYE_DAO, Clock.systemDefaultZone());

    /** False while the seed runs. This class loads lazily, often after login, so the session alone is not enough. */
    private static boolean demarrageTermine;

    static {
        // Dev seed data only: one manager account so the screens are not empty.
        // It is a real creation, so the audit log shows it once, done by "Système".
        UTILISATEUR_SERVICE.creer("Principal", "Admin", "admin@translogix.ma", "admin1234", Role.GERANT);
        semerRh(LocalDate.now());
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

    public static DashboardService dashboardService() {
        return DASHBOARD_SERVICE;
    }

    public static EmployeService employeService() {
        return EMPLOYE_SERVICE;
    }

    public static ConformiteService conformiteService() {
        return CONFORMITE_SERVICE;
    }

    /**
     * Demo staff from rh.html (Majda's 4 sample people). Names, posts, hiring dates and licence numbers come
     * from the mockup; CIN and CNSS are obviously fictional (ZZ-..., 9000...). Expiry dates are relative to
     * today so the demo looks right on any day: one expired licence, one FIMC card within 30 days, the rest valid.
     * Mohammed Bennani is linked to a demo driver account for the future "Mes ordres de transport" page ([O3]).
     */
    private static void semerRh(LocalDate aujourdhui) {
        Employe bennani = EMPLOYE_SERVICE.creer("Bennani", "Mohammed", "ZZ-000101", "900000101", Poste.CHAUFFEUR,
                TypeContrat.CDI, LocalDate.of(2021, 3, 15));
        Employe elAmrani = EMPLOYE_SERVICE.creer("El Amrani", "Abderrahim", "ZZ-000102", "900000102",
                Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2022, 6, 1));
        Employe amrani = EMPLOYE_SERVICE.creer("Amrani", "Youssef", "ZZ-000103", "900000103", Poste.CHAUFFEUR,
                TypeContrat.CDD, LocalDate.of(2020, 1, 10));
        Employe bouazzaoui = EMPLOYE_SERVICE.creer("Bouazzaoui", "Karima", "ZZ-000104", "900000104",
                Poste.ADMINISTRATIF, TypeContrat.CDI, LocalDate.of(2023, 2, 1));

        CONFORMITE_SERVICE.renouveler(bennani.getId(), TypeDocument.PERMIS, "PRM-883920", "EC", aujourdhui.plusYears(2));
        CONFORMITE_SERVICE.renouveler(bennani.getId(), TypeDocument.FIMC, "FIMC-20931", null, aujourdhui.plusMonths(20));
        CONFORMITE_SERVICE.renouveler(bennani.getId(), TypeDocument.VISITE_MEDICALE, null, null, aujourdhui.plusMonths(12));

        CONFORMITE_SERVICE.renouveler(elAmrani.getId(), TypeDocument.PERMIS, "PRM-112938", "C", aujourdhui.minusDays(12));
        CONFORMITE_SERVICE.renouveler(elAmrani.getId(), TypeDocument.FIMC, "FIMC-31877", null, aujourdhui.plusMonths(14));
        CONFORMITE_SERVICE.renouveler(elAmrani.getId(), TypeDocument.VISITE_MEDICALE, null, null, aujourdhui.plusMonths(7));

        CONFORMITE_SERVICE.renouveler(amrani.getId(), TypeDocument.PERMIS, "PRM-445201", "EC", aujourdhui.plusMonths(9));
        CONFORMITE_SERVICE.renouveler(amrani.getId(), TypeDocument.FIMC, "FIMC-17420", null, aujourdhui.plusDays(20));
        CONFORMITE_SERVICE.renouveler(amrani.getId(), TypeDocument.VISITE_MEDICALE, null, null, aujourdhui.plusMonths(5));

        CONFORMITE_SERVICE.renouveler(bouazzaoui.getId(), TypeDocument.PERMIS, "PRM-992011", "B", aujourdhui.plusYears(4));

        Utilisateur compteChauffeur = UTILISATEUR_SERVICE.creer("Bennani", "Mohammed", "m.bennani@translogix.ma",
                "chauffeur123", Role.CHAUFFEUR);
        EMPLOYE_SERVICE.lierCompte(bennani.getId(), compteChauffeur.getId());
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
