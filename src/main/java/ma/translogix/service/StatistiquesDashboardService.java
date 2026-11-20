package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.StatutFacture;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.util.Formats;

/**
 * Real figures of the Gérant (CEO) dashboard, computed from the dispatcher, comptable and RH services.
 * No SQL here: the figures follow whatever DAOs AppContext plugs in (in memory today, JDBC later).
 * Each method reads each list once, so a JDBC DAO costs one query per list, never one per row (no N+1).
 */
public class StatistiquesDashboardService implements DashboardService {

    static final String EN_MISSION = "En mission";
    static final String DISPONIBLE = "Disponible";
    static final String EN_MAINTENANCE = "En maintenance";

    private final MissionService missions;
    private final VehiculeService vehicules;
    private final FactureService factures;
    private final ConformiteService conformite;
    private final AuditService audit;
    private final Clock horloge;

    public StatistiquesDashboardService(MissionService missions, VehiculeService vehicules, FactureService factures,
                                        ConformiteService conformite, AuditService audit, Clock horloge) {
        this.missions = missions;
        this.vehicules = vehicules;
        this.factures = factures;
        this.conformite = conformite;
        this.audit = audit;
        this.horloge = horloge;
    }

    @Override
    public boolean estDemo() {
        return false;
    }

    /**
     * Revenue = sum of the HT amounts of the invoices issued that month, cancelled invoices excluded
     * (same definition as the comptable dashboard, FactureService.synthese). Rounded to the nearest MAD.
     */
    @Override
    public List<ChiffreMensuel> chiffreAffairesParMois(int nbMois) {
        if (nbMois < 1) {
            throw new IllegalArgumentException("Le nombre de mois doit être au moins 1 : " + nbMois);
        }
        YearMonth courant = YearMonth.now(horloge);
        YearMonth premier = courant.minusMonths(nbMois - 1L);
        Map<YearMonth, BigDecimal> parMois = new HashMap<>();
        for (Facture f : factures.lister()) {
            YearMonth mois = YearMonth.from(f.getDateEmission());
            if (f.getStatut() != StatutFacture.ANNULEE && !mois.isBefore(premier) && !mois.isAfter(courant)) {
                parMois.merge(mois, f.getMontantHT(), BigDecimal::add);
            }
        }
        List<ChiffreMensuel> resultat = new ArrayList<>();
        for (YearMonth mois = premier; !mois.isAfter(courant); mois = mois.plusMonths(1)) {
            BigDecimal montant = parMois.getOrDefault(mois, BigDecimal.ZERO);
            resultat.add(new ChiffreMensuel(mois, montant.setScale(0, RoundingMode.HALF_UP).longValueExact()));
        }
        return resultat;
    }

    /**
     * One Part per label of STATUTS_MISSION, in that order (contract of the interface).
     * A label with no matching StatutMission (today "Créée" and "Suspendue") counts 0.
     */
    @Override
    public List<Part> missionsParStatut() {
        Map<String, Long> parLibelle = missions.lister().stream()
                .collect(Collectors.groupingBy(m -> m.getStatut().getLibelle(), Collectors.counting()));
        return STATUTS_MISSION.stream()
                .map(libelle -> new Part(libelle, parLibelle.getOrDefault(libelle, 0L).intValue()))
                .toList();
    }

    /**
     * "En mission": a vehicle with a mission EN_COURS; "Disponible": EN_SERVICE with none;
     * "En maintenance": MAINTENANCE. HORS_SERVICE vehicles are not part of the operational fleet and are left out.
     */
    @Override
    public List<Part> etatFlotte() {
        Set<Long> surLaRoute = missions.lister().stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS)
                .map(Mission::getVehiculeId)
                .collect(Collectors.toSet());
        int enMission = 0;
        int disponibles = 0;
        int enMaintenance = 0;
        for (Vehicule v : vehicules.lister()) {
            if (v.getStatut() == StatutVehicule.MAINTENANCE) {
                enMaintenance++;
            } else if (v.getStatut() == StatutVehicule.EN_SERVICE) {
                if (surLaRoute.contains(v.getId())) {
                    enMission++;
                } else {
                    disponibles++;
                }
            }
        }
        return List.of(new Part(EN_MISSION, enMission), new Part(DISPONIBLE, disponibles),
                new Part(EN_MAINTENANCE, enMaintenance));
    }

    /**
     * Expired compliance documents (critical), documents to renew and overdue invoices (warning),
     * vehicles in maintenance (info). An alert appears only when its count is above 0. Most severe first.
     */
    @Override
    public List<Alerte> alertesConformite() {
        List<Alerte> alertes = new ArrayList<>();

        List<DocumentConformite> documents = conformite.alertes();
        long expires = documents.stream().filter(d -> conformite.statut(d) == StatutConformite.EXPIRE).count();
        long aRenouveler = documents.size() - expires;
        if (expires > 0) {
            alertes.add(new Alerte("Documents expirés",
                    pluriel(expires, "document de conformité expiré", "documents de conformité expirés") + ".",
                    Niveau.CRITIQUE, "rh/conformite.fxml"));
        }
        if (aRenouveler > 0) {
            alertes.add(new Alerte("Documents à renouveler",
                    pluriel(aRenouveler, "document expire", "documents expirent") + " dans moins de "
                            + ConformiteService.SEUIL_RENOUVELLEMENT_JOURS + " jours.",
                    Niveau.ATTENTION, "rh/conformite.fxml"));
        }

        List<Facture> enRetard = factures.lister().stream().filter(factures::estEnRetard).toList();
        if (!enRetard.isEmpty()) {
            BigDecimal du = enRetard.stream().map(Facture::getMontantTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
            alertes.add(new Alerte("Factures en retard",
                    pluriel(enRetard.size(), "facture impayée", "factures impayées") + " après échéance : "
                            + Formats.mad(du) + ".",
                    Niveau.ATTENTION, "comptable/factures.fxml"));
        }

        long atelier = vehicules.compter(StatutVehicule.MAINTENANCE);
        if (atelier > 0) {
            alertes.add(new Alerte("Maintenance flotte",
                    pluriel(atelier, "véhicule", "véhicules") + " à l'atelier.",
                    Niveau.INFO, "dispatcher/flotte.fxml"));
        }

        alertes.sort(Comparator.comparing(Alerte::niveau)); // stable: keeps the order above within a level
        return alertes;
    }

    @Override
    public List<EvenementAudit> derniersEvenementsAudit(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n doit être positif : " + n);
        }
        return audit.lister().stream().limit(n).toList();
    }

    private static String pluriel(long nombre, String singulier, String plurielTexte) {
        return nombre + " " + (nombre > 1 ? plurielTexte : singulier);
    }
}
