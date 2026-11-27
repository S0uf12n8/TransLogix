package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryPaieDao;
import ma.translogix.model.Employe;
import ma.translogix.model.ModePaiementSalaire;
import ma.translogix.model.Paie;
import ma.translogix.model.Poste;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.StatutPaie;
import ma.translogix.model.TypeContrat;
import ma.translogix.service.PaieService.CriterePaie;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Payroll rules: real entered salaries, computed net, one record per employee/month, audited actions. */
class PaieServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final YearMonth NOVEMBRE = YearMonth.of(2026, 11);

    private InMemoryPaieDao dao;
    private EmployeService employes;
    private AuditService audit;
    private PaieService service;
    private Employe employe;
    private Employe autre;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPaieDao();
        InMemoryEmployeDao employeDao = new InMemoryEmployeDao();
        employes = new EmployeService(employeDao, HORLOGE);
        audit = new AuditService(new InMemoryAuditDao(), HORLOGE, () -> "Comptable Test");
        service = new PaieService(dao, employes, audit, HORLOGE);
        employe = employes.creer("Bennani", "Mohammed", "ZZ-000201", "900000201", Poste.CHAUFFEUR,
                TypeContrat.CDI, LocalDate.of(2023, 1, 10));
        autre = employes.creer("Bouazzaoui", "Karima", "ZZ-000202", "900000202", Poste.ADMINISTRATIF,
                TypeContrat.CDI, LocalDate.of(2023, 2, 1));
    }

    private Paie fiche() {
        return service.creer(employe.getId(), NOVEMBRE, new BigDecimal("8000"),
                new BigDecimal("500"), new BigDecimal("1200"));
    }

    @Test
    void netEstCalculeAValeurSaisieEtLaFicheEstEnAttente() {
        var paie = fiche();

        assertEquals("PAI-202611-001", paie.getNumero());
        assertEquals(new BigDecimal("8000.00"), paie.getSalaireBase());
        assertEquals(new BigDecimal("500.00"), paie.getPrimes());
        assertEquals(new BigDecimal("1200.00"), paie.getDeductions());
        assertEquals(new BigDecimal("7300.00"), paie.getNet());
        assertEquals(StatutPaie.EN_ATTENTE, paie.getStatut());
        assertEquals("Paie", audit.lister().get(0).getModule());
        assertEquals(ResultatAudit.REUSSI, audit.lister().get(0).getResultat());
    }

    @Test
    void primesEtRetenuesSontOptionnelles() {
        var paie = service.creer(autre.getId(), NOVEMBRE, new BigDecimal("6000"), null, null);

        assertEquals(BigDecimal.ZERO.setScale(2), paie.getPrimes());
        assertEquals(BigDecimal.ZERO.setScale(2), paie.getDeductions());
        assertEquals(new BigDecimal("6000.00"), paie.getNet());
    }

    @Test
    void unSeulEnregistrementParEmployeEtParMois() {
        fiche();

        var erreur = assertThrows(ValidationException.class, () -> service.creer(employe.getId(), NOVEMBRE,
                new BigDecimal("8000"), null, null));

        assertTrue(erreur.getMessage().contains("existe déjà"));
        assertTrue(audit.lister().stream().anyMatch(e -> e.getResultat() == ResultatAudit.REFUSE));
    }

    @Test
    void deuxEmployesPeuventAvoirUneFicheSurLeMemeMois() {
        fiche();
        var seconde = service.creer(autre.getId(), NOVEMBRE, new BigDecimal("6000"), null, null);

        assertEquals("PAI-202611-002", seconde.getNumero());
    }

    @Test
    void montantsInvalidesSontRefuses() {
        assertThrows(ValidationException.class, () -> service.creer(employe.getId(), NOVEMBRE, null, null, null));
        assertThrows(ValidationException.class, () -> service.creer(employe.getId(), NOVEMBRE,
                BigDecimal.ZERO, null, null));
        assertThrows(ValidationException.class, () -> service.creer(employe.getId(), NOVEMBRE,
                new BigDecimal("1000"), new BigDecimal("-10"), null));
        assertThrows(ValidationException.class, () -> service.creer(employe.getId(), NOVEMBRE,
                new BigDecimal("1000"), null, new BigDecimal("2000")));
        assertThrows(ValidationException.class, () -> service.creer(employe.getId(), YearMonth.of(2026, 12),
                new BigDecimal("1000"), null, null));
    }

    @Test
    void validationPasseEnValideeEtRefuseUnSecondPassage() {
        var paie = fiche();

        service.valider(paie.getId());
        assertEquals(StatutPaie.VALIDEE, service.trouver(paie.getId()).getStatut());
        assertThrows(ValidationException.class, () -> service.valider(paie.getId()));
    }

    @Test
    void paiementRequiertUneFicheValideeEtEnregistreLesDetails() {
        var paie = fiche();

        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(),
                ModePaiementSalaire.VIREMENT, "VIR-PAIE-001", LocalDate.of(2026, 11, 15)));

        service.valider(paie.getId());
        service.enregistrerPaiement(paie.getId(), ModePaiementSalaire.VIREMENT, "VIR-PAIE-001",
                LocalDate.of(2026, 11, 15));

        var payee = service.trouver(paie.getId());
        assertEquals(StatutPaie.PAYEE, payee.getStatut());
        assertEquals(ModePaiementSalaire.VIREMENT, payee.getModePaiement());
        assertEquals("VIR-PAIE-001", payee.getReference());
        assertEquals(LocalDate.of(2026, 11, 15), payee.getDatePaiement());
    }

    @Test
    void paiementVerifieLaReferenceEtLaDate() {
        var paie = fiche();
        service.valider(paie.getId());

        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(), null,
                "VIR-PAIE-001", LocalDate.of(2026, 11, 15)));
        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(),
                ModePaiementSalaire.VIREMENT, "  ", LocalDate.of(2026, 11, 15)));
        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(),
                ModePaiementSalaire.VIREMENT, "VIR-PAIE-001", LocalDate.of(2026, 11, 18)));
        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(),
                ModePaiementSalaire.VIREMENT, "VIR-PAIE-001", LocalDate.of(2026, 10, 31)));
        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(paie.getId(),
                ModePaiementSalaire.ESPECES, null, LocalDate.of(2026, 11, 15)));
    }

    @Test
    void referenceDePaiementEstUnique() {
        var premiere = fiche();
        service.valider(premiere.getId());
        service.enregistrerPaiement(premiere.getId(), ModePaiementSalaire.VIREMENT, "VIR-COMMUN",
                LocalDate.of(2026, 11, 15));
        var seconde = service.creer(autre.getId(), NOVEMBRE, new BigDecimal("6000"), null, null);
        service.valider(seconde.getId());

        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(seconde.getId(),
                ModePaiementSalaire.CHEQUE, "vir-commun", LocalDate.of(2026, 11, 16)));
    }

    @Test
    void rechercheCombinePeriodeStatutEtTexte() {
        var paie = fiche();
        service.valider(paie.getId());
        service.creer(autre.getId(), YearMonth.of(2026, 10), new BigDecimal("6000"), null, null);

        assertEquals(1, service.rechercher(new CriterePaie(null, NOVEMBRE, StatutPaie.VALIDEE, null)).size());
        assertEquals(1, service.rechercher(new CriterePaie("bennani", null, null, null)).size());
        assertEquals(1, service.rechercher(new CriterePaie(null, null, null, autre.getId())).size());
        assertEquals("PAI-202611-001", service.rechercher(CriterePaie.AUCUN).get(0).getNumero());
    }

    @Test
    void syntheseAdditionneLesNetAffichesSansInventerDeChiffre() {
        var paie = fiche();
        service.valider(paie.getId());
        service.enregistrerPaiement(paie.getId(), ModePaiementSalaire.VIREMENT, "VIR-001",
                LocalDate.of(2026, 11, 15));
        service.creer(autre.getId(), NOVEMBRE, new BigDecimal("6000"), new BigDecimal("200"),
                new BigDecimal("100"));

        var synthese = service.synthese(service.lister());

        assertEquals(new BigDecimal("13400.00"), synthese.totalNet());
        assertEquals(new BigDecimal("7300.00"), synthese.totalPaye());
        assertEquals(new BigDecimal("6100.00"), synthese.totalImpaye());
        assertEquals(2, synthese.nombreFiches());
        assertEquals(1, synthese.nombrePayees());
        assertEquals(2, synthese.nombreEmployes());
    }

    @Test
    void employesActifsSontLesSeulsProposes() {
        employes.desactiver(autre.getId());

        List<Employe> proposes = service.employesActifs();

        assertEquals(List.of(employe.getId()), proposes.stream().map(Employe::getId).toList());
        assertNotNull(proposes.get(0).getMatricule());
    }
}
