package ma.translogix;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import ma.translogix.dao.AuditDao;
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.dao.EmployeDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.dao.VehiculeDao;
import ma.translogix.dao.DepenseDao;
import ma.translogix.dao.jdbc.JdbcAuditDao;
import ma.translogix.dao.jdbc.JdbcDepenseDao;
import ma.translogix.dao.jdbc.JdbcDocumentConformiteDao;
import ma.translogix.dao.jdbc.JdbcEmployeDao;
import ma.translogix.dao.jdbc.JdbcUtilisateurDao;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryCongeDao;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryDepenseDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryPaieDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDocument;
import ma.translogix.model.Utilisateur;
import ma.translogix.model.Vehicule;
import ma.translogix.service.AuditService;
import ma.translogix.service.AuthService;
import ma.translogix.service.ClientService;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.CongeService;
import ma.translogix.service.DepenseService;
import ma.translogix.service.FactureService;
import ma.translogix.service.PaieService;
import ma.translogix.service.TresorerieService;
import ma.translogix.service.MissionService;
import ma.translogix.service.DashboardService;
import ma.translogix.service.EmployeService;
import ma.translogix.service.EspaceChauffeurService;
import ma.translogix.service.RapportService;
import ma.translogix.service.StatistiquesDashboardService;
import ma.translogix.service.UtilisateurService;
import ma.translogix.service.VehiculeService;
import ma.translogix.util.Session;

/**
 * Builds the services once for the whole app.
 * Persistance decides the mode at startup: in MySQL mode every module that has a JDBC DAO uses it, the others stay
 * in memory until their JDBC DAO is delivered (then pick it here, the same way, and nowhere else).
 */
public final class AppContext {
    /** Must stay the first field: the DAOs below are chosen from it. */
    private static final Persistance.Etat PERSISTANCE = Persistance.etat();

    // Admin module: user accounts and audit log are in MySQL when the database is available.
    private static final AuditDao AUDIT_DAO = PERSISTANCE.connexion()
            .<AuditDao>map(JdbcAuditDao::new).orElseGet(InMemoryAuditDao::new);
    private static final AuditService AUDIT_SERVICE =
            new AuditService(AUDIT_DAO, Clock.systemDefaultZone(), AppContext::acteurCourant);
    private static final UtilisateurDao UTILISATEUR_DAO = PERSISTANCE.connexion()
            .<UtilisateurDao>map(JdbcUtilisateurDao::new).orElseGet(InMemoryUtilisateurDao::new);
    private static final UtilisateurService UTILISATEUR_SERVICE =
            new UtilisateurService(UTILISATEUR_DAO, AUDIT_SERVICE);
    private static final AuthService AUTH_SERVICE = new AuthService(UTILISATEUR_DAO);

