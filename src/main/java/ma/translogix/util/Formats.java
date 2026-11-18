package ma.translogix.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Display formats shared by every screen, so all modules show money and dates the same way.
 * Money: "1 234 567,50 MAD" (plain space between thousands, comma, always 2 decimals, HALF_UP).
 * Dates: "05/11/2026".
 */
public final class Formats {
    /** dd/MM/yyyy, strict: 31/02/2026 is rejected. */
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formats() {
    }

    /** "1 234,50" ; "" for null. */
    public static String montant(BigDecimal valeur) {
        if (valeur == null) {
            return "";
        }
        DecimalFormatSymbols symboles = new DecimalFormatSymbols();
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        symboles.setMinusSign('-');
        DecimalFormat format = new DecimalFormat("#,##0.00", symboles); // new instance: DecimalFormat is not thread-safe
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(valeur);
    }

    /** "1 234,50 MAD" ; "" for null. */
    public static String mad(BigDecimal valeur) {
        return valeur == null ? "" : montant(valeur) + " MAD";
    }

    /** "05/11/2026" ; "" for null. */
    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /**
     * Parses a date typed by the user.
     * @throws ValidationException with a French message when the text is empty or not a real dd/MM/yyyy date
     */
    public static LocalDate lireDate(String texte) {
        if (texte == null || texte.isBlank()) {
            throw new ValidationException("La date est obligatoire (format jj/mm/aaaa).");
        }
        try {
            return LocalDate.parse(texte.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date invalide : « " + texte.trim() + " » (format jj/mm/aaaa).");
        }
    }
}
