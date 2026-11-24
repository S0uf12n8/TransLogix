package ma.translogix.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import ma.translogix.dao.BonCarburantDao;
import ma.translogix.model.BonCarburant;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutBonCarburant;
import ma.translogix.model.StatutMission;
import ma.translogix.util.ValidationException;

/**
 * Fuel vouchers of the Dispatcher module. No JavaFX and no SQL here: every write goes through the DAO.
 * A voucher is only issued for a mission in progress, by the truck and the driver of that mission.
 */
public class BonCarburantService {

    /** First reference of a new database, like the other dispatcher documents. */
    static final int PREMIER_NUMERO = 8841;

    private final BonCarburantDao dao;
    private final MissionService missions;
    private final Clock horloge;

    public BonCarburantService(BonCarburantDao dao, MissionService missions, Clock horloge) {
        this.dao = dao;
        this.missions = missions;
        this.horloge = horloge;
    }

    /** All vouchers, newest first. */
    public List<BonCarburant> lister() {
        return dao.findAll().stream()
                .sorted(Comparator.comparing(BonCarburant::getDate).thenComparing(BonCarburant::getId).reversed())
                .toList();
    }

    /** Missions a voucher can be issued for: the dialog never hardcodes this rule. */
    public List<Mission> missionsEligibles() {
        return missions.lister().stream().filter(m -> m.getStatut() == StatutMission.EN_COURS).toList();
    }

    /** Issues and saves one voucher through the DAO. */
    public BonCarburant emettre(Long missionId, String station, BigDecimal litres, BigDecimal montant, LocalDate date) {
        Mission mission = missions.trouver(missionId);
        if (mission.getStatut() != StatutMission.EN_COURS) {
            throw new ValidationException("Un bon de carburant ne peut être émis que pour une mission en cours ("
                    + mission.getReference() + " est « " + mission.getStatut().getLibelle() + " »).");
        }
        if (mission.getVehiculeId() == null || mission.getChauffeurId() == null) {
            throw new ValidationException("La mission " + mission.getReference()
                    + " n'a pas encore de camion et de chauffeur affectés.");
        }

        String gare = obligatoire(station, "La station / le provider est obligatoire.");
        BigDecimal quantite = positif(litres, "La quantité doit être supérieure à 0 litre.");
        BigDecimal cout = positif(montant, "Le montant doit être supérieur à 0 MAD.");
        if (date == null) {
            throw new ValidationException("La date du bon est obligatoire (format jj/mm/aaaa).");
        }
        if (date.isAfter(LocalDate.now(horloge))) {
            throw new ValidationException("La date du bon ne peut pas être dans le futur.");
        }

        BonCarburant bon = new BonCarburant(null, prochaineReference(), mission.getId(), mission.getVehiculeId(),
                mission.getChauffeurId(), gare, quantite, cout, date, StatutBonCarburant.VALIDE);
        return dao.save(bon);
    }

    private String prochaineReference() {
        int dernier = dao.findAll().stream()
                .map(BonCarburant::getReference)
                .filter(r -> r != null && r.matches("BON-\\d+"))
                .mapToInt(r -> Integer.parseInt(r.substring(4)))
                .max()
                .orElse(PREMIER_NUMERO - 1);
        return "BON-" + (dernier + 1);
    }

    private static String obligatoire(String texte, String message) {
        if (texte == null || texte.isBlank()) {
            throw new ValidationException(message);
        }
        return texte.trim();
    }

    private static BigDecimal positif(BigDecimal valeur, String message) {
        if (valeur == null || valeur.signum() <= 0) {
            throw new ValidationException(message);
        }
        return valeur;
    }
}
