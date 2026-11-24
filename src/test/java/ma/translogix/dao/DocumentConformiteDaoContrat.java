package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every DocumentConformiteDao must do, whatever the storage. The memory and MySQL implementations
 * pass the same tests; only the way empty DAOs are built changes.
 */
public abstract class DocumentConformiteDaoContrat {

    protected DocumentConformiteDao dao;
    protected EmployeDao employes;

    protected abstract DocumentConformiteDao daoVide();

    /** Used to create the parent employee required by the MySQL foreign key. */
    protected abstract EmployeDao employeDaoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
        employes = employeDaoVide();
    }

    protected Long employe(String matricule, String cin) {
        return employes.save(new Employe(null, matricule, "EmployeFictif", "PrenomFictif", cin, null,
                Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2023, 1, 10), true, null)).getId();
    }

    protected static DocumentConformite document(Long employeId, TypeDocument type, String numero,
                                                 LocalDate expiration) {
        return new DocumentConformite(null, employeId, type, numero, null, expiration);
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Long employeId = employe("MAT-DOC-001", "ZZ-DOC-001");
        DocumentConformite cree = dao.save(new DocumentConformite(null, employeId, TypeDocument.PERMIS,
                "PERMIS-TEST-001", "EC", LocalDate.of(2030, 5, 20)));

        assertNotNull(cree.getId());
        DocumentConformite relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals(employeId, relu.getEmployeId());
        assertEquals(TypeDocument.PERMIS, relu.getType());
        assertEquals("PERMIS-TEST-001", relu.getNumero());
        assertEquals("EC", relu.getCategorie());
        assertEquals(LocalDate.of(2030, 5, 20), relu.getDateExpiration());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        Long employeId = employe("MAT-DOC-002", "ZZ-DOC-002");
        dao.save(document(employeId, TypeDocument.PERMIS, "PERMIS-TEST-002", LocalDate.of(2031, 1, 1)));
        dao.save(document(employeId, TypeDocument.FIMC, "FIMC-TEST-002", LocalDate.of(2031, 2, 1)));

        List<DocumentConformite> documents = dao.findAll();
        assertEquals(2, documents.size());
        assertNotEquals(documents.get(0).getId(), documents.get(1).getId());
    }

    @Test
    void champsNullConserves() {
        Long employeId = employe("MAT-DOC-003", "ZZ-DOC-003");
        DocumentConformite cree = dao.save(document(employeId, TypeDocument.VISITE_MEDICALE, null,
                LocalDate.of(2032, 3, 15)));

        DocumentConformite relu = dao.findById(cree.getId()).orElseThrow();
        assertNull(relu.getNumero());
        assertNull(relu.getCategorie());
        assertEquals(TypeDocument.VISITE_MEDICALE, relu.getType());
    }

    @Test
    void findByEmployeNeRenvoieQueSesDocuments() {
        Long premier = employe("MAT-DOC-004", "ZZ-DOC-004");
        Long second = employe("MAT-DOC-005", "ZZ-DOC-005");
        dao.save(document(premier, TypeDocument.PERMIS, "PERMIS-TEST-004", LocalDate.of(2033, 4, 1)));
        dao.save(document(second, TypeDocument.VISITE_MEDICALE, null, LocalDate.of(2033, 5, 1)));

        List<DocumentConformite> documents = dao.findByEmploye(premier);
        assertEquals(1, documents.size());
        assertEquals(TypeDocument.PERMIS, documents.get(0).getType());
        assertTrue(dao.findByEmploye(999_999L).isEmpty());
        assertTrue(dao.findByEmploye(null).isEmpty());
    }

    @Test
    void idInconnuOuNullDonneVide() {
        assertTrue(dao.findById(999_999L).isEmpty());
        assertTrue(dao.findById(null).isEmpty());
    }

    @Test
    void updateRemplaceTousLesChampsYComprisNull() {
        Long premier = employe("MAT-DOC-006", "ZZ-DOC-006");
        Long second = employe("MAT-DOC-007", "ZZ-DOC-007");
        DocumentConformite cree = dao.save(document(premier, TypeDocument.PERMIS, "PERMIS-TEST-006",
                LocalDate.of(2034, 6, 1)));

        cree.setEmployeId(second);
        cree.setType(TypeDocument.VISITE_MEDICALE);
        cree.setNumero(null);
        cree.setCategorie(null);
        cree.setDateExpiration(LocalDate.of(2035, 7, 2));
        dao.update(cree);

        DocumentConformite relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals(second, relu.getEmployeId());
        assertEquals(TypeDocument.VISITE_MEDICALE, relu.getType());
        assertNull(relu.getNumero());
        assertNull(relu.getCategorie());
        assertEquals(LocalDate.of(2035, 7, 2), relu.getDateExpiration());
    }

    @Test
    void updateDUnIdInconnuEstRefuse() {
        Long employeId = employe("MAT-DOC-008", "ZZ-DOC-008");
        DocumentConformite fantome = document(employeId, TypeDocument.FIMC, "FIMC-TEST-008",
                LocalDate.of(2036, 8, 1));
        fantome.setId(999_999L);

        assertThrows(IllegalArgumentException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeEtIdInconnuNeFaitRien() {
        Long employeId = employe("MAT-DOC-009", "ZZ-DOC-009");
        DocumentConformite cree = dao.save(document(employeId, TypeDocument.PERMIS, "PERMIS-TEST-009",
                LocalDate.of(2037, 9, 1)));

        dao.delete(cree.getId());
        dao.delete(999_999L);
        dao.delete(null);

        assertTrue(dao.findById(cree.getId()).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }
}
