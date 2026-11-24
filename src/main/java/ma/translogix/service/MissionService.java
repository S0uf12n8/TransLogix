package ma.translogix.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import ma.translogix.dao.CritereMission;
import ma.translogix.dao.MissionDao;
import ma.translogix.dao.VehiculeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/**
 * Missions ([H2], [H4]). The project's core rule: a vehicle or a driver is never on two missions at the same time.
 * Two missions overlap when a.debut < b.fin and b.debut < a.fin, so missions that only touch (end = start) are allowed.
 * Cancelled missions never block anything.
 */
public class MissionService {

    /** One slice of a dispatcher chart: a label and its count, as in the other dashboards. */
    public record Part(String libelle, long nombre) {
    }

    /** First reference of a new database: TRJ-9081, as in dispatcher.html. */
    static final int PREMIER_NUMERO = 9081;

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Allowed status changes; everything else is refused. */
    private static final Map<StatutMission, Set<StatutMission>> TRANSITIONS = Map.of(
            StatutMission.PLANIFIEE, EnumSet.of(StatutMission.EN_COURS, StatutMission.ANNULEE),
            StatutMission.EN_COURS, EnumSet.of(StatutMission.LIVREE, StatutMission.ANNULEE),
            StatutMission.LIVREE, EnumSet.of(StatutMission.FACTUREE),
            StatutMission.FACTUREE, EnumSet.noneOf(StatutMission.class),
            StatutMission.ANNULEE, EnumSet.noneOf(StatutMission.class));

    private final MissionDao dao;
    private final VehiculeDao vehicules;
    private final EmployeService employes;
    private final ClientService clients;
    private final Clock clock;

    public MissionService(MissionDao dao, VehiculeDao vehicules, EmployeService employes, ClientService clients,
                          Clock clock) {
        this.dao = dao;
        this.vehicules = vehicules;
        this.employes = employes;
        this.clients = clients;
        this.clock = clock;
    }

    /** All missions, by reference (TRJ-9081 first). */
    public List<Mission> lister() {
        return dao.findAll().stream().sorted(Comparator.comparing(Mission::getReference)).toList();
    }

    /**
     * Missions matching every supplied criterion (reference, driver and city), by reference.
     * The controller only builds the criteria: the actual filtering belongs to the DAO, which can
     * express it as one dynamic, parameterized SQL WHERE clause. Driver names are resolved here so
     * the DAO receives ids it can bind as parameters.
     */
    public List<Mission> rechercher(CritereMission critere) {
        CritereMission c = critere == null ? CritereMission.TOUT : critere;
        List<Long> idsChauffeurs = new ArrayList<>();
        if (c.chauffeur() != null && !c.chauffeur().isBlank()) {
            for (Employe employe : employes.lister()) {
                if (Recherche.correspond(c.chauffeur(), employe.getNom(), employe.getPrenom(),
                        employe.getNomComplet(), employe.getMatricule())) {
                    idsChauffeurs.add(employe.getId());
                }
            }
        }
        CritereMission pourDao = new CritereMission(c.reference(), c.chauffeur(), c.ville(), idsChauffeurs);
        return dao.search(pourDao).stream().sorted(Comparator.comparing(Mission::getReference)).toList();
    }

    public Mission trouver(Long id) {
        if (id == null) {
            throw new ValidationException("Mission introuvable.");
        }
        return dao.findById(id).orElseThrow(() -> new ValidationException("Mission introuvable."));
    }

    public long compter(StatutMission statut) {
        return dao.findAll().stream().filter(m -> m.getStatut() == statut).count();
    }

    /** Counts for the "Missions par statut" bar chart, in lifecycle order, straight from the DAO. */
    public List<Part> missionsParStatut() {
        Map<StatutMission, Long> comptages = dao.compterParStatut();
        return List.of(StatutMission.PLANIFIEE, StatutMission.EN_COURS, StatutMission.LIVREE).stream()
                .map(statut -> new Part(statut.getLibelle(), comptages.getOrDefault(statut, 0L)))
                .toList();
    }

    /**
     * Slices for the truck availability pie chart. An EN_SERVICE vehicle is "En mission" when the DAO
     * finds an in-progress mission for it, otherwise it is "Disponibles"; maintenance and out-of-service
     * vehicles are "Indisponibles".
     */
    public List<Part> disponibiliteCamions() {
        Set<Long> occupes = dao.findVehiculesEnMission();
        long disponibles = 0;
        long enMission = 0;
        long indisponibles = 0;
        for (Vehicule vehicule : vehicules.findAll()) {
            if (vehicule.getStatut() != StatutVehicule.EN_SERVICE) {
                indisponibles++;
            } else if (occupes.contains(vehicule.getId())) {
                enMission++;
            } else {
                disponibles++;
            }
        }
        return List.of(new Part("Disponibles", disponibles), new Part("En mission", enMission),
                new Part("Indisponibles", indisponibles));
    }

