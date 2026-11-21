package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilisateurServiceTest {

    private UtilisateurService service;
    private AuditService audit;

    @BeforeEach
    void setUp() {
        Clock horloge = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        audit = new AuditService(new InMemoryAuditDao(), horloge, () -> "Testeur (Gérant)");
        service = new UtilisateurService(new InMemoryUtilisateurDao(), audit);
    }

    @Test
    void creerUtilisateurValideLeSauvegardeAvecMotDePasseHache() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertNotNull(u.getId());
        assertNotEquals("motdepasse1", u.getMotDePasseHash());
        assertTrue(PasswordHasher.verifier("motdepasse1", u.getMotDePasseHash()));
    }

    @Test
    void emailDejaUtiliseEstRefuseMemeAvecMajuscules() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "SARA@translogix.ma", "motdepasse2", Role.RH));
    }

    @Test
    void emailInvalideEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "pas-un-email", "motdepasse2", Role.RH));
    }

    @Test
    void motDePasseTropCourtEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "ali@translogix.ma", "court", Role.RH));
    }

    @Test
    void dernierGerantNePeutPasEtreSupprime() {
        Utilisateur gerant = service.creer("Principal", "Admin", "admin@translogix.ma", "motdepasse1", Role.GERANT);

        assertThrows(ValidationException.class, () -> service.supprimer(gerant.getId()));
    }

    @Test
    void modifierMetAJourLesChamps() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.modifier(u.getId(), "Alami", "Sara", "sara@translogix.ma", Role.COMPTABLE, false);

        Utilisateur modifie = service.lister().get(0);
        assertEquals(Role.COMPTABLE, modifie.getRole());
        assertEquals(false, modifie.isActif());
    }

    // ---------------------------------------------------------------- audit

    @Test
    void creationEstJournalisee() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        List<EvenementAudit> journal = audit.lister();
        assertEquals(1, journal.size());
        EvenementAudit e = journal.get(0);
        assertEquals(UtilisateurService.MODULE_AUDIT, e.getModule());
        assertEquals("Testeur (Gérant)", e.getUtilisateur());
        assertTrue(e.getAction().contains("Création"));
        assertTrue(e.getAction().contains("sara@translogix.ma"));
        assertEquals(ResultatAudit.REUSSI, e.getResultat());
    }

    @Test
    void modificationEtSuppressionSontJournalisees() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.modifier(u.getId(), "Alami", "Sara", "sara@translogix.ma", Role.RH, true);
        service.supprimer(u.getId());

        List<EvenementAudit> journal = audit.lister(); // most recent first
        assertEquals(3, journal.size());
        assertTrue(journal.get(0).getAction().startsWith("Suppression du compte"));
        assertTrue(journal.get(1).getAction().startsWith("Modification du compte"));
        assertTrue(journal.stream().allMatch(e -> e.getResultat() == ResultatAudit.REUSSI));
    }

    @Test
    void suppressionRefuseeEstJournaliseeCommeRefusee() {
        Utilisateur gerant = service.creer("Principal", "Admin", "admin@translogix.ma", "motdepasse1", Role.GERANT);

        assertThrows(ValidationException.class, () -> service.supprimer(gerant.getId()));

        EvenementAudit dernier = audit.lister().get(0);
        assertEquals(ResultatAudit.REFUSE, dernier.getResultat());
        assertTrue(dernier.getAction().contains("compte Gérant"));
        assertEquals(1, service.lister().size());
    }

    // ---------------------------------------------------------------- single, protected Gérant

    private Utilisateur gerant() {
        return service.creer("Bennani", "Karim", "pdg@translogix.ma", "motdepasse1", Role.GERANT);
    }

    @Test
    void premierGerantEstAccepte() {
        Utilisateur g = gerant();

        assertEquals(Role.GERANT, g.getRole());
        assertEquals(g.getId(), service.gerantExistant().orElseThrow().getId());
    }

    @Test
    void unSecondGerantEstRefuseEtJournalise() {
        gerant();

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.GERANT));

        assertTrue(e.getMessage().contains("déjà un compte Gérant"));
        assertEquals(1, service.lister().size());
        assertEquals(ResultatAudit.REFUSE, audit.lister().get(0).getResultat());
    }

    @Test
    void promouvoirUnAutreUtilisateurEnGerantEstRefuse() {
        gerant();
        Utilisateur sara = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertThrows(ValidationException.class,
                () -> service.modifier(sara.getId(), "Alami", "Sara", "sara@translogix.ma", Role.GERANT, true));

        assertEquals(Role.DISPATCHER, sara.getRole());
        assertEquals(ResultatAudit.REFUSE, audit.lister().get(0).getResultat());
    }

    @Test
    void leGerantNePeutPasEtreRetrograde() {
        Utilisateur g = gerant();

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.modifier(g.getId(), "Bennani", "Karim", "pdg@translogix.ma", Role.RH, true));

        assertTrue(e.getMessage().contains("rôle du Gérant"));
        assertEquals(Role.GERANT, g.getRole());
        assertEquals(ResultatAudit.REFUSE, audit.lister().get(0).getResultat());
    }

    @Test
    void leGerantNePeutPasEtreDesactive() {
        Utilisateur g = gerant();

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.modifier(g.getId(), "Bennani", "Karim", "pdg@translogix.ma", Role.GERANT, false));

        assertTrue(e.getMessage().contains("désactivé"));
        assertTrue(g.isActif());
        assertEquals(ResultatAudit.REFUSE, audit.lister().get(0).getResultat());
    }

    @Test
    void leGerantNePeutPasEtreSupprimeMemeSIlNEstPasSeul() {
        Utilisateur g = gerant();
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        ValidationException e = assertThrows(ValidationException.class, () -> service.supprimer(g.getId()));

        assertTrue(e.getMessage().contains("ne peut pas être supprimé"));
        assertTrue(service.gerantExistant().isPresent());
    }

    @Test
    void leGerantPeutModifierSonNomEtSonEmail() {
        Utilisateur g = gerant();

        service.modifier(g.getId(), "Bennani", "Karim-Ali", "karim@translogix.ma", Role.GERANT, true);

        Utilisateur modifie = service.gerantExistant().orElseThrow();
        assertEquals("Karim-Ali", modifie.getPrenom());
        assertEquals("karim@translogix.ma", modifie.getEmail());
        assertEquals(ResultatAudit.REUSSI, audit.lister().get(0).getResultat());
    }

    @Test
    void erreurDeSaisieNEstPasJournalisee() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "pas-un-email", "motdepasse2", Role.RH));

        assertTrue(audit.lister().isEmpty());
    }

    // ------------------------------------------------------------------ multi-criteria search

    private void troisComptes() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
        Utilisateur ali = service.creer("Bennani", "Ali", "ali@translogix.ma", "motdepasse1", Role.DISPATCHER);
        service.modifier(ali.getId(), "Bennani", "Ali", "ali@translogix.ma", Role.DISPATCHER, false);
        service.creer("Élhadi", "Nora", "nora@translogix.ma", "motdepasse1", Role.RH);
    }

    private List<String> emails(UtilisateurService.CritereUtilisateur critere) {
        return service.filtrer(critere).stream().map(Utilisateur::getEmail).sorted().toList();
    }

    @Test
    void criteresSeCombinentEnEt() {
        troisComptes();

        assertEquals(List.of("ali@translogix.ma", "sara@translogix.ma"),
                emails(new UtilisateurService.CritereUtilisateur(null, Role.DISPATCHER, null)));
        assertEquals(List.of("sara@translogix.ma"),
                emails(new UtilisateurService.CritereUtilisateur(null, Role.DISPATCHER, true)));
        assertEquals(List.of("ali@translogix.ma"),
                emails(new UtilisateurService.CritereUtilisateur("benn", Role.DISPATCHER, false)));
        assertEquals(List.of(), emails(new UtilisateurService.CritereUtilisateur("sara", Role.RH, null)));
    }

    @Test
    void texteCherche_dansNomPrenomEmail_sansAccents() {
        troisComptes();

        assertEquals(List.of("nora@translogix.ma"),
                emails(new UtilisateurService.CritereUtilisateur("ELHADI", null, null)));
        assertEquals(List.of("ali@translogix.ma"), emails(new UtilisateurService.CritereUtilisateur("ali@", null, null)));
    }

    @Test
    void sansCritereToutEstRenvoye() {
        troisComptes();

        assertEquals(3, service.filtrer(UtilisateurService.CritereUtilisateur.AUCUN).size());
        assertEquals(3, service.filtrer(null).size());
        assertEquals(3, service.filtrer(new UtilisateurService.CritereUtilisateur("  ", null, null)).size());
    }

    // ------------------------------------------------------------------ right-click actions

    private Utilisateur relu(Long id) {
        return service.lister().stream().filter(u -> u.getId().equals(id)).findFirst().orElseThrow();
    }

    private List<String> actionsAudit() {
        return audit.lister().stream().map(EvenementAudit::getAction).toList();
    }

    @Test
    void changerStatutDesactiveEtReactiveAvecAudit() {
        Utilisateur sara = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.changerStatut(sara.getId(), false);
        assertFalse(relu(sara.getId()).isActif());
        service.changerStatut(sara.getId(), true);
        assertTrue(relu(sara.getId()).isActif());

        assertEquals("Activation du compte sara@translogix.ma", actionsAudit().get(0));
        assertEquals("Désactivation du compte sara@translogix.ma", actionsAudit().get(1));
    }

    @Test
    void changerStatutSansChangementNeJournaliseRien() {
        Utilisateur sara = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
        int avant = audit.lister().size();

        service.changerStatut(sara.getId(), true);

        assertEquals(avant, audit.lister().size());
    }

    @Test
    void gerantNePeutPasEtreDesactiveParLeMenu() {
        Utilisateur gerant = service.creer("Chef", "Karim", "chef@translogix.ma", "motdepasse1", Role.GERANT);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.changerStatut(gerant.getId(), false));

        assertEquals("Le compte Gérant ne peut pas être désactivé.", ex.getMessage());
        assertTrue(relu(gerant.getId()).isActif());
        assertEquals(ResultatAudit.REFUSE, audit.lister().get(0).getResultat());
    }

    @Test
    void reinitialiserMotDePasseRemplaceLeHashSansLeJournaliser() {
        Utilisateur sara = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.reinitialiserMotDePasse(sara.getId(), "nouveauSecret9", "nouveauSecret9");

        String hash = relu(sara.getId()).getMotDePasseHash();
        assertTrue(PasswordHasher.verifier("nouveauSecret9", hash));
        assertFalse(PasswordHasher.verifier("motdepasse1", hash));
        assertEquals("Réinitialisation du mot de passe de sara@translogix.ma", actionsAudit().get(0));
        assertTrue(actionsAudit().stream().noneMatch(a -> a.contains("nouveauSecret9")));
    }

    @Test
    void reinitialiserMotDePasseRefuseTropCourtOuDifferent() {
        Utilisateur sara = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertEquals("Le mot de passe doit contenir au moins 8 caractères.", assertThrows(ValidationException.class,
                () -> service.reinitialiserMotDePasse(sara.getId(), "court", "court")).getMessage());
        assertEquals("Les deux mots de passe ne sont pas identiques.", assertThrows(ValidationException.class,
                () -> service.reinitialiserMotDePasse(sara.getId(), "motdepasse2", "motdepasse3")).getMessage());
        assertTrue(PasswordHasher.verifier("motdepasse1", relu(sara.getId()).getMotDePasseHash()));
    }
}