    // RH module ([M2], [M4]): employees and compliance documents in MySQL when the database is available
    // (Persistance: DB_URL set and reachable), otherwise in memory. Leave requests stay in memory for now.
    private static final EmployeDao EMPLOYE_DAO = PERSISTANCE.connexion()
            .<EmployeDao>map(JdbcEmployeDao::new).orElseGet(InMemoryEmployeDao::new);
    private static final DocumentConformiteDao DOCUMENT_CONFORMITE_DAO = PERSISTANCE.connexion()
            .<DocumentConformiteDao>map(JdbcDocumentConformiteDao::new).orElseGet(InMemoryDocumentConformiteDao::new);
    private static final EmployeService EMPLOYE_SERVICE = new EmployeService(EMPLOYE_DAO, Clock.systemDefaultZone());
    private static final ConformiteService CONFORMITE_SERVICE =
            new ConformiteService(DOCUMENT_CONFORMITE_DAO, EMPLOYE_DAO, Clock.systemDefaultZone());
    private static final CongeService CONGE_SERVICE =
            new CongeService(new InMemoryCongeDao(), EMPLOYE_SERVICE, Clock.systemDefaultZone());
    // Comptable module ([C2], [C3]): in memory until the JDBC DAOs of [C5] exist.
    private static final InMemoryClientDao CLIENT_DAO = new InMemoryClientDao();
    private static final FactureDao FACTURE_DAO = new InMemoryFactureDao();
    private static final ClientService CLIENT_SERVICE = new ClientService(CLIENT_DAO, FACTURE_DAO);
    // Dispatcher module ([H2]-[H4]): in memory until the JDBC DAOs of [H6] exist.
    private static final VehiculeDao VEHICULE_DAO = new InMemoryVehiculeDao();
    private static final VehiculeService VEHICULE_SERVICE = new VehiculeService(VEHICULE_DAO);
    private static final MissionService MISSION_SERVICE = new MissionService(new InMemoryMissionDao(), VEHICULE_DAO,
            EMPLOYE_SERVICE, CLIENT_SERVICE, Clock.systemDefaultZone());
    // Invoices need MissionService: only a delivered mission is invoiced ([C3]).
    private static final FactureService FACTURE_SERVICE = new FactureService(FACTURE_DAO, CLIENT_DAO,
            new InMemoryPaiementDao(), MISSION_SERVICE, Clock.systemDefaultZone());
    // Comptable expenses: driver fuel/toll vouchers. The depenses table exists, so this is in MySQL when available.
    private static final DepenseDao DEPENSE_DAO = PERSISTANCE.connexion()
            .<DepenseDao>map(JdbcDepenseDao::new).orElseGet(InMemoryDepenseDao::new);
    private static final DepenseService DEPENSE_SERVICE = new DepenseService(DEPENSE_DAO, AUDIT_SERVICE,
            MISSION_SERVICE, EMPLOYE_SERVICE, Clock.systemDefaultZone(),
            () -> Session.estConnecte() ? Session.utilisateur().getEmail() : "Inconnu");
    // Payroll: no JDBC DAO / table yet, so in memory (lost on restart) — wired here, not in the controller.
    private static final PaieService PAIE_SERVICE = new PaieService(new InMemoryPaieDao(), EMPLOYE_SERVICE,
            AUDIT_SERVICE, Clock.systemDefaultZone());
    // Treasury: read-only aggregation over invoices and clients, no DAO of its own.
    private static final TresorerieService TRESORERIE_SERVICE =
            new TresorerieService(FACTURE_SERVICE, CLIENT_SERVICE, Clock.systemDefaultZone());
    // Driver space ([O3]): the logged-in account's own missions.
    private static final EspaceChauffeurService ESPACE_CHAUFFEUR_SERVICE = new EspaceChauffeurService(MISSION_SERVICE,
            EMPLOYE_DAO, () -> Session.utilisateur().getId());
    // Gérant dashboard: real figures read from the services above, so it must be built after them.
    private static final DashboardService DASHBOARD_SERVICE = new StatistiquesDashboardService(MISSION_SERVICE,
            VEHICULE_SERVICE, FACTURE_SERVICE, CONFORMITE_SERVICE, EMPLOYE_SERVICE, AUDIT_SERVICE, Clock.systemDefaultZone());
    // Reports page: filtered reports read from the services above, so it must be built after them.
    private static final RapportService RAPPORT_SERVICE = new RapportService(UTILISATEUR_SERVICE, FACTURE_SERVICE,
            MISSION_SERVICE, CLIENT_SERVICE, VEHICULE_SERVICE, EMPLOYE_SERVICE, CONFORMITE_SERVICE,
            Clock.systemDefaultZone());

    /** False while the seed runs. This class loads lazily, often after login, so the session alone is not enough. */
    private static boolean demarrageTermine;

    static {
        // Dev seed data only. Accounts: only when there is none yet (ComptesDemo), so a restart in MySQL mode never
        // recreates or resets them. The other modules are still in memory, so they are seeded at every start.
        ComptesDemo.semer(UTILISATEUR_DAO, UTILISATEUR_SERVICE);
        semerRh(LocalDate.now());
        semerClients();
        LocalDateTime maintenant = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        semerDispatcher(maintenant);
        semerFactures(maintenant.toLocalDate());
        demarrageTermine = true;
    }

    private AppContext() {
    }

    public static UtilisateurService utilisateurService() {
        return UTILISATEUR_SERVICE;
    }

    public static AuthService authService() {
        return AUTH_SERVICE;
    }

    public static EspaceChauffeurService espaceChauffeurService() {
        return ESPACE_CHAUFFEUR_SERVICE;
    }

