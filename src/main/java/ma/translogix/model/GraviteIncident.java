package ma.translogix.model;

/** Severity used by the dispatcher to prioritise an incident. */
public enum GraviteIncident {
    FAIBLE("Faible"),
    MOYENNE("Moyenne"),
    URGENTE("Urgente");

    private final String libelle;

    GraviteIncident(String libelle) {
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
