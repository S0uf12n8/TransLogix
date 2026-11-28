package ma.translogix.controller.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The sidebar logo of shell.fxml must point to a real JPEG, resolved the way FXMLLoader resolves "@" URLs. */
class ShellLogoTest {

    private static final String SHELL = "/ma/translogix/fxml/common/shell.fxml";
    private static final String CHEMIN_RELATIF = "../../images/translogix-logo.jpeg";

    @Test
    void leShellReferenceLeLogo() throws IOException {
        URL shell = getClass().getResource(SHELL);
        assertNotNull(shell);
        try (InputStream in = shell.openStream()) {
            String fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(fxml.contains("@" + CHEMIN_RELATIF), "shell.fxml ne référence plus le logo");
        }
    }

    @Test
    void leLogoExisteEtEstUnJpeg() throws IOException, URISyntaxException {
        URL logo = getClass().getResource(SHELL).toURI().resolve(CHEMIN_RELATIF).toURL();
        try (InputStream in = logo.openStream()) {
            byte[] entete = in.readNBytes(3);
            assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, entete);
        }
    }
}
