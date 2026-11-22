package ma.translogix.controller.common;

/**
 * A page controller that knows its own printed version. The top-bar "Imprimer" button uses it; pages
 * without it print their first table (RapportDepuisTable). Implement it when the page is not a plain table
 * (a dashboard) or when its table holds composite cells (missions, planning).
 */
public interface Imprimable {

    /** The document to print, built from the page's current data and filters. */
    ImpressionRapport.Document documentAImprimer();
}
