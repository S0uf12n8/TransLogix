package ma.translogix.controller.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;

class NavigationMenuTest {

    @Test
    void leGerantAUnMenu() {
        assertFalse(NavigationMenu.pour(Role.GERANT).isEmpty());
    }

    @Test
    void lesEcransDuGerantExistent() {
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.GERANT)) {
            assertNotNull(getClass().getResource(NavigationMenu.RACINE + e.fxml()), e.fxml());
        }
    }
}
