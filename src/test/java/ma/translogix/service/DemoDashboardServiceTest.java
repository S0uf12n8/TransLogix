package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.service.DashboardService.Alerte;
import ma.translogix.service.DashboardService.ChiffreMensuel;
import ma.translogix.service.DashboardService.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DemoDashboardServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

    private AuditService audit;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        audit = new AuditService(new InMemoryAuditDao(), HORLOGE, () -> "Test");
        service = new DemoDashboardService(audit, HORLOGE);
    }

    @Test
    void estMarqueDemo() {
        assertTrue(service.estDemo());
    }

    @Test
    void sixMoisConsecutifsFinissantLeMoisCourant() {
        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(6);

        assertEquals(6, mois.size());
        assertEquals(YearMonth.of(2026, 6), mois.get(0).mois());
        assertEquals(YearMonth.of(2026, 11), mois.get(5).mois());
        for (int i = 1; i < mois.size(); i++) {
            assertEquals(mois.get(i - 1).mois().plusMonths(1), mois.get(i).mois());
        }
        assertTrue(mois.stream().allMatch(m -> m.montantMad() > 0));
    }

    @Test
    void chiffreDuMoisCourantCorrespondAuKpi() {
        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(2);

        long precedent = mois.get(0).montantMad();
        long courant = mois.get(1).montantMad();
        assertEquals(452_000, courant);
        assertEquals(14, Math.round((courant - precedent) * 100.0 / precedent)); // "+14 %" on the KPI card
    }

    @Test
    void nombreDeMoisInvalideEstRefuse() {
        assertEquals(3, service.chiffreAffairesParMois(3).size());
        assertThrows(IllegalArgumentException.class, () -> service.chiffreAffairesParMois(0));
        assertThrows(IllegalArgumentException.class, () -> service.chiffreAffairesParMois(13));
    }

    @Test
    void missionsSuiventLOrdreDuCycleDeVie() {
        List<Part> parts = service.missionsParStatut();

        assertEquals(DashboardService.STATUTS_MISSION, parts.stream().map(Part::libelle).toList());
        assertTrue(parts.stream().allMatch(p -> p.nombre() >= 0));
    }

    @Test
    void flotteCorrespondAuKpi() {
        List<Part> flotte = service.etatFlotte();

        assertEquals(List.of("En mission", "Disponible", "En maintenance"),
                flotte.stream().map(Part::libelle).toList());
        assertEquals(22, flotte.stream().mapToInt(Part::nombre).sum());
        assertEquals(18, flotte.get(0).nombre() + flotte.get(1).nombre()); // "18 / 22" operational
        assertEquals(4, flotte.get(2).nombre());                          // "4 véhicules en maintenance"
    }

    @Test
    void alertesOntUnNiveauEtUneCible() {
        List<Alerte> alertes = service.alertesConformite();

        assertFalse(alertes.isEmpty());
        for (Alerte a : alertes) {
            assertNotNull(a.niveau(), a.titre());
            assertTrue(a.cibleFxml().matches("[a-z]+/[a-z-]+\\.fxml"), a.cibleFxml());
        }
        assertEquals(DashboardService.Niveau.CRITIQUE, alertes.get(0).niveau());
    }

    @Test
    void auditRenvoieLesCinqDerniersEvenementsReels() {
        IntStream.rangeClosed(1, 7).forEach(i ->
                audit.enregistrer("Utilisateurs", "action " + i, ResultatAudit.REUSSI));

        List<EvenementAudit> derniers = service.derniersEvenementsAudit(5);

        assertEquals(List.of("action 7", "action 6", "action 5", "action 4", "action 3"),
                derniers.stream().map(EvenementAudit::getAction).toList());
    }

    @Test
    void auditRenvoieMoinsSiLeJournalEstCourt() {
        assertTrue(service.derniersEvenementsAudit(5).isEmpty());

        audit.enregistrer("Utilisateurs", "seule action", ResultatAudit.REUSSI);

        assertEquals(1, service.derniersEvenementsAudit(5).size());
    }
}
