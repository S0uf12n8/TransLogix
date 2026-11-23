package ma.translogix.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Multi-criteria filter for invoices. Every criterion is optional: null (or a blank keyword) means
 * "no restriction". Dates are inclusive, amounts apply to the TTC total.
 *
 * <p>The keyword is matched against the invoice number and trip reference by the DAO, and against
 * client names/ICE/phones through {@code idsClientsMotes}: the service resolves those client ids so
 * that a JDBC implementation can express the whole search with PreparedStatement parameters only.</p>
 */
public record CritereFacture(String motCle, List<Long> idsClientsMotes, Long clientId, String statut,
                             LocalDate du, LocalDate au, BigDecimal montantMin, BigDecimal montantMax) {

    /** No criterion: every invoice. */
    public static final CritereFacture TOUT =
            new CritereFacture(null, List.of(), null, null, null, null, null, null);

    /** Convenience constructor for callers that do not search by client name. */
    public CritereFacture(String motCle, Long clientId, String statut, LocalDate du, LocalDate au,
                          BigDecimal montantMin, BigDecimal montantMax) {
        this(motCle, List.of(), clientId, statut, du, au, montantMin, montantMax);
    }
}
