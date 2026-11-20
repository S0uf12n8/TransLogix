package ma.translogix.service;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.StatutMission;

/**
 * Figures of the Gérant (CEO) dashboard: money, fleet, risk.
 * Today implemented by DemoDashboardService; real implementations will read the dispatcher,
 * comptable and RH services. The screen only depends on this interface.
 */
public interface DashboardService {

    /** Revenue of one month, in MAD. */
    record ChiffreMensuel(YearMonth mois, long montantMad) {
    }

    /** One slice or bar: a label and a count. */
    record Part(String libelle, int nombre) {
    }

    /** Severity of an alert; the screen maps it to red / amber / blue. */
    enum Niveau { CRITIQUE, ATTENTION, INFO }

    /** One item of "Alertes prioritaires". cibleFxml is the page opened on click (via Navigator). */
    record Alerte(String titre, String detail, Niveau niveau, String cibleFxml) {
    }

    /**
     * Mission statuses, in lifecycle order (business rule 2): the labels of StatutMission, in declaration order,
     * so the pie always matches the statuses missions can really have.
     */
    List<String> STATUTS_MISSION = Arrays.stream(StatutMission.values()).map(StatutMission::getLibelle).toList();

    /** True when the figures are sample values; the screen then labels them "Données de démonstration". */
    boolean estDemo();

    /**
     * Revenue of the last nbMois months, oldest first, the last one being the current month.
     * @throws IllegalArgumentException if nbMois is less than 1
     */
    List<ChiffreMensuel> chiffreAffairesParMois(int nbMois);

    /**
     * Number of missions per status.
     * CONTRACT: one Part per status, exactly in the order of STATUTS_MISSION, zero counts included.
     * The pie chart colours (app.css .default-color0..4) follow this order, so changing it changes the colours.
     */
    List<Part> missionsParStatut();

    /** Vehicles per state: "En mission", "Disponible", "En maintenance" (in this order). */
    List<Part> etatFlotte();

    /** Compliance and risk alerts, most severe first. */
    List<Alerte> alertesConformite();

    /** The n most recent audit events, newest first (fewer if the log is shorter). */
    List<EvenementAudit> derniersEvenementsAudit(int n);
}
