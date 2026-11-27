package ma.translogix.controller.chauffeur;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.ScrollEvent;

/** Aide à la construction des colonnes de TableView (la ligne entière est la valeur de chaque cellule). */
public final class TableUtil {
    private TableUtil() {}

    public static final double HEADER_HEIGHT = 42;
    public static final double ROW_HEIGHT = 54;

    @SuppressWarnings("unchecked")
    private static <R> TableColumn<R, R> cast(TableColumn<R, ?> c) { return (TableColumn<R, R>) c; }

    /** Colonne de texte simple. */
    public static <R> void text(TableColumn<R, ?> col, Function<R, String> f, String... styleClasses) {
        TableColumn<R, R> c = cast(col);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        c.setCellFactory(x -> new TableCell<R, R>() {
            @Override protected void updateItem(R item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(styleClasses);
                if (empty || item == null) { setText(null); return; }
                getStyleClass().addAll(styleClasses);
                setWrapText(true);
                setText(f.apply(item));
            }
        });
    }

    /** Colonne à contenu graphique (pastilles, boutons...). */
    public static <R> void node(TableColumn<R, ?> col, Function<R, Node> f) {
        TableColumn<R, R> c = cast(col);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        c.setCellFactory(x -> new TableCell<R, R>() {
            @Override protected void updateItem(R item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : f.apply(item));
            }
        });
    }

    /**
     * Ajuste la hauteur du tableau à son contenu (comme un tableau HTML) et laisse la molette
     * défiler la page au lieu d'être absorbée par le tableau.
     * @param emptyHeight hauteur minimale lorsque la liste est vide (ligne « Aucun ... » de l'original)
     */
    public static <R> void fit(TableView<R> t, ObservableList<R> items, double emptyHeight) {
        t.setFixedCellSize(ROW_HEIGHT);
        // les largeurs déclarées dans le FXML servent de proportions : les colonnes occupent toute la largeur
        double sum = t.getColumns().stream().mapToDouble(TableColumn::getPrefWidth).sum();
        for (TableColumn<R, ?> c : t.getColumns()) {
            double share = c.getPrefWidth() / sum;
            c.setMinWidth(40);
            c.prefWidthProperty().bind(t.widthProperty().subtract(2).multiply(share));
        }
        t.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(HEADER_HEIGHT + items.size() * ROW_HEIGHT + 1, items.isEmpty() ? HEADER_HEIGHT + emptyHeight : 0),
                items));
        t.minHeightProperty().bind(t.prefHeightProperty());
        t.maxHeightProperty().bind(t.prefHeightProperty());
        t.addEventFilter(ScrollEvent.ANY, e -> {
            Node p = t.getParent();
            if (p != null && !e.isConsumed()) {
                e.consume();
                Event.fireEvent(p, e.copyFor(p, p));
            }
        });
    }
}
