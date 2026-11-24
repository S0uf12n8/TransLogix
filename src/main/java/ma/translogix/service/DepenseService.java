package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import ma.translogix.dao.DepenseDao;
import ma.translogix.model.Depense;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeDepense;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/**
 * Rules of the "Valider dépenses" screen. A driver expense is always linked to a real mission and a real
 * driver; the accountant can only validate or refuse one that is still waiting. No JavaFX and no SQL here.
 */
public class DepenseService {

    /** Search criteria of the expense screen; null or blank means "no restriction". */
    public record CritereDepense(String texte, StatutDepense statut, TypeDepense type, Long chauffeurId,
                                 LocalDate du, LocalDate au) {
        public static final CritereDepense AUCUN = new CritereDepense(null, null, null, null, null, null);
    }

    /** Real figures of the page: only amounts from recorded expenses, never invented numbers. */
    public record Synthese(int enAttente, BigDecimal montantEnAttente, BigDecimal montantValide, int refusees) {
    }

    private static final BigDecimal MONTANT_MAX = new BigDecimal("1000000.00");

    private final DepenseDao dao;
    private final MissionService missions;
    private final EmployeService employes;
    private final AuditService audit;
    private final Clock horloge;
    private final Supplier<String> acteur;

    public DepenseService(DepenseDao dao, AuditService audit, MissionService missions, EmployeService employes,
                          Clock horloge, Supplier<String> acteur) {
        this.dao = dao;
        this.audit = audit;
        this.missions = missions;
        this.employes = employes;
        this.horloge = horloge;
        this.acteur = acteur;
    }

    /**
     * Records a receipt submitted by a driver. The mission must belong to that driver and the date must be
     * inside the mission, up to today. The caller supplies the figures read from the real receipt.
     */
    public Depense creer(Long missionId, Long chauffeurId, TypeDepense type, String station, BigDecimal montant,
                         BigDecimal litres, LocalDate date) {
        Mission mission = missions.trouver(missionId);
        Employe chauffeur = employes.trouver(chauffeurId);
        if (chauffeur.getPoste() != Poste.CHAUFFEUR || !chauffeur.isActif()) {
            throw new ValidationException("La dépense doit être rattachée à un chauffeur actif.");
        }
        if (mission.getChauffeurId() == null || !mission.getChauffeurId().equals(chauffeurId)) {
            throw new ValidationException("Ce chauffeur n'est pas affecté à la mission " + mission.getReference() + ".");
        }
        if (type == null) {
            throw new ValidationException("Choisissez le type de dépense.");
        }
        verifierStation(station);
        verifierMontant(montant);
        verifierLitres(type, litres);
        verifierDate(date, mission);

        Depense depense = new Depense(null, missionId, chauffeurId, type, station.trim(),
                montant.setScale(2, RoundingMode.HALF_UP), litres == null ? null : litres.setScale(2, RoundingMode.HALF_UP),
                date, StatutDepense.EN_ATTENTE, null);
        if (dao.findAll().stream().anyMatch(d -> memeDepense(d, depense))) {
            throw new ValidationException("Cette dépense a déjà été saisie pour la mission "
                    + mission.getReference() + ". Vérifiez le montant et la date du reçu.");
        }
        Depense enregistree = dao.save(depense);
        audit.enregistrer("Dépenses", "Dépense soumise : " + mission.getReference() + " · "
                + type.getLibelle() + " · " + Formats.mad(depense.getMontant()), ResultatAudit.REUSSI);
        return enregistree;
    }

    /** Waiting -> validated. Validating twice is refused, and a refused expense never comes back. */
    public void valider(Long id) {
        Depense depense = trouver(id);
        if (depense.getStatut() != StatutDepense.EN_ATTENTE) {
            throw new ValidationException("Seule une dépense en attente peut être validée (statut actuel : "
                    + libelleStatut(depense.getStatut()) + ").");
        }
        depense.setStatut(StatutDepense.VALIDEE);
        depense.setMotifRefus(null);
        dao.update(depense);
        audit.enregistrer("Dépenses", "Dépense validée n° " + id + " par " + acteur.get(),
                ResultatAudit.REUSSI);
    }

    /** Waiting -> refused. The reason is mandatory and kept with the expense for the audit trail. */
    public void refuser(Long id, String motif) {
        Depense depense = trouver(id);
        if (depense.getStatut() != StatutDepense.EN_ATTENTE) {
            throw new ValidationException("Seule une dépense en attente peut être refusée (statut actuel : "
                    + libelleStatut(depense.getStatut()) + ").");
        }
        if (motif == null || motif.isBlank()) {
            throw new ValidationException("Indiquez le motif du refus.");
        }
        if (motif.trim().length() > 500) {
            throw new ValidationException("Le motif du refus ne peut pas dépasser 500 caractères.");
        }
        depense.setStatut(StatutDepense.REFUSEE);
        depense.setMotifRefus(motif.trim());
        dao.update(depense);
        audit.enregistrer("Dépenses", "Dépense refusée n° " + id + " : " + motif.trim(), ResultatAudit.REUSSI);
    }

