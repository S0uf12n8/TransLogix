package ma.translogix.model;

/** Employment contract ([M2]). */
public enum TypeContrat {
    CDI("CDI"),
    CDD("CDD"),
    ESSAI("Période d'essai");

    private final String libelle;

    TypeContrat(String libelle) {
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
