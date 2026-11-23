package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import ma.translogix.dao.DonneesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SchemaBaseTest {

    @Test
    void decoupeSurLePointVirguleDeFinDeLigneEtIgnoreLesCommentaires() {
        String script = """
                -- titre; avec un point-virgule dans le commentaire
                CREATE TABLE a (
                    x INT
                );
                -- entre les deux
                CREATE TABLE b (y VARCHAR(10) DEFAULT 'a;b');
                """;

        List<String> instructions = SchemaBase.decouper(script);

        assertEquals(2, instructions.size());
        assertTrue(instructions.get(0).startsWith("CREATE TABLE a ("));
        assertTrue(instructions.get(1).contains("'a;b'"), instructions.get(1));
    }

    @Test
    void scriptVideNeDonneAucuneInstruction() {
        assertTrue(SchemaBase.decouper("-- rien\n\n").isEmpty());
    }

    @Test
    void baseInjoignableDonneUneErreurDeConnexionEnFrancais() {
        ConnexionBase injoignable = new ConnexionBase("jdbc:mysql://127.0.0.1:1/translogix_db?connectTimeout=2000",
                "personne", "rien");

        DonneesException e = assertThrows(DonneesException.class, () -> SchemaBase.appliquer(injoignable));

        assertEquals(DonneesException.Type.CONNEXION, e.getType());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
    void schemaRejouableSurUneBaseQuiALesTablesEtLesDonnees() throws Exception {
        ConnexionBase base = BaseDeTest.prete();
        BaseDeTest.executer("INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role) "
                + "VALUES ('Test', 'Rejeu', 'rejeu@test.ma', 'x', 'RH')");

        SchemaBase.appliquer(base);
        SchemaBase.appliquer(base);

        try (Connection c = base.ouvrir(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utilisateurs WHERE email = 'rejeu@test.ma'")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "le schéma ne doit jamais effacer les données existantes");
        }
    }
}