    /** The driver's mission in progress, otherwise his next planned one (the planning page). */
    public Optional<Mission> missionActuelleOuProchaine(Long chauffeurId) {
        List<Mission> siennes = dao.findByChauffeur(chauffeurId);
        Optional<Mission> enCours = siennes.stream().filter(m -> m.getStatut() == StatutMission.EN_COURS).findFirst();
        if (enCours.isPresent()) {
            return enCours;
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return siennes.stream()
                .filter(m -> m.getStatut() == StatutMission.PLANIFIEE && m.getDateFin().isAfter(maintenant))
                .min(Comparator.comparing(Mission::getDateDebut));
    }

    /** The vehicle's mission in progress, if any (the fleet page). */
    public Optional<Mission> missionEnCoursDuVehicule(Long vehiculeId) {
        return dao.findByVehicule(vehiculeId).stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS)
                .findFirst();
    }

    /** A new mission starts PLANIFIEE with the next TRJ-xxxx reference. */
    public Mission creer(Long clientId, Long vehiculeId, Long chauffeurId, String villeDepart, String villeArrivee,
                         String cargaison, BigDecimal poidsTonnes, LocalDateTime dateDebut, LocalDateTime dateFin) {
        Mission m = new Mission();
        m.setStatut(StatutMission.PLANIFIEE);
        remplir(m, clientId, vehiculeId, chauffeurId, villeDepart, villeArrivee, cargaison, poidsTonnes,
                dateDebut, dateFin);
        m.setReference(prochaineReference());
        // JDBC ([H6], Iteration 3): the overlap check and this insert must run in ONE transaction that locks
        // the vehicle and driver rows (SELECT ... FOR UPDATE), otherwise two dispatchers can both pass the check.
        return dao.save(m);
    }

    /** Only a planned mission can be edited; the reference and status do not change. */
    public void modifier(Long id, Long clientId, Long vehiculeId, Long chauffeurId, String villeDepart,
                         String villeArrivee, String cargaison, BigDecimal poidsTonnes, LocalDateTime dateDebut,
                         LocalDateTime dateFin) {
        Mission m = trouver(id);
        if (m.getStatut() != StatutMission.PLANIFIEE) {
            throw new ValidationException("Seule une mission planifiée peut être modifiée ("
                    + m.getReference() + " est « " + m.getStatut().getLibelle() + " »).");
        }
        Mission copie = new Mission(m.getId(), m.getReference(), null, null, null, null, null, null, null, null,
                null, m.getStatut());
        remplir(copie, clientId, vehiculeId, chauffeurId, villeDepart, villeArrivee, cargaison, poidsTonnes,
                dateDebut, dateFin);
        // JDBC: same transaction and row locks as in creer().
        dao.update(copie);
    }

    public void demarrer(Long id) {
        changerStatut(id, StatutMission.EN_COURS);
    }

    public void livrer(Long id) {
        changerStatut(id, StatutMission.LIVREE);
    }

    /**
     * Assigns an existing planned mission to a driver and a vehicle. The dates of the mission do not change:
     * only its two resources do. The overlap check stays HERE, in the service, and reads every mission of the
     * driver and of the vehicle through the DAO, so a resource already busy on the same period is refused.
     * The mission being assigned is excluded from the comparison (it can keep its own resources).
     */
    public Mission assigner(Long missionId, Long vehiculeId, Long chauffeurId) {
        Mission m = trouver(missionId);
        if (m.getStatut() != StatutMission.PLANIFIEE) {
            throw new ValidationException("Seule une mission planifiée peut être assignée ("
                    + m.getReference() + " est « " + m.getStatut().getLibelle() + " »).");
        }
        Vehicule vehicule = vehiculeAffectable(vehiculeId);
        Employe chauffeur = chauffeurAffectable(chauffeurId);
        verifierChevauchement(m, vehicule, chauffeur);
        m.setVehiculeId(vehicule.getId());
        m.setChauffeurId(chauffeur.getId());
        dao.update(m);
        return m;
    }

    public void annuler(Long id) {
        changerStatut(id, StatutMission.ANNULEE);
    }

    /** Called by the invoicing side ([C3]) once the invoice of a delivered mission exists. */
    public void marquerFacturee(Long id) {
        changerStatut(id, StatutMission.FACTUREE);
    }

    /** PLANIFIEE -> EN_COURS -> LIVREE -> FACTUREE, and PLANIFIEE / EN_COURS -> ANNULEE. */
    public void changerStatut(Long id, StatutMission nouveau) {
        Mission m = trouver(id);
        if (nouveau == null || !TRANSITIONS.get(m.getStatut()).contains(nouveau)) {
            throw new ValidationException("Changement de statut impossible pour " + m.getReference() + " : « "
                    + m.getStatut().getLibelle() + " » → « " + (nouveau == null ? "?" : nouveau.getLibelle()) + " ».");
        }
        m.setStatut(nouveau);
        dao.update(m);
    }

