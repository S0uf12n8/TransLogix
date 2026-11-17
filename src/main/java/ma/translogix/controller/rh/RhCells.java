package ma.translogix.controller.rh;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/** Reusable {@link TableCell} factories so the RH tables share the same look. */
public final class RhCells {

    private RhCells() {
    }

    /** Renders the value as a coloured status pill. {@code ton} returns a css suffix (green, amber, ...). */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> statut(Function<T, String> ton) {
        return colonne -> new TableCell<>() {
            @Override
            protected void updateItem(String valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide || valeur == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pastille = new Label(valeur);
                pastille.getStyleClass().addAll("rh-status", ton.apply(ligne()));
                setGraphic(pastille);
                setText(null);
            }

            private T ligne() {
                return getTableView().getItems().get(getIndex());
            }
        };
    }

    /** Colours the cell text with a css class returned by {@code ton} (rh-positive, rh-warning, ...). */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> texteColore(Function<T, String> ton) {
        return colonne -> new TableCell<>() {
            @Override
            protected void updateItem(String valeur, boolean vide) {
                super.updateItem(valeur, vide);
                getStyleClass().removeAll("rh-positive", "rh-negative", "rh-warning", "rh-accent", "rh-strong");
                setText(vide ? null : valeur);
                if (!vide && valeur != null) {
                    getStyleClass().add(ton.apply(getTableView().getItems().get(getIndex())));
                }
            }
        };
    }

    /** Renders a bold first line and a muted second line inside the same cell. */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> doubleLigne(
            Function<T, String> principale, Function<T, String> secondaire) {
        return colonne -> new TableCell<>() {
            private final Label titre = new Label();
            private final Label sousTitre = new Label();
            private final VBox boite = new VBox(2, titre, sousTitre);

            {
                titre.getStyleClass().add("rh-cell-primary");
                sousTitre.getStyleClass().add("rh-cell-secondary");
            }

            @Override
            protected void updateItem(String valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide) {
                    setGraphic(null);
                    return;
                }
                T ligne = getTableView().getItems().get(getIndex());
                titre.setText(principale.apply(ligne));
                sousTitre.setText(secondaire.apply(ligne));
                setGraphic(boite);
                setText(null);
            }
        };
    }

    /** Renders a button that acts on the row it belongs to. */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> bouton(
            String libelle, String styleClass, Consumer<T> action) {
        return colonne -> new TableCell<>() {
            private final Button bouton = new Button(libelle);

            {
                bouton.getStyleClass().add(styleClass);
            }

            @Override
            protected void updateItem(String valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide) {
                    setGraphic(null);
                    return;
                }
                T ligne = getTableView().getItems().get(getIndex());
                bouton.setOnAction(e -> action.accept(ligne));
                setGraphic(bouton);
                setText(null);
            }
        };
    }
}
