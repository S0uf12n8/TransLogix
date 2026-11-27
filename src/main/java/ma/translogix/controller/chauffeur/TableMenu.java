package ma.translogix.controller.chauffeur;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;

/**
 * Menu contextuel des tableaux de l'espace chauffeur, sur le modèle de
 * {@code controller/admin/UtilisateursController} (lu, non modifié). Les entrées sont reconstruites à
 * l'ouverture du menu : elles suivent donc toujours l'état réel de la ligne.
 */
public final class TableMenu {
    private TableMenu() {}

    /**
     * @param items      entrées possibles pour la ligne affichée (menu vide si aucune ligne)
     * @param doubleClic action du double-clic, ou null si la ligne n'a rien à ouvrir
     */
    public static <R> void installer(TableView<R> table,
                                     Function<R, List<MenuItem>> items,
                                     Consumer<R> doubleClic) {
        table.setRowFactory(t -> {
            TableRow<R> ligne = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            menu.setOnShowing(e -> {
                R valeur = ligne.getItem();
                menu.getItems().setAll(valeur == null ? List.<MenuItem>of() : items.apply(valeur));
            });
            ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
            if (doubleClic != null) {
                ligne.setOnMouseClicked(e -> {
                    R valeur = ligne.getItem();
                    if (valeur != null && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                        doubleClic.accept(valeur);
                    }
                });
            }
            return ligne;
        });
    }

    /** Entrée « Copier … » : presse-papiers local et retour visuel. */
    public static MenuItem copier(String libelle, String valeur) {
        MenuItem entree = new MenuItem(libelle);
        entree.setDisable(valeur == null || valeur.isBlank() || "—".equals(valeur));
        entree.setOnAction(e -> {
            ClipboardContent contenu = new ClipboardContent();
            contenu.putString(valeur);
            Clipboard.getSystemClipboard().setContent(contenu);
            EspaceChauffeurUi.get().toast("Copié dans le presse-papiers : " + valeur);
        });
        return entree;
    }
}
