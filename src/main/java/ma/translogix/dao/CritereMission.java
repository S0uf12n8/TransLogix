package ma.translogix.dao;

import java.util.List;

/**
 * Multi-criteria filter for missions. Every criterion is optional: null or blank means "no restriction".
 * The three supplied criteria are combined (AND) by the DAO.
 *
 * <p>A mission only stores the driver id, so {@code MissionService} resolves the free-text driver name
 * into {@code idsChauffeursMotes} before calling the DAO. This keeps the JDBC search expressible with
 * PreparedStatement parameters only, without concatenating user input into SQL.</p>
 */
public record CritereMission(String reference, String chauffeur, String ville, List<Long> idsChauffeursMotes) {

    /** No criterion: every mission. */
    public static final CritereMission TOUT = new CritereMission(null, null, null, List.of());

    /** Convenience constructor for callers that do not resolve driver ids themselves. */
    public CritereMission(String reference, String chauffeur, String ville) {
        this(reference, chauffeur, ville, List.of());
    }
}
