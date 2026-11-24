package ma.translogix.model;

/** How a salary is paid to an employee (payroll-specific, unlike client invoice payments). */
public enum ModePaiementSalaire {
    VIREMENT("Virement bancaire"),
    CHEQUE("Chèque bancaire"),
    ESPECES("Espèces");

    private final String libelle;

    ModePaiementSalaire(String libelle) {
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
