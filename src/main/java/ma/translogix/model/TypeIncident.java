package ma.translogix.model;

/** What happened on the road. */
public enum TypeIncident {
    PANNE("Panne mécanique"),
    ACCIDENT("Accident"),
    RETARD("Blocage / retard"),
    AUTRE("Autre");

    private final String libelle;

    TypeIncident(String libelle) {
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
