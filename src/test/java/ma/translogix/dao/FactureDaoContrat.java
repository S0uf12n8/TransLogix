package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every FactureDao must do, whatever the storage. A parent client is created first so the same
 * tests can run against the in-memory maps and against MySQL with its foreign key.
 */
public abstract class FactureDaoContrat {

    protected FactureDao dao;
    protected ClientDao clients;

    protected abstract FactureDao daoVide();

    protected abstract ClientDao clientDaoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
        clients = clientDaoVide();
    }

    protected Client client(String nom, String ice) {
        return clients.save(new Client(null, nom, ice, null, null, null, 30));
    }

    protected static Facture facture(Client client, String numero, String montantTtc, StatutFacture statut) {
        Facture facture = new Facture();
        facture.setNumero(numero);
        facture.setClientId(client.getId());
        facture.setReferenceTrajet("TRJ-" + numero);
        facture.setMontantHT(new BigDecimal("100.00"));
        facture.setTauxTva(new BigDecimal("14.00"));
        facture.setMontantTva(new BigDecimal("14.00"));
        facture.setMontantTtc(new BigDecimal(montantTtc));
        facture.setDateEmission(LocalDate.of(2026, 11, 5));
        facture.setDateEcheance(LocalDate.of(2026, 12, 5));
        facture.setStatut(statut);
        return facture;
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Client client = client("Atlas Transport", "ICE-F001");
        Facture facture = facture(client, "FACT-2026-001", "114.00", StatutFacture.PAYEE);
        facture.setMissionId(7_001L);
        facture.setReferenceTrajet("TRJ-ATLAS-001");
        facture.setDateEmission(LocalDate.of(2026, 10, 1));
        facture.setDateEcheance(LocalDate.of(2026, 10, 31));

        Facture cree = dao.save(facture);
        assertNotNull(cree.getId());
        Facture relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("FACT-2026-001", relu.getNumero());
        assertEquals(client.getId(), relu.getClientId());
        assertEquals(Long.valueOf(7_001L), relu.getMissionId());
        assertEquals("TRJ-ATLAS-001", relu.getReferenceTrajet());
        assertEquals(new BigDecimal("100.00"), relu.getMontantHT());
        assertEquals(new BigDecimal("14.00"), relu.getTauxTva());
        assertEquals(new BigDecimal("14.00"), relu.getMontantTva());
        assertEquals(new BigDecimal("114.00"), relu.getMontantTtc());
        assertEquals(LocalDate.of(2026, 10, 1), relu.getDateEmission());
        assertEquals(LocalDate.of(2026, 10, 31), relu.getDateEcheance());
        assertEquals(StatutFacture.PAYEE, relu.getStatut());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        Client client = client("Atlas Transport", "ICE-F002");
        dao.save(facture(client, "FACT-2026-002", "114.00", StatutFacture.EN_ATTENTE));
        dao.save(facture(client, "FACT-2026-003", "228.00", StatutFacture.PAYEE));

        List<Facture> factures = dao.findAll();
        assertNotEquals(factures.get(0).getId(), factures.get(1).getId());
        assertEquals(List.of("FACT-2026-002", "FACT-2026-003"),
                factures.stream().map(Facture::getNumero).toList());
    }

    @Test
    void idInconnuDonneVide() {
        assertTrue(dao.findById(999_999L).isEmpty());
    }

    @Test
    void findByClientNeRenvoieQueLesFacturesDuClient() {
        Client atlas = client("Atlas Transport", "ICE-F003");
        Client sud = client("Logistique Sud", "ICE-F004");
        dao.save(facture(atlas, "FACT-2026-004", "114.00", StatutFacture.EN_ATTENTE));
        dao.save(facture(sud, "FACT-2026-005", "228.00", StatutFacture.PAYEE));

        assertEquals(List.of("FACT-2026-004"),
                dao.findByClient(atlas.getId()).stream().map(Facture::getNumero).toList());
        assertEquals(List.of("FACT-2026-005"),
                dao.findByClient(sud.getId()).stream().map(Facture::getNumero).toList());
    }

    @Test
    void findByMissionTrouveLaFacture() {
        Client client = client("Atlas Transport", "ICE-F005");
        Facture facture = facture(client, "FACT-2026-006", "114.00", StatutFacture.EN_ATTENTE);
        facture.setMissionId(7_100L);
        dao.save(facture);

        assertEquals("FACT-2026-006", dao.findByMission(7_100L).orElseThrow().getNumero());
        assertTrue(dao.findByMission(99_999L).isEmpty());
    }

    @Test
    void updateRemplaceLesChamps() {
        Client atlas = client("Atlas Transport", "ICE-F006");
        Client sud = client("Logistique Sud", "ICE-F007");
        Facture facture = facture(atlas, "FACT-2026-007", "114.00", StatutFacture.EN_ATTENTE);
        facture.setMissionId(7_200L);
        dao.save(facture);
        facture.setNumero("FACT-2026-008");
        facture.setClientId(sud.getId());
        facture.setMissionId(7_201L);
        facture.setReferenceTrajet("TRJ-MODIFIEE");
        facture.setMontantHT(new BigDecimal("200.00"));
        facture.setTauxTva(new BigDecimal("14.00"));
        facture.setMontantTva(new BigDecimal("28.00"));
        facture.setMontantTtc(new BigDecimal("228.00"));
        facture.setDateEmission(LocalDate.of(2026, 9, 1));
        facture.setDateEcheance(LocalDate.of(2026, 9, 30));
        facture.setStatut(StatutFacture.ANNULEE);

        dao.update(facture);

        Facture relu = dao.findById(facture.getId()).orElseThrow();
        assertEquals("FACT-2026-008", relu.getNumero());
        assertEquals(sud.getId(), relu.getClientId());
        assertEquals(Long.valueOf(7_201L), relu.getMissionId());
        assertEquals("TRJ-MODIFIEE", relu.getReferenceTrajet());
        assertEquals(new BigDecimal("200.00"), relu.getMontantHT());
        assertEquals(new BigDecimal("228.00"), relu.getMontantTtc());
        assertEquals(LocalDate.of(2026, 9, 1), relu.getDateEmission());
        assertEquals(LocalDate.of(2026, 9, 30), relu.getDateEcheance());
        assertEquals(StatutFacture.ANNULEE, relu.getStatut());
    }

    @Test
    void updateDUnIdInconnuEstRefuse() {
        Client client = client("Atlas Transport", "ICE-F008");
        Facture fantome = facture(client, "FACT-2026-009", "114.00", StatutFacture.EN_ATTENTE);
        fantome.setId(999_999L);

        assertThrows(IllegalArgumentException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeEtIdInconnuNeFaitRien() {
        Client client = client("Atlas Transport", "ICE-F009");
        Facture facture = dao.save(facture(client, "FACT-2026-010", "114.00", StatutFacture.EN_ATTENTE));

        dao.delete(facture.getId());
        dao.delete(999_999L);

        assertTrue(dao.findById(facture.getId()).isEmpty());
        assertTrue(dao.findByClient(client.getId()).isEmpty());
    }

    @Test
    void searchSansCritereRenvoieTout() {
        Client client = client("Atlas Transport", "ICE-F010");
        dao.save(facture(client, "FACT-2026-011", "114.00", StatutFacture.EN_ATTENTE));
        dao.save(facture(client, "FACT-2026-012", "228.00", StatutFacture.PAYEE));

        assertEquals(List.of("FACT-2026-011", "FACT-2026-012"),
                dao.search(CritereFacture.TOUT).stream().map(Facture::getNumero).toList());
    }

    @Test
    void searchFiltreParMotCleSurNumeroReferenceEtIdsClients() {
        Client atlas = client("Atlas Transport", "ICE-F011");
        Client sud = client("Logistique Sud", "ICE-F012");
        dao.save(facture(atlas, "FACT-2026-013", "114.00", StatutFacture.EN_ATTENTE));
        Facture factureSud = facture(sud, "FACT-2026-014", "228.00", StatutFacture.PAYEE);
        factureSud.setReferenceTrajet("TRJ-SUD-014");
        dao.save(factureSud);
        Facture avecPourcent = facture(sud, "FACT-2026-015%", "342.00", StatutFacture.EN_ATTENTE);
        dao.save(avecPourcent);

        assertEquals(List.of("FACT-2026-014"),
                dao.search(new CritereFacture("sud", List.of(), null, null, null, null, null, null))
                        .stream().map(Facture::getNumero).toList());
        assertEquals(List.of("FACT-2026-013"),
                dao.search(new CritereFacture("atlas", List.of(atlas.getId()), null, null, null, null, null, null))
                        .stream().map(Facture::getNumero).toList());
        assertEquals(List.of("FACT-2026-015%"),
                dao.search(new CritereFacture("%", List.of(), null, null, null, null, null, null))
                        .stream().map(Facture::getNumero).toList());
    }

    @Test
    void searchCombineTousLesFiltres() {
        Client atlas = client("Atlas Transport", "ICE-F013");
        Client sud = client("Logistique Sud", "ICE-F014");
        dao.save(facture(atlas, "FACT-2026-016", "114.00", StatutFacture.EN_ATTENTE));
        dao.save(facture(atlas, "FACT-2026-017", "114.00", StatutFacture.PAYEE));
        dao.save(facture(sud, "FACT-2026-018", "114.00", StatutFacture.EN_ATTENTE));
        Facture horsDate = facture(atlas, "FACT-2026-019", "114.00", StatutFacture.EN_ATTENTE);
        horsDate.setDateEmission(LocalDate.of(2026, 10, 1));
        dao.save(horsDate);
        dao.save(facture(atlas, "FACT-2026-020", "500.00", StatutFacture.EN_ATTENTE));

        CritereFacture critere = new CritereFacture("016", List.of(atlas.getId()), atlas.getId(),
                StatutFacture.EN_ATTENTE.getLibelle(), LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), new BigDecimal("100.00"), new BigDecimal("200.00"));

        assertEquals(List.of("FACT-2026-016"),
                dao.search(critere).stream().map(Facture::getNumero).toList());
    }
}