    private void remplir(Mission m, Long clientId, Long vehiculeId, Long chauffeurId, String villeDepart,
                         String villeArrivee, String cargaison, BigDecimal poidsTonnes, LocalDateTime dateDebut,
                         LocalDateTime dateFin) {
        if (clientId == null) {
            throw new ValidationException("Choisissez un client.");
        }
        clients.trouver(clientId);
        Vehicule vehicule = vehiculeAffectable(vehiculeId);
        Employe chauffeur = chauffeurAffectable(chauffeurId);
        String depart = obligatoire(villeDepart, "La ville de départ est obligatoire.");
        String arrivee = obligatoire(villeArrivee, "La ville d'arrivée est obligatoire.");
        if (poidsTonnes == null || poidsTonnes.signum() <= 0) {
            throw new ValidationException("Le poids doit être supérieur à 0 tonne.");
        }
        if (dateDebut == null || dateFin == null) {
            throw new ValidationException("Les dates de début et de fin sont obligatoires.");
        }
        if (!dateFin.isAfter(dateDebut)) {
            throw new ValidationException("La date de fin doit être après la date de début.");
        }

        m.setClientId(clientId);
        m.setVehiculeId(vehicule.getId());
        m.setChauffeurId(chauffeur.getId());
        m.setVilleDepart(depart);
        m.setVilleArrivee(arrivee);
        m.setCargaison(cargaison == null || cargaison.isBlank() ? null : cargaison.trim());
        m.setPoidsTonnes(poidsTonnes);
        m.setDateDebut(dateDebut);
        m.setDateFin(dateFin);
        verifierChevauchement(m, vehicule, chauffeur);
    }

    private Vehicule vehiculeAffectable(Long vehiculeId) {
        if (vehiculeId == null) {
            throw new ValidationException("Choisissez un véhicule.");
        }
        Vehicule v = vehicules.findById(vehiculeId)
                .orElseThrow(() -> new ValidationException("Véhicule introuvable."));
        if (v.getStatut() != StatutVehicule.EN_SERVICE) {
            throw new ValidationException("Le véhicule " + v.getMatricule() + " est « "
                    + v.getStatut().getLibelle().toLowerCase() + " » : il ne peut pas être affecté.");
        }
        return v;
    }

    private Employe chauffeurAffectable(Long chauffeurId) {
        if (chauffeurId == null) {
            throw new ValidationException("Choisissez un chauffeur.");
        }
        return employes.listerChauffeurs().stream()
                .filter(e -> e.getId().equals(chauffeurId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Seul un chauffeur actif peut être affecté à une mission."));
    }

    /** Refuses the mission when its vehicle or its driver is already on another non-cancelled mission. */
    private void verifierChevauchement(Mission m, Vehicule vehicule, Employe chauffeur) {
        for (Mission autre : dao.findByVehicule(vehicule.getId())) {
            if (chevauche(m, autre)) {
                throw new ValidationException("Chevauchement : le véhicule " + vehicule.getMatricule()
                        + " est déjà affecté à " + decrire(autre) + ".");
            }
        }
        for (Mission autre : dao.findByChauffeur(chauffeur.getId())) {
            if (chevauche(m, autre)) {
                throw new ValidationException("Chevauchement : le chauffeur " + chauffeur.getNomComplet()
                        + " est déjà affecté à " + decrire(autre) + ".");
            }
        }
    }

    private static boolean chevauche(Mission a, Mission b) {
        return !Objects.equals(a.getId(), b.getId())
                && b.getStatut() != StatutMission.ANNULEE
                && a.getDateDebut() != null && a.getDateFin() != null
                && b.getDateDebut() != null && b.getDateFin() != null
                && a.getDateDebut().isBefore(b.getDateFin())
                && b.getDateDebut().isBefore(a.getDateFin());
    }

    /** "TRJ-9081 (Casablanca → Tanger Med, du 17/11/2026 08:00 au 17/11/2026 18:30)". */
    private static String decrire(Mission m) {
        return m.getReference() + " (" + m.getItineraire() + ", du " + DATE_HEURE.format(m.getDateDebut())
                + " au " + DATE_HEURE.format(m.getDateFin()) + ")";
    }

    private String prochaineReference() {
        int dernier = dao.findAll().stream()
                .map(Mission::getReference)
                .filter(r -> r != null && r.matches("TRJ-\\d+"))
                .mapToInt(r -> Integer.parseInt(r.substring(4)))
                .max()
                .orElse(PREMIER_NUMERO - 1);
        return "TRJ-" + (dernier + 1);
    }

    private static String obligatoire(String texte, String message) {
        if (texte == null || texte.isBlank()) {
            throw new ValidationException(message);
        }
        return texte.trim();
    }
}
