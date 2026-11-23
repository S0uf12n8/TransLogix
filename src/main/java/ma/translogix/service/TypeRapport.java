package ma.translogix.service;

/** The reports of the "Rapports & analytics" page, and which filters each one uses. */
public enum TypeRapport {
    FACTURATION("Facturation", true, true),
    PAIEMENTS("Règlements clients", true, true),
    MISSIONS("Missions", true, true),
    CONFORMITE("Conformité RH (documents à traiter)", true, false),
    COMPTES("Comptes utilisateurs", false, false);

    private final String libelle;
    private final boolean filtreDates;
    private final boolean filtreClient;

    TypeRapport(String libelle, boolean filtreDates, boolean filtreClient) {
        this.libelle = libelle;
        this.filtreDates = filtreDates;
        this.filtreClient = filtreClient;
    }

    public String getLibelle() {
        return libelle;
    }

    /** True when the period (du / au) filters this report. */
    public boolean utiliseDates() {
        return filtreDates;
    }

    /** True when the client filters this report. */
    public boolean utiliseClient() {
        return filtreClient;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
