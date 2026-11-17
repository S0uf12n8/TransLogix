package ma.translogix.model;

import java.time.LocalDate;

/** A licence, FIMC card or medical visit of an employee ([M4]). Its status is computed, not stored. */
public class DocumentConformite {
    private Long id;
    private Long employeId;
    private TypeDocument type;
    /** Document number (licence number...); may be null for a medical visit. */
    private String numero;
    /** Licence category (C, EC...); null for the other documents. */
    private String categorie;
    private LocalDate dateExpiration;

    public DocumentConformite() {
    }

    public DocumentConformite(Long id, Long employeId, TypeDocument type, String numero, String categorie,
                              LocalDate dateExpiration) {
        this.id = id;
        this.employeId = employeId;
        this.type = type;
        this.numero = numero;
        this.categorie = categorie;
        this.dateExpiration = dateExpiration;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeId() { return employeId; }
    public void setEmployeId(Long employeId) { this.employeId = employeId; }

    public TypeDocument getType() { return type; }
    public void setType(TypeDocument type) { this.type = type; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
}
