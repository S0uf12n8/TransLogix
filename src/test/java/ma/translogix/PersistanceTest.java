package ma.translogix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.util.Optional;
import ma.translogix.dao.jdbc.ConnexionBase;
import ma.translogix.dao.jdbc.JdbcAuditDao;
import ma.translogix.dao.jdbc.JdbcUtilisateurDao;
import ma.translogix.service.AuditService;
import ma.translogix.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class PersistanceTest {

    @Test
    void sansDbUrlLesDonneesRestentEnMemoire() {
        Persistance.Etat etat = Persistance.choisir(Optional.empty());

        assertEquals(Persistance.Mode.MEMOIRE, etat.mode());
        assertTrue(etat.connexion().isEmpty());
        assertFalse(etat.enregistre());
        assertNull(etat.probleme());
    }

    @Test
    void baseInjoignableDemarreEnMemoireDeSecoursAvecLeMessageFrancais() {
        ConnexionBase injoignable = new ConnexionBase("jdbc:mysql://127.0.0.1:1/translogix_db?connectTimeout=2000",
                "personne", "rien");

        Persistance.Etat etat = Persistance.choisir(Optional.of(injoignable));

        assertEquals(Persistance.Mode.MEMOIRE_SECOURS, etat.mode());
        assertTrue(etat.connexion().isEmpty(), "aucun DAO JDBC ne doit être créé sur une base injoignable");
        assertFalse(etat.enregistre());
        assertTrue(etat.probleme() != null && !etat.probleme().isBlank());
        assertFalse(etat.probleme().contains("jdbc:"), "le message utilisateur ne montre pas l'URL : " + etat.probleme());
    }

    @Test
    void leModeSecoursDitPourquoiEtQueRienNEstEnregistre() {
        Persistance.Etat etat = new Persistance.Etat(Persistance.Mode.MEMOIRE_SECOURS, null,
                "Impossible de joindre la base de données.");

        assertEquals("MySQL indisponible · non enregistré", etat.libelle());
        assertTrue(etat.detail().startsWith("Impossible de joindre la base de données."), etat.detail());
        assertTrue(etat.detail().contains("ne seront pas enregistrées"), etat.detail());
    }

    @Test
    void leModeMySqlNommeCeQuiEstVraimentEnBase() {
        Persistance.Etat etat = new Persistance.Etat(Persistance.Mode.MYSQL, null, null);

        assertEquals("Base MySQL connectée", etat.libelle());
        assertTrue(etat.detail().contains("comptes utilisateurs, journal d'audit"), etat.detail());
        assertTrue(etat.detail().contains("restent en mémoire"), "ne pas laisser croire que tout est enregistré");
    }

    @Test
    void leModeMemoireDitQueToutRepartAuDemarrage() {
        Persistance.Etat etat = Persistance.choisir(Optional.empty());

        assertEquals("Mode démonstration · non enregistré", etat.libelle());
        assertTrue(etat.detail().contains("DB_URL"), etat.detail());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
    void avecMySqlLeSchemaEstAppliqueEtLesComptesDemoNeSontCreesQuUneFois() throws Exception {
        Persistance.Etat etat = Persistance.choisir(ConnexionBase.depuisEnvironnement());
        assertEquals(Persistance.Mode.MYSQL, etat.mode());
        assertTrue(etat.enregistre());
        ConnexionBase base = etat.connexion().orElseThrow();
        try (Connection c = base.ouvrir(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM audit_evenements");
            st.execute("DELETE FROM utilisateurs");
        }

        // First start, then a "restart": new DAO and service objects on the same database.
        assertTrue(demarrer(base));
        assertFalse(demarrer(base), "le second démarrage ne recrée pas les comptes (pas de doublon d'email)");

        JdbcUtilisateurDao dao = new JdbcUtilisateurDao(base);
        assertEquals(5, dao.findAll().size());
        assertTrue(dao.findByEmail(ComptesDemo.EMAIL_CHAUFFEUR).isPresent());
        assertEquals(5, new JdbcAuditDao(base).findAll().size(), "une seule trace de création par compte");
    }

    private static boolean demarrer(ConnexionBase base) {
        JdbcUtilisateurDao dao = new JdbcUtilisateurDao(base);
        AuditService audit = new AuditService(new JdbcAuditDao(base), Clock.systemDefaultZone(), () -> "Système");
        return ComptesDemo.semer(dao, new UtilisateurService(dao, audit));
    }
}
