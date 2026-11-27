package ma.translogix.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import ma.translogix.controller.common.NavigationMenu;
import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Structural check of the delivered comptable pages: an FXML can compile as XML yet still fail at runtime
 * when an fx:id has no field or an onAction points to a missing method. This test catches those mistakes
 * without needing a graphical display.
 */
class ComptableFxmlIntegrationTest {

    private static final List<String> PAGES = List.of(
            "comptable/tresorerie.fxml",
            "comptable/paie.fxml",
            "comptable/paie-dialog.fxml",
            "comptable/paie-paiement-dialog.fxml",
            "comptable/journal.fxml",
            "comptable/recouvrement.fxml");
    /** Pages of this task: every fx:id must have a matching controller field. */
    private static final List<String> PAGES_STRICTES = List.of(
            "comptable/tresorerie.fxml",
            "comptable/paie.fxml",
            "comptable/paie-dialog.fxml",
            "comptable/paie-paiement-dialog.fxml",
            "comptable/journal.fxml");

    @Test
    void leMenuComptableOuvreLesPagesLivrees() {
        List<String> entrees = NavigationMenu.pour(Role.COMPTABLE).stream()
                .map(NavigationMenu.Entree::fxml).toList();
        for (String page : List.of("comptable/recouvrement.fxml", "comptable/tresorerie.fxml",
                "comptable/paie.fxml", "comptable/journal.fxml")) {
            assertTrue(entrees.contains(page), page + " absent du menu comptable");
            assertNotNull(ComptableFxmlIntegrationTest.class.getResource("/ma/translogix/fxml/" + page), page);
        }
    }

    @Test
    void toutesLesPagesComptablesSontCoherentes() {
        List<String> erreurs = new ArrayList<>();
        for (String page : PAGES) {
            verifier(page, PAGES_STRICTES.contains(page), erreurs);
        }
        assertTrue(erreurs.isEmpty(), String.join(System.lineSeparator(), erreurs));
    }

    private static void verifier(String page, boolean strict, List<String> erreurs) {
        String chemin = "/ma/translogix/fxml/" + page;
        try (InputStream flux = ComptableFxmlIntegrationTest.class.getResourceAsStream(chemin)) {
            if (flux == null) {
                erreurs.add(page + " : FXML introuvable");
                return;
            }
            byte[] octets = flux.readAllBytes();
            if (octets.length >= 3 && (octets[0] & 0xFF) == 0xEF
                    && (octets[1] & 0xFF) == 0xBB && (octets[2] & 0xFF) == 0xBF) {
                erreurs.add(page + " : BOM UTF-8 interdit (FXMLLoader échoue sur Java 21)");
            }
            var usine = DocumentBuilderFactory.newInstance();
            usine.setNamespaceAware(false); // fx:id and fx:controller are read as literal attribute names
            Document document = usine.newDocumentBuilder().parse(new ByteArrayInputStream(octets));
            verifierImports(document, page, erreurs);

            Element racine = document.getDocumentElement();
            String nomControleur = racine.getAttribute("fx:controller");
            if (nomControleur.isBlank()) {
                erreurs.add(page + " : aucun fx:controller");
                return;
            }
            Class<?> controleur = Class.forName(nomControleur, false,
                    ComptableFxmlIntegrationTest.class.getClassLoader());
            Set<String> identifiants = new HashSet<>();
            verifierNoeud(racine, page, strict, controleur, identifiants, erreurs);
        } catch (Exception e) {
            erreurs.add(page + " : " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
    }

    private static void verifierImports(Document document, String page, List<String> erreurs) {
        NodeList enfants = document.getChildNodes();
        for (int i = 0; i < enfants.getLength(); i++) {
            Node noeud = enfants.item(i);
            if (noeud.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE
                    && "import".equals(noeud.getNodeName())) {
                String classe = noeud.getNodeValue().trim();
                try {
                    Class.forName(classe, false, ComptableFxmlIntegrationTest.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    erreurs.add(page + " : import inconnu " + classe);
                }
            }
        }
    }

    private static void verifierNoeud(Element element, String page, boolean strict, Class<?> controleur,
                                      Set<String> identifiants, List<String> erreurs) {
        if (element.hasAttribute("fx:id")) {
            String id = element.getAttribute("fx:id");
            if (!identifiants.add(id)) {
                erreurs.add(page + " : fx:id dupliqué " + id);
            }
            if (strict) {
                try {
                    controleur.getDeclaredField(id);
                } catch (NoSuchFieldException e) {
                    erreurs.add(page + " : champ @" + id + " absent de " + controleur.getSimpleName());
                }
            }
        }
        NamedNodeMap attributs = element.getAttributes();
        for (int i = 0; i < attributs.getLength(); i++) {
            Node attribut = attributs.item(i);
            String nom = attribut.getNodeName();
            String valeur = attribut.getNodeValue();
            if (nom.startsWith("on") && valeur.startsWith("#")) {
                String methode = valeur.substring(1);
                try {
                    controleur.getDeclaredMethod(methode);
                } catch (NoSuchMethodException e) {
                    erreurs.add(page + " : méthode " + methode + "() absente de " + controleur.getSimpleName());
                }
            }
        }
        NodeList enfants = element.getChildNodes();
        for (int i = 0; i < enfants.getLength(); i++) {
            Node enfant = enfants.item(i);
            if (enfant.getNodeType() == Node.ELEMENT_NODE) {
                verifierNoeud((Element) enfant, page, strict, controleur, identifiants, erreurs);
            }
        }
    }
}
