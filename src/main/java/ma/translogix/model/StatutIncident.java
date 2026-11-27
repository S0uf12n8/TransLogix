package ma.translogix.model;

/** Incident lifecycle. Only an open incident is shown in the "En cours" list. */
public enum StatutIncident {
    OUVERT("Ouvert"),
    RESOLU("Résolu");

    private final String libelle;

    StatutIncident(String libelle) {
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
