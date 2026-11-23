package ma.translogix.controller.admin;

import java.util.List;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** "Copier…" entries of the admin right-click menus. */
final class PressePapiers {

    private PressePapiers() {
    }

    static void copier(String texte) {
        ClipboardContent contenu = new ClipboardContent();
        contenu.putString(texte == null ? "" : texte);
        Clipboard.getSystemClipboard().setContent(contenu);
    }

    /** One table row, cells separated by tabs: it pastes into separate Excel columns. */
    static void copierLigne(List<String> cellules) {
        copier(String.join("\t", cellules));
    }
}
