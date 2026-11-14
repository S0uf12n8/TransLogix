package ma.translogix.model;

public enum Role {
    GERANT("Gérant"),
    DISPATCHER("Dispatcher"),
    COMPTABLE("Comptable"),
    RH("Ressources humaines"),
    CHAUFFEUR("Chauffeur");

    private final String libelle;

    Role(String libelle) {
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
