package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
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

class AuthServiceTest {

    private HorlogeReglable horloge;
    private AuditService audit;
    private InMemoryUtilisateurDao dao;
    private UtilisateurService utilisateurs;
    private AuthService auth;
    private Utilisateur sara;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable(Instant.parse("2026-11-18T10:00:00Z"));
        dao = new InMemoryUtilisateurDao();
        audit = new AuditService(new InMemoryAuditDao(), horloge, () -> "Test");
        utilisateurs = new UtilisateurService(dao, audit);
        auth = new AuthService(dao, audit, horloge);
        sara = utilisateurs.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
    }

    @Test
    void bonMotDePasseRenvoieLeCompte() {
        Utilisateur connecte = auth.connecter("sara@translogix.ma", "motdepasse1");

        assertEquals(sara.getId(), connecte.getId());
        assertEquals(Role.DISPATCHER, connecte.getRole());
    }

    @Test
    void emailEnMajusculesEtEspacesEstAccepte() {
        assertEquals(sara.getId(), auth.connecter("  SARA@Translogix.ma ", "motdepasse1").getId());
    }

    @Test
    void mauvaisMotDePasseEstRefuseAvecLeMessageGenerique() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "motdepasse2"));
        assertEquals("Email ou mot de passe incorrect.", e.getMessage());
    }

    @Test
    void emailInconnuEstRefuseAvecLeMemeMessage() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("inconnu@translogix.ma", "motdepasse1"));
        assertEquals(AuthService.ECHEC, e.getMessage());
    }

    @Test
    void compteDesactiveEstRefuseMemeAvecLeBonMotDePasse() {
        utilisateurs.modifier(sara.getId(), "Alami", "Sara", "sara@translogix.ma", Role.DISPATCHER, false);

        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "motdepasse1"));
        assertEquals(AuthService.ECHEC, e.getMessage());
    }

    @Test
    void unAncienHashSha256EstRemplaceParPbkdf2ALaConnexion() throws Exception {
        sara.setMotDePasseHash(ancienHash("motdepasse1"));
        dao.update(sara);

        auth.connecter("sara@translogix.ma", "motdepasse1");

        String nouveau = dao.findById(sara.getId()).orElseThrow().getMotDePasseHash();
        assertTrue(nouveau.startsWith("pbkdf2-sha512$"), nouveau);
        assertTrue(PasswordHasher.verifier("motdepasse1", nouveau));
        assertEquals(sara.getId(), auth.connecter("sara@translogix.ma", "motdepasse1").getId());
    }

    @Test
    void unMauvaisMotDePasseNeTouchePasAuHash() throws Exception {
        String ancien = ancienHash("motdepasse1");
        sara.setMotDePasseHash(ancien);
        dao.update(sara);

        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", "faux"));

        assertEquals(ancien, dao.findById(sara.getId()).orElseThrow().getMotDePasseHash());
    }

    @Test
    void unHashAJourNEstPasReecritAChaqueConnexion() {
        String avant = dao.findById(sara.getId()).orElseThrow().getMotDePasseHash();

        auth.connecter("sara@translogix.ma", "motdepasse1");

        assertEquals(avant, dao.findById(sara.getId()).orElseThrow().getMotDePasseHash());
    }

    /** The iteration-2 format: Base64(salt) + ":" + Base64(SHA-256(salt + password)). */
    private static String ancienHash(String motDePasse) throws Exception {
        byte[] sel = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sel);
        byte[] hash = md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void champsVidesSontRefuses() {
        assertThrows(ValidationException.class, () -> auth.connecter("", "motdepasse1"));
        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", ""));
        assertThrows(ValidationException.class, () -> auth.connecter(null, null));
    }

    // ------------------------------------------------------------------ brute force and audit

    @Test
    void quatreEchecsPuisLeBonMotDePasseConnecteEtRemetLeCompteurAZero() {
        echouer("sara@translogix.ma", 4);

        assertEquals(sara.getId(), auth.connecter("sara@translogix.ma", "motdepasse1").getId());

        echouer("sara@translogix.ma", 4);
        assertFalse(auth.estBloque("sara@translogix.ma"), "le compteur repart de zéro après une réussite");
    }

    @Test
    void auCinquiemeEchecLEmailEstBloqueQuinzeMinutesMemeAvecLeBonMotDePasse() {
        echouer("sara@translogix.ma", 4);

        ValidationException cinquieme = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "faux"));
        assertEquals("Trop de tentatives de connexion. Réessayez dans 15 minutes.", cinquieme.getMessage());

        horloge.avancer(Duration.ofMinutes(14));
        ValidationException bloque = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "motdepasse1"));
        assertEquals("Trop de tentatives de connexion. Réessayez dans 1 minute.", bloque.getMessage());

        horloge.avancer(Duration.ofMinutes(1));
        assertEquals(sara.getId(), auth.connecter("sara@translogix.ma", "motdepasse1").getId());
    }

    @Test
    void leBlocageNeDependPasDeLaCasseNiDesEspaces() {
        echouer("SARA@translogix.ma ", 5);

        assertTrue(auth.estBloque("sara@translogix.ma"));
    }

    @Test
    void unEmailInconnuEstBloqueCommeUnVraiPourNeRienReveler() {
        echouer("inconnu@translogix.ma", 4);

        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("inconnu@translogix.ma", "x"));
        assertTrue(e.getMessage().startsWith("Trop de tentatives"), e.getMessage());
    }

    @Test
    void leBlocageDUnEmailNeGenePasLesAutres() {
        utilisateurs.creer("Idrissi", "Omar", "omar@translogix.ma", "motdepasse9", Role.RH);
        echouer("sara@translogix.ma", 5);

        assertEquals("omar@translogix.ma", auth.connecter("omar@translogix.ma", "motdepasse9").getEmail());
    }

    @Test
    void unCompteDesactiveCompteCommeUnEchec() {
        utilisateurs.changerStatut(sara.getId(), false);

        echouerAvec("sara@translogix.ma", "motdepasse1", 5);

        assertTrue(auth.estBloque("sara@translogix.ma"));
    }

    @Test
    void chaqueTentativeEstAuditeeSansJamaisLeMotDePasse() {
        int avant = audit.lister().size();
        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", "MauvaisSecret"));
        auth.connecter("sara@translogix.ma", "motdepasse1");

        List<EvenementAudit> connexions = audit.lister().subList(0, audit.lister().size() - avant);
        assertEquals(2, connexions.size());
        EvenementAudit reussie = connexions.get(0); // most recent first
        assertEquals(AuthService.MODULE_AUDIT, reussie.getModule());
        assertEquals(ResultatAudit.REUSSI, reussie.getResultat());
        assertEquals("Sara Alami (" + Role.DISPATCHER.getLibelle() + ")", reussie.getUtilisateur());
        EvenementAudit echec = connexions.get(1);
        assertEquals(ResultatAudit.REFUSE, echec.getResultat());
        assertEquals("sara@translogix.ma", echec.getUtilisateur());
        assertTrue(echec.getAction().contains("1/5"), echec.getAction());
        assertTrue(audit.lister().stream().noneMatch(e -> e.getAction().contains("MauvaisSecret")
                || e.getAction().contains("motdepasse1")));
    }

    @Test
    void leBlocageEstAuditeUneFoisPuisChaqueRefus() {
        echouer("sara@translogix.ma", 5);
        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", "motdepasse1"));

        List<String> actions = audit.filtrer(new AuditService.CritereAudit(null, AuthService.MODULE_AUDIT,
                ResultatAudit.REFUSE, null, null)).stream().map(EvenementAudit::getAction).toList();
        assertEquals(1, actions.stream().filter(a -> a.startsWith("Compte bloqué 15 minutes")).count());
        assertTrue(actions.get(0).contains("temporairement bloqué"), actions.get(0));
    }

    @Test
    void lesMinutesRestantesSontArrondiesAuDessus() {
        assertEquals("Trop de tentatives de connexion. Réessayez dans 15 minutes.",
                AuthService.messageBlocage(Duration.ofMinutes(14).plusSeconds(1)));
        assertEquals("Trop de tentatives de connexion. Réessayez dans 1 minute.",
                AuthService.messageBlocage(Duration.ofSeconds(5)));
    }

    private void echouer(String email, int fois) {
        echouerAvec(email, "faux", fois);
    }

    private void echouerAvec(String email, String motDePasse, int fois) {
        for (int i = 0; i < fois; i++) {
            assertThrows(ValidationException.class, () -> auth.connecter(email, motDePasse));
        }
    }

    /** A clock the test moves forward by hand. */
    private static final class HorlogeReglable extends Clock {
        private Instant maintenant;

        HorlogeReglable(Instant depart) {
            this.maintenant = depart;
        }

        void avancer(Duration duree) {
            maintenant = maintenant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }
    }
}
