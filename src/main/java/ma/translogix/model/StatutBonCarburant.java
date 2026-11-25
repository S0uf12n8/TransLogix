package ma.translogix.model;

/** Lifecycle of a fuel voucher issued by the dispatcher. */
public enum StatutBonCarburant {
    VALIDE("Validé Dispatch"),
    EN_ATTENTE_RECU("En attente reçu");

    private final String libelle;

    StatutBonCarburant(String libelle) {
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
