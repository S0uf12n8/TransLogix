package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FormatsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0          | 0,00",
        "5          | 5,00",
        "999.9      | 999,90",
        "1000       | 1 000,00",
        "452000     | 452 000,00",
        "1234567.5  | 1 234 567,50",
        "-89400     | -89 400,00",
        "46.665     | 46,67",   // HALF_UP
        "46.664     | 46,66"
    })
    void montantAvecEspacesEtDeuxDecimales(String valeur, String attendu) {
        assertEquals(attendu, Formats.montant(new BigDecimal(valeur)));
    }

    @Test
    void madAjouteLUnite() {
        assertEquals("1 140,00 MAD", Formats.mad(new BigDecimal("1140")));
    }

    @Test
    void nullDonneUneChaineVide() {
        assertEquals("", Formats.montant(null));
        assertEquals("", Formats.mad(null));
        assertEquals("", Formats.date(null));
    }

    @Test
    void dateAuFormatJourMoisAnnee() {
        assertEquals("05/11/2026", Formats.date(LocalDate.of(2026, 11, 5)));
    }

    @Test
    void lireDateAccepteLeFormatAffiche() {
        assertEquals(LocalDate.of(2026, 11, 5), Formats.lireDate(" 05/11/2026 "));
        LocalDate d = LocalDate.of(2027, 2, 28);
        assertEquals(d, Formats.lireDate(Formats.date(d)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "2026-11-05", "5/11/26", "31/02/2026", "32/01/2026", "abc"})
    void lireDateRefuseLesDatesInvalides(String texte) {
        assertThrows(ValidationException.class, () -> Formats.lireDate(texte));
    }

    @Test
    void lireDateRefuseNull() {
        assertThrows(ValidationException.class, () -> Formats.lireDate(null));
    }
}
