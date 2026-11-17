package ma.translogix.model;

/** Outcome of an audited action. */
public enum ResultatAudit {
    REUSSI("Réussi"),
    REFUSE("Refusé");

    private final String libelle;

    ResultatAudit(String libelle) {
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
