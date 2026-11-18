package ma.translogix.model;

/** Only an EN_SERVICE vehicle can be assigned to a mission (MissionService). */
public enum StatutVehicule {
    EN_SERVICE("En service"),
    MAINTENANCE("En maintenance"),
    HORS_SERVICE("Hors service");

    private final String libelle;

    StatutVehicule(String libelle) {
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
