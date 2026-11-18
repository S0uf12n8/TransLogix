package ma.translogix.model;

/** Mission lifecycle: PLANIFIEE -> EN_COURS -> LIVREE -> FACTUREE, or ANNULEE before delivery (MissionService). */
public enum StatutMission {
    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    LIVREE("Livrée"),
    FACTUREE("Facturée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutMission(String libelle) {
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
