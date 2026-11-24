package ma.translogix.model;

/** Real expense categories reported by a driver on a mission. */
public enum TypeDepense {
    CARBURANT("Carburant"),
    PEAGE("Péage"),
    REPAS("Repas"),
    HEBERGEMENT("Hébergement"),
    AUTRE("Autre");

    private final String libelle;

    TypeDepense(String libelle) {
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