    /** How this run stores its data (MySQL, memory, or memory because MySQL was unreachable). */
    public static Persistance.Etat persistance() {
        return PERSISTANCE;
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

    public static CongeService congeService() {
        return CONGE_SERVICE;
    }

    /**
     * Demo staff from rh.html (Majda's 4 sample people). Names, posts, hiring dates and licence numbers come
     * from the mockup; CIN and CNSS are obviously fictional (ZZ-..., 9000...). Expiry dates are relative to
     * today so the demo looks right on any day: one expired licence, one FIMC card within 30 days, the rest valid.
     * Mohammed Bennani is linked to a demo driver account for the future "Mes ordres de transport" page ([O3]).
     */
    private static void semerRh(LocalDate aujourdhui) {
        // With MySQL the staff survives restarts: seed it only into an empty table, like ComptesDemo. Replaying it
        // would reset every document the RH user edited, and stop the app if a demo employee was deactivated
        // (renouveler refuses a deactivated employee).
        if (!EMPLOYE_SERVICE.lister().isEmpty()) {
            return;
        }
        Employe bennani = employeOuCree("Bennani", "Mohammed", "ZZ-000101", "900000101", Poste.CHAUFFEUR,
                TypeContrat.CDI, LocalDate.of(2021, 3, 15));
        Employe elAmrani = employeOuCree("El Amrani", "Abderrahim", "ZZ-000102", "900000102",
                Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2022, 6, 1));
        Employe amrani = employeOuCree("Amrani", "Youssef", "ZZ-000103", "900000103", Poste.CHAUFFEUR,
                TypeContrat.CDD, LocalDate.of(2020, 1, 10));
        Employe bouazzaoui = employeOuCree("Bouazzaoui", "Karima", "ZZ-000104", "900000104",
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

        // The driver account may come from an earlier start (MySQL mode), or have been deleted by the Gérant.
        UTILISATEUR_DAO.findByEmail(ComptesDemo.EMAIL_CHAUFFEUR)
                .ifPresent(compte -> EMPLOYE_SERVICE.lierCompte(bennani.getId(), compte.getId()));
    }

    /** The RH seed is replayed on every start: with MySQL, the employees may already exist from a previous run. */
    private static Employe employeOuCree(String nom, String prenom, String cin, String cnss, Poste poste,
                                         TypeContrat typeContrat, LocalDate dateEmbauche) {
        return EMPLOYE_SERVICE.lister().stream()
                .filter(e -> cin.equals(e.getCin()))
                .findFirst()
                .orElseGet(() -> EMPLOYE_SERVICE.creer(nom, prenom, cin, cnss, poste, typeContrat, dateEmbauche));
    }

    public static VehiculeService vehiculeService() {
        return VEHICULE_SERVICE;
    }

    public static MissionService missionService() {
        return MISSION_SERVICE;
    }

    public static ClientService clientService() {
        return CLIENT_SERVICE;
    }

    public static FactureService factureService() {
        return FACTURE_SERVICE;
    }

    public static DepenseService depenseService() {
        return DEPENSE_SERVICE;
    }

    public static PaieService paieService() {
        return PAIE_SERVICE;
    }

    public static TresorerieService tresorerieService() {
        return TRESORERIE_SERVICE;
    }

    /** Demo clients from contable.html (names, ICE, phones). */
    private static void semerClients() {
        CLIENT_SERVICE.creer("LogiMaroc SARL", "001525487000012", "0522 45 18 90",
                null, "Zone industrielle Ain Sebaa, Casablanca", Client.DELAI_PAR_DEFAUT);
        CLIENT_SERVICE.creer("Atlas Prime Transports", "002136985000047", "0537 72 64 11",
                null, "Avenue Hassan II, Rabat", Client.DELAI_PAR_DEFAUT);
        CLIENT_SERVICE.creer("Souss Express Logistique", "001874520000031", "0528 82 37 05",
                null, "Quartier industriel Tassila, Agadir", Client.DELAI_PAR_DEFAUT);
        CLIENT_SERVICE.creer("Maroc Primeurs Agadir", null, "0528 23 90 14",
                null, "Marché de gros, Inezgane", Client.DELAI_PAR_DEFAUT);
    }

    /**
     * Demo invoices from contable.html (TTC amounts), each one for a past mission delivered just before it
     * (TRJ-9085 to TRJ-9088, created and delivered here, then FACTUREE): an invoice always comes from a
     * delivered mission ([C3]). Dates are relative to today: one paid and one pending invoice this month
     * (the monthly revenue KPI is never 0) and two unpaid invoices past their due date (overdue).
     * HT amounts are chosen so that HT + 14 % VAT gives the mockup's TTC exactly.
     * TRJ-9082 (delivered 15 days ago) is left without an invoice, ready for the demo.
     */
    private static void semerFactures(LocalDate aujourdhui) {
        int jourDuMois = aujourdhui.getDayOfMonth();
        LocalDate debutDeMois = aujourdhui.minusDays(Math.min(5, jourDuMois - 1)); // stays in this month
        LocalDate hier = aujourdhui.minusDays(Math.min(1, jourDuMois - 1));

        Long m1 = missionLivree("LogiMaroc SARL", "12345-A-6", "Bennani", "Casablanca", "Tanger Med",
                "Produits frais", "24", debutDeMois.minusDays(1).atTime(6, 0), 8);
        Long m2 = missionLivree("Atlas Prime Transports", "67890-B-2", "El Amrani", "Casablanca", "Agadir",
                "Conteneurs", "22", aujourdhui.minusDays(76).atTime(6, 0), 10);
        Long m3 = missionLivree("Souss Express Logistique", "45678-C-1", "Amrani", "Agadir", "Casablanca",
                "Primeurs", "20", aujourdhui.minusDays(93).atTime(6, 0), 9);
        Long m4 = missionLivree("Maroc Primeurs Agadir", "11223-D-8", "Amrani", "Agadir", "Casablanca",
                "Agrumes", "25", hier.minusDays(1).atTime(5, 0), 7);

        // FACT-...-001 paid, 002 and 003 overdue (45 and 62 days late), 004 pending: as in the mockup.
        Facture payee = FACTURE_SERVICE.creer(m1, new BigDecimal("21052.63"), null, debutDeMois);
        FACTURE_SERVICE.creer(m2, new BigDecimal("39473.68"), null, aujourdhui.minusDays(75));
        FACTURE_SERVICE.creer(m3, new BigDecimal("17894.74"), null, aujourdhui.minusDays(92));
        FACTURE_SERVICE.creer(m4, new BigDecimal("33333.33"), null, hier);
        FACTURE_SERVICE.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-882910", aujourdhui);
    }

    /** A past mission created, started and delivered through MissionService (so it obeys the overlap rule). */
    private static Long missionLivree(String client, String matricule, String chauffeur, String depart,
                                      String arrivee, String cargaison, String poids, LocalDateTime debut,
                                      int heures) {
        Long vehicule = VEHICULE_DAO.findByMatricule(matricule).orElseThrow().getId();
        Mission m = MISSION_SERVICE.creer(client(client), vehicule, chauffeur(chauffeur), depart, arrivee,
                cargaison, new BigDecimal(poids), debut, debut.plusHours(heures));
        MISSION_SERVICE.demarrer(m.getId());
        MISSION_SERVICE.livrer(m.getId());
        return m.getId();
    }

    /**
     * Demo fleet and missions from dispatcher.html (Hamid's sample rows). Missions go through MissionService, so
     * the seed obeys the no-overlap rule; drivers are the RH seed drivers, clients the comptable seed clients.
     * Times are relative to now so the demo looks right at any hour: TRJ-9081 on the road, TRJ-9082 delivered
     * 15 days ago (before El Amrani's licence expired), TRJ-9084 loading today and TRJ-9083 tomorrow 09:00.
     * TRJ-9084 goes to Youssef Amrani (the mockup's Karim El Idrissi is not an RH seed employee).
     */
    private static void semerDispatcher(LocalDateTime maintenant) {
        Vehicule volvo = VEHICULE_SERVICE.creer("12345-A-6", "Volvo", "FH", new BigDecimal("26"));
        Vehicule scania = VEHICULE_SERVICE.creer("67890-B-2", "Scania", "R500", new BigDecimal("26"));
        Vehicule man = VEHICULE_SERVICE.creer("45678-C-1", "MAN", "TGX", new BigDecimal("32"));
        Vehicule daf = VEHICULE_SERVICE.creer("11223-D-8", "DAF", "XF", new BigDecimal("30"));
        Vehicule atelier = VEHICULE_SERVICE.creer("98765-B-2", "Scania", "R450", new BigDecimal("26"));
        VEHICULE_SERVICE.changerStatut(atelier.getId(), StatutVehicule.MAINTENANCE);

        Long bennani = chauffeur("Bennani");
        Long elAmrani = chauffeur("El Amrani");
        Long amrani = chauffeur("Amrani");

        Mission m9081 = MISSION_SERVICE.creer(client("LogiMaroc SARL"), volvo.getId(), bennani, "Casablanca",
                "Tanger Med", "Produits frais", new BigDecimal("24"), maintenant.minusHours(4),
                maintenant.plusHours(2).plusMinutes(30));
        Mission m9082 = MISSION_SERVICE.creer(client("Atlas Prime Transports"), scania.getId(), elAmrani, "Agadir",
                "Marrakech", "Agrumes", new BigDecimal("18"), maintenant.minusDays(15).minusHours(5),
                maintenant.minusDays(15).minusMinutes(45));
        LocalDateTime demain9h = maintenant.toLocalDate().plusDays(1).atTime(9, 0);
        MISSION_SERVICE.creer(client("Souss Express Logistique"), man.getId(), amrani, "Rabat", "Fès",
                "Matériaux const.", new BigDecimal("30"), demain9h, demain9h.plusHours(5));
        MISSION_SERVICE.creer(client("Maroc Primeurs Agadir"), daf.getId(), amrani, "Jorf Lasfar", "Casablanca",
                "Engrais", new BigDecimal("28"), maintenant.plusHours(1), maintenant.plusHours(4));

        MISSION_SERVICE.demarrer(m9081.getId());
        MISSION_SERVICE.demarrer(m9082.getId());
        MISSION_SERVICE.livrer(m9082.getId());
    }

    private static Long chauffeur(String nom) {
        return EMPLOYE_SERVICE.listerChauffeurs().stream().filter(e -> e.getNom().equals(nom))
                .findFirst().orElseThrow().getId();
    }

    private static Long client(String nom) {
        return CLIENT_SERVICE.lister().stream().filter(c -> c.getNom().equals(nom)).findFirst().orElseThrow().getId();
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
