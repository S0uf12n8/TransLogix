package ma.translogix.model;

/** Leave categories handled by the RH leave page. */
public enum TypeConge {
    PAYE("Congé payé"),
    MALADIE("Congé maladie"),
    EXCEPTIONNEL("Congé exceptionnel"),
    SANS_SOLDE("Congé sans solde");

    private final String libelle;

    TypeConge(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
