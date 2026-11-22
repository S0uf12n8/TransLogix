package ma.translogix.service;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import ma.translogix.model.EvenementAudit;

/**
 * DEMO figures for the dashboard, consistent with the KPI cards of admine.html
 * (452 000 MAD this month, +14 % vs last month, fleet 18 / 22, 4 in maintenance).
 * Only derniersEvenementsAudit is real (read from AuditService).
 * Replace with a real implementation once the dispatcher, comptable and RH modules exist.
 */
public class DemoDashboardService implements DashboardService {

    /** Last 12 months of demo revenue, oldest first; the last value is the current month. */
    private static final long[] CHIFFRES_DEMO = {
        255_000, 262_000, 270_500, 281_000, 289_000, 301_500,
        318_000, 341_000, 362_500, 377_000, 396_500, 452_000
    };

    /** Same order as STATUTS_MISSION. */
    private static final int[] MISSIONS_DEMO = {5, 6, 9, 14, 2};

    private final AuditService audit;
    private final Clock horloge;

    public DemoDashboardService(AuditService audit, Clock horloge) {
        this.audit = audit;
        this.horloge = horloge;
    }

    @Override
    public boolean estDemo() {
        return true;
    }

    @Override
    public List<ChiffreMensuel> chiffreAffairesParMois(int nbMois) {
        if (nbMois < 1 || nbMois > CHIFFRES_DEMO.length) {
            throw new IllegalArgumentException(
                    "Nombre de mois entre 1 et " + CHIFFRES_DEMO.length + " : " + nbMois);
        }
        YearMonth courant = YearMonth.now(horloge);
        List<ChiffreMensuel> resultat = new ArrayList<>();
        for (int i = nbMois - 1; i >= 0; i--) {
            resultat.add(new ChiffreMensuel(courant.minusMonths(i), CHIFFRES_DEMO[CHIFFRES_DEMO.length - 1 - i]));
        }
        return resultat;
    }

    @Override
    public List<Part> missionsParStatut() {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < STATUTS_MISSION.size(); i++) {
            parts.add(new Part(STATUTS_MISSION.get(i), MISSIONS_DEMO[i]));
        }
        return parts;
    }

    @Override
    public List<Part> etatFlotte() {
        return List.of(new Part("En mission", 6), new Part("Disponible", 12), new Part("En maintenance", 4));
    }

    @Override
    public List<Alerte> alertesConformite() {
        return List.of(
                new Alerte("Permis à renouveler", "1 permis nécessite une vérification.",
                        Niveau.CRITIQUE, "rh/conformite.fxml"),
                new Alerte("Factures impayées", "89 400 MAD à suivre.",
                        Niveau.ATTENTION, "comptable/factures.fxml"),
                new Alerte("Maintenance flotte", "4 véhicules à l'atelier.",
                        Niveau.INFO, "dispatcher/flotte.fxml"));
    }

    @Override
    public List<EvenementAudit> derniersEvenementsAudit(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n doit être positif : " + n);
        }
        return audit.lister().stream().limit(n).toList();
    }

    /** The mockup's KPI cards (admine.html). */
    @Override
    public Indicateurs indicateurs() {
        return new Indicateurs(452_000, 14, 18, 22, 4, 89_400, 3, 95, 0, 1);
    }

    /** The mockup's "Trajets récents" rows. */
    @Override
    public List<Trajet> trajetsRecents(int n) {
        return List.of(
                new Trajet("TRJ-9081", "Mohammed Bennani", "12345-A-6 · Volvo", "Casablanca", "Tanger",
                        "En cours", null),
                new Trajet("TRJ-9082", "Abderrahim El Amrani", "67890-B-2 · Scania", "Agadir", "Marrakech",
                        "Livrée", "12 100 MAD"),
                new Trajet("TRJ-9083", "Youssef Amrani", "45678-C-1 · MAN", "Rabat", "Fès", "Planifiée", null))
                .stream().limit(Math.max(0, n)).toList();
    }
}
