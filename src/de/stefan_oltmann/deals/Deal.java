package de.stefan_oltmann.deals;

import java.util.Comparator;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class Deal {

    private Seite seite;
    private String titel;
    private String link;
    private Long pubTime;
    private Long dayOfYear;

    private Key datastoreKey;

    public static final Comparator<Deal> PUBTIME_COMPARATOR =
            new Comparator<Deal>() {

                @Override
                public int compare(Deal o1, Deal o2) {
                    return -o1.getPubTime().compareTo(o2.getPubTime());
                }
            };

    public Deal(Seite seite, String titel, String link, Long pubTime, Long dayOfYear) {
        this.seite = seite;
        this.titel = titel;
        this.link = link;
        this.pubTime = pubTime;
        this.dayOfYear = dayOfYear;
    }

    public Key getDatastoreKey() {

        if (datastoreKey == null)
            datastoreKey = KeyFactory.createKey("Deal", seite.getName() + " @ " + pubTime);

        return datastoreKey;
    }

    public Seite getSeite() {
        return seite;
    }

    public String getTitel() {
        return titel;
    }

    public String getLink() {
        return link;
    }

    public Long getPubTime() {
        return pubTime;
    }

    public Long getDayOfYear() {
        return dayOfYear;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        result = prime * result + ((pubTime == null) ? 0 : pubTime.hashCode());
        result = prime * result + ((seite == null) ? 0 : seite.hashCode());
        result = prime * result + ((titel == null) ? 0 : titel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Deal other = (Deal) obj;
        if (link == null) {
            if (other.link != null)
                return false;
        } else if (!link.equals(other.link))
            return false;
        if (pubTime == null) {
            if (other.pubTime != null)
                return false;
        } else if (!pubTime.equals(other.pubTime))
            return false;
        if (seite == null) {
            if (other.seite != null)
                return false;
        } else if (!seite.equals(other.seite))
            return false;
        if (titel == null) {
            if (other.titel != null)
                return false;
        } else if (!titel.equals(other.titel))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Deal [seite=" + seite + ", titel=" + titel + ", link=" + link + ", pubTime=" + pubTime + "]";
    }

    /*
     * Filter den Titel um unnütze Tags
     */
    public static String getFilteredTitle(String title) {

        title = title.replaceAll(" – Update", "");
        title = title.replaceAll("Wieder da!", "");
        title = title.replaceAll("Wieder da:", "");
        title = title.replaceAll("– wieder da!", "");
        title = title.replaceAll("Update!", "");
        title = title.replaceAll("WOW!", "");
        title = title.replaceAll("\\[AMAZON\\]", "");
        title = title.replaceAll("\\[AMAZON UK\\]", "");
        title = title.replaceAll("\\(Amazon\\)", "");
        title = title.replaceAll("\\[EBAY\\]", "");
        title = title.replaceAll("\\[Müller\\]", "");
        title = title.replaceAll(" für nur ", " für ");
        title = title.replaceAll("\\*UPDATE.*\\*", "");
        title = title.replaceAll("\\*UP.*\\*", "");
        title = title.replaceAll("\\*Knaller\\*", "");
        title = title.replaceAll("\\*Kracher\\*", "");
        title = title.replaceAll("\\*Mega Kracher\\*", "");
        title = title.replaceAll("\\*Genial\\*", "");
        title = title.replaceAll("\\*Tipp\\*", "");
        title = title.replaceAll("\\*Top\\*", "");
        title = title.replaceAll("\\*TOP\\*", "");
        title = title.replaceAll("\\*Schnell\\*", "");
        title = title.replaceAll("\\*Krass\\*", "");
        title = title.replaceAll("\\*FETT\\*", "");
        title = title.replaceAll("\\*Endspurt\\*", "");
        title = title.replaceAll("\\*Update\\*", "");
        title = title.replaceAll("KRACHER!*", "");
        title = title.replaceAll("Knaller!*", "");
        title = title.replaceAll("Tipp!", "");
        title = title.replaceAll("Top!", "");
        title = title.replaceAll("Top:", "");
        title = title.replaceAll("TOP:", "");
        title = title.replaceAll("Neu!", "");
        title = title.replaceAll("Schnell!", "");
        title = title.replaceAll("Blitzangebote!", "");
        title = title.replaceAll("Nur heute!", "");
        title = title.replaceAll("Nur heute:", "");
        title = title.replaceAll("Nur Heute:", "");
        title = title.replaceAll("– nur für kurze Zeit", "");
        title = title.replaceAll("Nur noch heute!", "");
        title = title.replaceAll("Nur noch heute:", "");
        title = title.replaceAll("Nur heute – ", "");
        title = title.replaceAll("Endlich wieder da:", "");
        title = title.replaceAll("Nur bis morgen!", "");
        title = title.replaceAll("Auch heute wieder!", "");
        title = title.replaceAll("eBay Angebot:", "");
        title = title.replaceAll("Satte", "");
        title = title.replaceAll("satte", "");
        title = title.replaceAll("günstige", "");
        title = title.replaceAll("Geht noch!", "");
        title = title.replaceAll("Bestpreis!", "");
        title = title.replaceAll("LETZTER TAG: ", "");
        title = title.trim();

        return title;
    }

}
