package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every EmployeDao must do, whatever the storage. The memory and MySQL implementations pass the
 * same tests; only the way an empty DAO is built changes.
 */
public abstract class EmployeDaoContrat {

    protected EmployeDao dao;

    /** A DAO with no employee in it. */
    protected abstract EmployeDao daoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
    }

    protected static Employe employe(String matricule, String nom, String cin, Poste poste) {
        return new Employe(null, matricule, nom, "PrenomFictif", cin, null, poste, TypeContrat.CDI,
                LocalDate.of(2023, 1, 10), true, null);
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Employe cree = dao.save(new Employe(null, "MAT-TEST-001", "NomFictif", "PrenomFictif", "ZZ-TEST-001",
                "900000001", Poste.CHAUFFEUR, TypeContrat.CDD, LocalDate.of(2023, 4, 5), false, 4242L));

        assertNotNull(cree.getId());
        Employe relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("MAT-TEST-001", relu.getMatricule());
        assertEquals("NomFictif", relu.getNom());
        assertEquals("PrenomFictif", relu.getPrenom());
        assertEquals("ZZ-TEST-001", relu.getCin());
        assertEquals("900000001", relu.getCnss());
        assertEquals(Poste.CHAUFFEUR, relu.getPoste());
        assertEquals(TypeContrat.CDD, relu.getTypeContrat());
        assertEquals(LocalDate.of(2023, 4, 5), relu.getDateEmbauche());
        assertFalse(relu.isActif());
        assertEquals(4242L, relu.getUtilisateurId());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        dao.save(employe("MAT-TEST-002", "AlamiFictif", "ZZ-TEST-002", Poste.CHAUFFEUR));
        dao.save(employe("MAT-TEST-003", "BennaniFictif", "ZZ-TEST-003", Poste.ADMINISTRATIF));

        List<Employe> employes = dao.findAll();
        assertEquals(2, employes.size());
        assertNotEquals(employes.get(0).getId(), employes.get(1).getId());
        assertEquals(List.of("AlamiFictif", "BennaniFictif"),
                employes.stream().map(Employe::getNom).toList());
    }

    @Test
    void accentsEtValeursNullConserves() {
        Employe cree = dao.save(new Employe(null, "MAT-TEST-004", "ÉlhaniFictif", "NoûrFictif", "ZZ-TEST-004",
                null, Poste.AGENT_DEPOT, TypeContrat.ESSAI, LocalDate.of(2024, 2, 29), true, null));

        Employe relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("ÉlhaniFictif", relu.getNom());
        assertEquals("NoûrFictif", relu.getPrenom());
        assertNull(relu.getCnss());
        assertNull(relu.getUtilisateurId());
        assertEquals(Poste.AGENT_DEPOT, relu.getPoste());
        assertEquals(TypeContrat.ESSAI, relu.getTypeContrat());
    }

    @Test
    void idInconnuOuNullDonneVide() {
        assertTrue(dao.findById(999_999L).isEmpty());
        assertTrue(dao.findById(null).isEmpty());
    }

    @Test
    void findByPosteFiltreSansFiltrerActif() {
        dao.save(employe("MAT-TEST-005", "ActifFictif", "ZZ-TEST-005", Poste.CHAUFFEUR));
        Employe inactif = employe("MAT-TEST-006", "InactifFictif", "ZZ-TEST-006", Poste.CHAUFFEUR);
        inactif.setActif(false);
        dao.save(inactif);
        dao.save(employe("MAT-TEST-007", "MecanicienFictif", "ZZ-TEST-007", Poste.MECANICIEN));

        List<Employe> chauffeurs = dao.findByPoste(Poste.CHAUFFEUR);
        assertEquals(2, chauffeurs.size());
        assertTrue(chauffeurs.stream().anyMatch(e -> e.getId().equals(inactif.getId())));
        assertTrue(dao.findByPoste(null).isEmpty());
    }

    @Test
    void findByUtilisateurIdTrouveEtIgnoreNull() {
        Employe employe = employe("MAT-TEST-008", "LieFictif", "ZZ-TEST-008", Poste.CHAUFFEUR);
        employe.setUtilisateurId(77L);
        Employe cree = dao.save(employe);

        assertEquals(cree.getId(), dao.findByUtilisateurId(77L).orElseThrow().getId());
        assertTrue(dao.findByUtilisateurId(999_999L).isEmpty());
        assertTrue(dao.findByUtilisateurId(null).isEmpty());
    }

    @Test
    void findByCinEtMatriculeTrouventEtIgnorentNull() {
        Employe cree = dao.save(employe("MAT-TEST-009", "CherkaouiFictif", "ZZ-TEST-009", Poste.CHAUFFEUR));

        assertEquals(cree.getId(), dao.findByCin("ZZ-TEST-009").orElseThrow().getId());
        assertEquals(cree.getId(), dao.findByMatricule("MAT-TEST-009").orElseThrow().getId());
        assertTrue(dao.findByCin("ZZ-TEST-999").isEmpty());
        assertTrue(dao.findByMatricule("MAT-TEST-999").isEmpty());
        assertTrue(dao.findByCin(null).isEmpty());
        assertTrue(dao.findByMatricule(null).isEmpty());
    }

    @Test
    void updateRemplaceTousLesChamps() {
        Employe cree = dao.save(employe("MAT-TEST-010", "AvantFictif", "ZZ-TEST-010", Poste.CHAUFFEUR));
        cree.setMatricule("MAT-TEST-011");
        cree.setNom("ApresFictif");
        cree.setPrenom("NouveauPrenomFictif");
        cree.setCin("ZZ-TEST-011");
        cree.setCnss("900000011");
        cree.setPoste(Poste.MECANICIEN);
        cree.setTypeContrat(TypeContrat.ESSAI);
        cree.setDateEmbauche(LocalDate.of(2022, 7, 14));
        cree.setActif(false);
        cree.setUtilisateurId(88L);

        dao.update(cree);

        Employe relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("MAT-TEST-011", relu.getMatricule());
        assertEquals("ApresFictif", relu.getNom());
        assertEquals("NouveauPrenomFictif", relu.getPrenom());
        assertEquals("ZZ-TEST-011", relu.getCin());
        assertEquals("900000011", relu.getCnss());
        assertEquals(Poste.MECANICIEN, relu.getPoste());
        assertEquals(TypeContrat.ESSAI, relu.getTypeContrat());
        assertEquals(LocalDate.of(2022, 7, 14), relu.getDateEmbauche());
        assertFalse(relu.isActif());
        assertEquals(88L, relu.getUtilisateurId());
    }

    @Test
    void updateDUnIdInconnuEstRefuse() {
        Employe fantome = employe("MAT-TEST-012", "FantomeFictif", "ZZ-TEST-012", Poste.CHAUFFEUR);
        fantome.setId(999_999L);

        assertThrows(IllegalArgumentException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeEtIdInconnuNeFaitRien() {
        Employe cree = dao.save(employe("MAT-TEST-013", "SupprimeFictif", "ZZ-TEST-013", Poste.CHAUFFEUR));

        dao.delete(cree.getId());
        dao.delete(999_999L);
        dao.delete(null);

        assertTrue(dao.findById(cree.getId()).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }
}
