package ma.translogix;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import ma.translogix.dao.EmployeDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Poste;
import ma.translogix.model.Role;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDocument;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.AuditService;
import ma.translogix.service.ClientService;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.FactureService;
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
    // Comptable module ([C2], [C3]): in memory until the JDBC DAOs of [C5] exist.
    private static final InMemoryClientDao CLIENT_DAO = new InMemoryClientDao();
    private static final FactureDao FACTURE_DAO = new InMemoryFactureDao();
    private static final ClientService CLIENT_SERVICE = new ClientService(CLIENT_DAO, FACTURE_DAO);
    private static final FactureService FACTURE_SERVICE =
            new FactureService(FACTURE_DAO, CLIENT_DAO, new InMemoryPaiementDao(), Clock.systemDefaultZone());

    /** False while the seed runs. This class loads lazily, often after login, so the session alone is not enough. */
    private static boolean demarrageTermine;

    static {
        // Dev seed data only: one manager account so the screens are not empty.
        // It is a real creation, so the audit log shows it once, done by "Système".
        UTILISATEUR_SERVICE.creer("Principal", "Admin", "admin@translogix.ma", "admin1234", Role.GERANT);
        semerRh(LocalDate.now());
        semerComptabilite(LocalDate.now());
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

    public static ClientService clientService() {
        return CLIENT_SERVICE;
    }

    public static FactureService factureService() {
        return FACTURE_SERVICE;
    }

    /**
     * Demo clients and invoices from contable.html (names, numbers, TTC amounts), dated relative to today
     * so the demo looks right on any day: one paid and one pending invoice this month (the monthly
     * revenue KPI is never 0) and two unpaid invoices past their due date (overdue).
     * HT amounts are chosen so that HT + 14 % VAT gives the mockup's TTC exactly.
     */
    private static void semerComptabilite(LocalDate aujourdhui) {
        Client logiMaroc = CLIENT_SERVICE.creer("LogiMaroc SARL", "001525487000012", "0522 45 18 90",
                null, "Zone industrielle Ain Sebaa, Casablanca", Client.DELAI_PAR_DEFAUT);
        Client atlas = CLIENT_SERVICE.creer("Atlas Prime Transports", "002136985000047", "0537 72 64 11",
                null, "Avenue Hassan II, Rabat", Client.DELAI_PAR_DEFAUT);
        Client souss = CLIENT_SERVICE.creer("Souss Express Logistique", "001874520000031", "0528 82 37 05",
                null, "Quartier industriel Tassila, Agadir", Client.DELAI_PAR_DEFAUT);
        Client primeurs = CLIENT_SERVICE.creer("Maroc Primeurs Agadir", null, "0528 23 90 14",
                null, "Marché de gros, Inezgane", Client.DELAI_PAR_DEFAUT);

        int jourDuMois = aujourdhui.getDayOfMonth();
        LocalDate debutDeMois = aujourdhui.minusDays(Math.min(5, jourDuMois - 1)); // stays in this month
        LocalDate hier = aujourdhui.minusDays(Math.min(1, jourDuMois - 1));

        // FACT-...-001 paid, 002 and 003 overdue (45 and 62 days late), 004 pending: as in the mockup.
        Facture payee = FACTURE_SERVICE.creer(logiMaroc.getId(), new BigDecimal("21052.63"), "TRJ-9081", null,
                debutDeMois);
        FACTURE_SERVICE.creer(atlas.getId(), new BigDecimal("39473.68"), "TRJ-9082", null,
                aujourdhui.minusDays(75));
        FACTURE_SERVICE.creer(souss.getId(), new BigDecimal("17894.74"), "TRJ-9083", null,
                aujourdhui.minusDays(92));
        FACTURE_SERVICE.creer(primeurs.getId(), new BigDecimal("33333.33"), "TRJ-9084", null, hier);
        FACTURE_SERVICE.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-882910", aujourdhui);
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
