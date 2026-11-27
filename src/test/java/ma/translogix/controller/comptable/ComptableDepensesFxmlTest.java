package ma.translogix.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import ma.translogix.controller.common.NavigationMenu;
import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Structural checks: the page opens from the menu and every fx:id / handler exists in its controller. */
class ComptableDepensesFxmlTest {

    private record Attendu(String fxml, Class<?> controller, List<String> ids, List<String> actions,
                           List<String> imports) {
    }

    private static final List<Attendu> ECRANS = List.of(
            new Attendu("comptable/depenses.fxml", DepensesController.class,
                    List.of("kpiEnAttente", "kpiMontantAttente", "kpiValidees", "kpiRefusees", "boutonNouveau",
                            "boutonActualiser", "recherche", "filtreStatut", "filtreType", "filtreChauffeur",
                            "filtreDu", "filtreAu", "boutonReset", "table", "colMission", "colChauffeur",
                            "colType", "colStation", "colMontant", "colDate", "colStatut", "colActions"),
                    List.of("nouveau", "actualiser", "reinitialiser"),
                    List.of("ComboBox", "DatePicker", "TableView", "TextField", "Button")),
            new Attendu("comptable/depense-dialog.fxml", DepenseFormulaireController.class,
                    List.of("titre", "apercuMission", "champMission", "champChauffeur", "champType", "champStation",
                            "champMontant", "champLitres", "champDate"),
                    List.of(), List.of("ComboBox", "DatePicker", "TextField", "DialogPane")),
            new Attendu("comptable/depense-refus-dialog.fxml", DepenseRefusController.class,
                    List.of("titre", "resume", "champMotif"),
                    List.of(), List.of("TextArea", "DialogPane")));

    @Test
    void leMenuComptableOuvreLaPageValiderDepenses() {
        List<String> entrees = NavigationMenu.pour(Role.COMPTABLE).stream()
                .map(NavigationMenu.Entree::fxml).toList();
        assertTrue(entrees.contains("comptable/depenses.fxml"), "Valider dépenses absent du menu comptable");
        assertNotNull(getClass().getResource("/ma/translogix/fxml/comptable/depenses.fxml"));
    }

    @Test
    void lesFxmlSontCoherentsAvecLeursControllers() throws Exception {
        var usine = DocumentBuilderFactory.newInstance();
        usine.setNamespaceAware(false); // fx:id and fx:controller are read as literal attribute names
        for (Attendu ecran : ECRANS) {
            String chemin = "/ma/translogix/fxml/" + ecran.fxml();
            try (InputStream flux = getClass().getResourceAsStream(chemin)) {
                assertNotNull(flux, chemin);
                byte[] octets = flux.readAllBytes();
                assertTrue(!(octets.length >= 3 && (octets[0] & 0xFF) == 0xEF
                        && (octets[1] & 0xFF) == 0xBB && (octets[2] & 0xFF) == 0xBF),
                        ecran.fxml() + " : BOM UTF-8 interdit (FXMLLoader échoue sur Java 21)");
                Document document = usine.newDocumentBuilder().parse(new ByteArrayInputStream(octets));
                Element racine = document.getDocumentElement();
                assertTrue(ecran.controller().getName().equals(racine.getAttribute("fx:controller")),
                        ecran.fxml() + " : mauvais contrôleur");

                List<String> presents = new ArrayList<>();
                NodeList noeuds = document.getElementsByTagName("*");
                for (int i = 0; i < noeuds.getLength(); i++) {
                    Element noeud = (Element) noeuds.item(i);
                    if (noeud.hasAttribute("fx:id")) {
                        presents.add(noeud.getAttribute("fx:id"));
                    }
                }
                for (String id : ecran.ids()) {
                    assertTrue(presents.contains(id), ecran.fxml() + " : fx:id manquant " + id);
                    assertNotNull(ecran.controller().getDeclaredField(id),
                            ecran.fxml() + " : champ Java manquant " + id);
                }
                for (String action : ecran.actions()) {
                    assertNotNull(ecran.controller().getDeclaredMethod(action),
                            ecran.fxml() + " : handler manquant #" + action);
                }
                String source = new String(octets, java.nio.charset.StandardCharsets.UTF_8);
                for (String importation : ecran.imports()) {
                    assertTrue(source.contains("import javafx.scene.control." + importation + "?"),
                            ecran.fxml() + " : import manquant " + importation);
                }
            }
        }
    }
}
