package ma.translogix.service;

import java.time.LocalDate;

/**
 * Criteria of a report. Every criterion is optional: null means "no limit".
 * du / au are inclusive. statut is a label from RapportService.statuts(type).
 */
public record FiltreRapport(LocalDate du, LocalDate au, Long clientId, String statut) {

    /** No criterion: the whole report. */
    public static final FiltreRapport TOUT = new FiltreRapport(null, null, null, null);

    /** True when the date is inside the period (a null date is outside any limited period). */
    boolean dansLaPeriode(LocalDate date) {
        if (du == null && au == null) {
            return true;
        }
        return date != null && (du == null || !date.isBefore(du)) && (au == null || !date.isAfter(au));
    }

    boolean pourLeClient(Long id) {
        return clientId == null || clientId.equals(id);
    }

    boolean avecLeStatut(String libelle) {
        return statut == null || statut.equals(libelle);
    }
}