    /** All expenses, most recent first. */
    public List<Depense> lister() {
        return dao.findAll().stream()
                .sorted(Comparator.comparing(Depense::getDate).thenComparing(Depense::getId).reversed())
                .toList();
    }

    /** Expenses matching every criterion (AND), most recent first. */
    public List<Depense> rechercher(CritereDepense critere) {
        CritereDepense c = critere == null ? CritereDepense.AUCUN : critere;
        if (c.du() != null && c.au() != null && c.au().isBefore(c.du())) {
            throw new ValidationException("La date de fin doit être après la date de début.");
        }
        Map<Long, Mission> parMission = missions.lister().stream()
                .collect(Collectors.toMap(Mission::getId, m -> m));
        Map<Long, Employe> parChauffeur = employes.lister().stream()
                .collect(Collectors.toMap(Employe::getId, e -> e));
        return lister().stream()
                .filter(d -> c.statut() == null || d.getStatut() == c.statut())
                .filter(d -> c.type() == null || d.getType() == c.type())
                .filter(d -> c.chauffeurId() == null || c.chauffeurId().equals(d.getChauffeurId()))
                .filter(d -> c.du() == null || !d.getDate().isBefore(c.du()))
                .filter(d -> c.au() == null || !d.getDate().isAfter(c.au()))
                .filter(d -> Recherche.correspond(c.texte(),
                        d.getType().getLibelle(),
                        d.getStation(),
                        libelleStatut(d.getStatut()),
                        d.getMontant().toPlainString(),
                        d.getMotifRefus(),
                        parMission.get(d.getMissionId()) == null ? null
                                : parMission.get(d.getMissionId()).getReference(),
                        parChauffeur.get(d.getChauffeurId()) == null ? null
                                : nom(parChauffeur.get(d.getChauffeurId()))))
                .toList();
    }

    /** Counts and amounts of the KPI row, computed from the same list as the table. */
    public Synthese synthese() {
        List<Depense> tout = dao.findAll();
        BigDecimal enAttente = tout.stream()
                .filter(d -> d.getStatut() == StatutDepense.EN_ATTENTE)
                .map(Depense::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal validees = tout.stream()
                .filter(d -> d.getStatut() == StatutDepense.VALIDEE)
                .map(Depense::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long nombreAttente = tout.stream().filter(d -> d.getStatut() == StatutDepense.EN_ATTENTE).count();
        long nombreRefusees = tout.stream().filter(d -> d.getStatut() == StatutDepense.REFUSEE).count();
        return new Synthese((int) nombreAttente, enAttente, validees, (int) nombreRefusees);
    }

    public static String libelleStatut(StatutDepense statut) {
        return statut == null ? "—" : statut.getLibelle();
    }

    public static String nom(Employe employe) {
        return employe.getPrenom() + " " + employe.getNom();
    }

    private Depense trouver(Long id) {
        if (id == null) {
            throw new ValidationException("Dépense introuvable.");
        }
        return dao.findById(id).orElseThrow(() -> new ValidationException("Dépense introuvable."));
    }

    private static boolean memeDepense(Depense a, Depense b) {
        return a.getMissionId().equals(b.getMissionId())
                && a.getChauffeurId().equals(b.getChauffeurId())
                && a.getType() == b.getType()
                && a.getStation().equalsIgnoreCase(b.getStation())
                && a.getDate().equals(b.getDate())
                && a.getMontant().compareTo(b.getMontant()) == 0;
    }

    private static void verifierStation(String station) {
        if (station == null || station.isBlank()) {
            throw new ValidationException("Indiquez la station ou le lieu de la dépense.");
        }
        if (station.trim().length() > 120) {
            throw new ValidationException("Le lieu ne peut pas dépasser 120 caractères.");
        }
    }

    private static void verifierMontant(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new ValidationException("Le montant doit être supérieur à zéro.");
        }
        if (montant.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("Le montant ne peut pas avoir plus de deux décimales.");
        }
        if (montant.compareTo(MONTANT_MAX) > 0) {
            throw new ValidationException("Le montant dépasse la limite autorisée.");
        }
    }

    private static void verifierLitres(TypeDepense type, BigDecimal litres) {
        if (litres == null) {
            return;
        }
        if (type != TypeDepense.CARBURANT) {
            throw new ValidationException("Les litres ne concernent que les dépenses de carburant.");
        }
        if (litres.signum() <= 0 || litres.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("Le nombre de litres doit être positif (deux décimales maximum).");
        }
    }

    private void verifierDate(LocalDate date, Mission mission) {
        if (date == null) {
            throw new ValidationException("Indiquez la date du reçu.");
        }
        if (date.isAfter(LocalDate.now(horloge))) {
            throw new ValidationException("La date du reçu ne peut pas être dans le futur.");
        }
        if (mission.getDateDebut() != null && date.isBefore(mission.getDateDebut().toLocalDate())) {
            throw new ValidationException("La date du reçu ne peut pas précéder le début de la mission "
                    + mission.getReference() + ".");
        }
    }

}
