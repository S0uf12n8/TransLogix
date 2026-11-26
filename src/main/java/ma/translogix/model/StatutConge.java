package ma.translogix.model;

/** Lifecycle of a leave request: EN_ATTENTE -> APPROUVE or REFUSE (CongeService). */
public enum StatutConge {
    EN_ATTENTE("En attente"),
    APPROUVE("Approuvé"),
    REFUSE("Refusé");

    private final String libelle;

    StatutConge(String libelle) {
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
