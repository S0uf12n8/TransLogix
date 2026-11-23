package ma.translogix.controller.admin;

import java.time.LocalDate;
import java.util.function.Consumer;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/** Date fields of the admin filter bars: dd/MM/yyyy, empty = no date, a bad date never throws. */
final class ChampDate {

    private ChampDate() {
    }

    /**
     * Shows and reads dates as dd/MM/yyyy. A badly typed date keeps the previous value and passes the
     * French message to erreur, instead of throwing inside JavaFX.
     */
    static void configurer(DatePicker champ, Consumer<String> erreur) {
        champ.setPromptText("jj/mm/aaaa");
        champ.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String texte) {
                if (texte == null || texte.isBlank()) {
                    return null;
                }
                try {
                    return Formats.lireDate(texte);
                } catch (ValidationException ex) {
                    erreur.accept(ex.getMessage());
                    return champ.getValue();
                }
            }
        });
    }
}
