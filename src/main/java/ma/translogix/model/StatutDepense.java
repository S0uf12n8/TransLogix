package ma.translogix.model;

/** Approval lifecycle of a driver expense: waiting, validated or refused by the accountant. */
public enum StatutDepense {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    REFUSEE("Refusée");

    private final String libelle;

    StatutDepense(String libelle) {
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
