package ma.translogix.model;

/** Legal document a driver must keep valid ([M4]). */
public enum TypeDocument {
    PERMIS("Permis de conduire"),
    FIMC("Carte FIMC / FCO"),
    VISITE_MEDICALE("Visite médicale");

    private final String libelle;

    TypeDocument(String libelle) {
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
