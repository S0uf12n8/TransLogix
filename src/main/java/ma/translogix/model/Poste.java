package ma.translogix.model;

/** Job of an employee ([M2]). */
public enum Poste {
    CHAUFFEUR("Chauffeur"),
    AGENT_DEPOT("Agent de dépôt"),
    MECANICIEN("Mécanicien"),
    ADMINISTRATIF("Administratif");

    private final String libelle;

    Poste(String libelle) {
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
