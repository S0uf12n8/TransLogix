package ma.translogix.controller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.translogix.controller.common.MiseEnPageRapport.Tranche;
import org.junit.jupiter.api.Test;

class MiseEnPageRapportTest {

    @Test
    void largeursProportionnellesAuTexteLePlusLongEtRemplissentLaPage() {
        double[] l = MiseEnPageRapport.largeurs(List.of("N°", "Client"),
                List.of(List.of("FACT-2026-001", "LogiMaroc SARL"), List.of("FACT-2026-002", "Atlas")), 540);

        assertEquals(540, l[0] + l[1], 1e-9);
        assertEquals(540 * 13.0 / 27, l[0], 1e-9); // "FACT-2026-001" = 13, "LogiMaroc SARL" = 14
    }

    @Test
    void colonneCourteOuTresLongueEstBornee() {
        String long80 = "x".repeat(80);
        double[] l = MiseEnPageRapport.largeurs(List.of("A", "Action"), List.of(List.of("1", long80)), 420);

        assertEquals(420.0 * 4 / 42, l[0], 1e-9);  // at least 4 characters
        assertEquals(420.0 * 38 / 42, l[1], 1e-9); // at most 38 characters
    }

    @Test
    void premierePageMoinsRemplieQueLesSuivantes() {
        assertEquals(List.of(new Tranche(0, 20), new Tranche(20, 50), new Tranche(50, 55)),
                MiseEnPageRapport.pages(55, 20, 30));
        assertEquals(List.of(new Tranche(0, 20)), MiseEnPageRapport.pages(20, 20, 30));
    }

    @Test
    void rapportVideImprimeQuandMemeUnePage() {
        List<Tranche> pages = MiseEnPageRapport.pages(0, 20, 30);

        assertEquals(1, pages.size());
        assertEquals(0, pages.get(0).taille());
    }

    @Test
    void pageTropPetiteEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> MiseEnPageRapport.pages(5, 0, 30));
    }

    @Test
    void lignesQuiTiennentEtOrientation() {
        assertEquals(10, MiseEnPageRapport.lignesQuiTiennent(185, 18));
        assertEquals(1, MiseEnPageRapport.lignesQuiTiennent(5, 18));
        assertFalse(MiseEnPageRapport.paysage(6));
        assertTrue(MiseEnPageRapport.paysage(7));
    }
}
