package de.stefan_oltmann.deals;

import java.util.ArrayList;
import java.util.List;

public class Seite {

    private String name;
    private String feedUrl;

    public Seite(String name, String feedUrl) {
        this.name = name;
        this.feedUrl = feedUrl;
    }

    public String getName() {
        return name;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public static List<Seite> createStartingSet() {

        List<Seite> seiten = new ArrayList<Seite>();

        seiten.add(new Seite("Mein-Deal.com", "http://www.mein-deal.com/feed/"));
        seiten.add(new Seite("OutdoorDeals.de", "http://www.outdoordeals.de/feed/"));
        seiten.add(new Seite("MyDealz.de", "http://feeds.feedburner.com/myDealZ?format=xml"));
        seiten.add(new Seite("SchnäppchenFuchs.com", "http://www.schnaeppchenfuchs.com/feed"));
        seiten.add(new Seite("Chillmo.com", "http://chillmo.com/feed/"));
        seiten.add(new Seite("PreisJäger.de", "http://www.preisjaeger.de/feed/"));
        seiten.add(new Seite("DealDoktor.de", "http://feeds.feedburner.com/dealdoktor?format=xml"));
        seiten.add(new Seite("SparBlog.com", "http://feeds.feedburner.com/Sparblog?format=xml"));
        seiten.add(new Seite("Reichweite.de", "http://www.reichweite.de/feed/"));
        seiten.add(new Seite("YourDealz.de", "http://www.yourdealz.de/feed/"));
        seiten.add(new Seite("Sparen-im-Netz.de", "http://www.sparen-im-netz.de/feed/?format=xml"));
        seiten.add(new Seite("Snipz.de", "http://snipz.de/feed"));
        seiten.add(new Seite("Juppp.de", "http://feeds2.feedburner.com/Juppp?format=xml"));
        seiten.add(new Seite("Schnappilette.de", "http://www.schnappilette.de/feed"));
        seiten.add(new Seite("Sparbote.de", "http://feeds.feedburner.com/sparbote-schnaeppchen?format=xml"));

        return seiten;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feedUrl == null) ? 0 : feedUrl.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Seite other = (Seite) obj;
        if (feedUrl == null) {
            if (other.feedUrl != null)
                return false;
        } else if (!feedUrl.equals(other.feedUrl))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Seite [name=" + name + ", feedUrl=" + feedUrl + "]";
    }

}
