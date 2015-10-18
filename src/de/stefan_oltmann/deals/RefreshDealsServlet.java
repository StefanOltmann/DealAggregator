package de.stefan_oltmann.deals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

@SuppressWarnings("serial")
public class RefreshDealsServlet extends HttpServlet {

    private static final TimeZone GERMAN_TIMEZONE = TimeZone.getTimeZone("Europe/Berlin");

    public static final int MAX_AGE_IN_DAYS = 30;

    private static final Logger LOGGER = Logger.getLogger("RefreshDealsServlet");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Date maxAgeDate = calcMaxAgeDate();

        if (req.getParameter("init") != null) {

            initSeiten(datastore);

            resp.setContentType("text/plain");
            resp.getWriter().println("Initialized.");
            return;
        }

        if (req.getParameter("cleanup") != null) {

            int deleteCount = deleteOldDeals(datastore, maxAgeDate);

            String logMessage = "Cleanup: Deleted " + deleteCount + " deals older than " + maxAgeDate + " (millis: " + maxAgeDate.getTime() + ")";

            LOGGER.info(logMessage);
            resp.setContentType("text/plain");
            resp.getWriter().println(logMessage);
            return;
        }

        long startTime = System.currentTimeMillis();

        Set<Seite> seiten = SeitenCache.getInstance().readFromCache(datastore);

        Set<Deal> deals = findDealsFromFeeds(seiten, maxAgeDate);

        deals = filterNeueDeals(deals, datastore);

        for (Abo abo : findAbos(datastore)) {

            List<Deal> matchingDeals = new ArrayList<Deal>();

            for (String tag : abo.getTags())
                for (Deal deal : deals)
                    if (deal.getTitel().toLowerCase().contains(tag.toLowerCase()))
                        matchingDeals.add(deal);

            if (!matchingDeals.isEmpty()) {

                StringBuilder sb = new StringBuilder();
                sb.append("Folgende Deals könnten Sie interessieren:<ul>\n");
                for (Deal deal : matchingDeals)
                    sb.append("<li><a href=" + deal.getLink() + ">" + deal.getTitel() + "</a></li>");
                sb.append("</ul>\n");

                sb.append("\nZur Beendigung des Abos antworten Sie auf diese Mail mit dem Betreff 'unsubscribe'.");

                try {

                    MailHandlerServlet.sendMail(new InternetAddress(abo.getEmail()), "Interessante Deals", sb.toString(), true);

                } catch (MessagingException me) {
                    LOGGER.log(Level.SEVERE, "Mail an " + abo.getEmail() + " konnte nicht gesendet werden.", me);
                }
            }
        }

        storeDeals(datastore, deals);

        long endTime = System.currentTimeMillis();

        String logMessage = "Refresh for " + seiten.size() + " sites added " + deals.size() + " deals and finished in " + (endTime - startTime) + "ms.";

        LOGGER.info(logMessage);
        resp.setContentType("text/plain");
        resp.getWriter().println(logMessage);
    }

    /**
     * Befüllt die leere Datenbank mit einigen vor-konfigurierten Seiten
     */
    private void initSeiten(DatastoreService datastore) {

        List<Seite> seiten = Seite.createStartingSet();

        for (Seite seite : seiten) {

            Key seiteKey = KeyFactory.createKey("Seite", seite.getName());

            Entity seiteEntity = new Entity(seiteKey);

            seiteEntity.setProperty("siteName", seite.getName());
            seiteEntity.setProperty("feedUrl", seite.getFeedUrl());

            datastore.put(seiteEntity);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Abo> findAbos(DatastoreService datastore) {

        Query query = new Query("Abo");

        List<Entity> aboEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        List<Abo> seiten = new ArrayList<Abo>();

        for (Entity entity : aboEntities)
            seiten.add(new Abo((String) entity.getProperty("mail"),
                    (List<String>) entity.getProperty("tags")));

        return seiten;
    }

    public static Set<Seite> findAllSeiten(DatastoreService datastore) {

        Query query = new Query("Seite");
        List<Entity> seiteEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        Set<Seite> seiten = new HashSet<Seite>();

        for (Entity entity : seiteEntities)
            seiten.add(new Seite((String) entity.getProperty("siteName"),
                    (String) entity.getProperty("feedUrl")));

        return seiten;
    }

    private Set<Deal> findDealsFromFeeds(Set<Seite> seiten, Date maxAgeDate) {

        Calendar calendar = Calendar.getInstance(GERMAN_TIMEZONE);

        Set<Deal> deals = new HashSet<Deal>();

        for (Seite seite : seiten) {

            XmlReader reader = null;

            try {

                URL url = new URL(seite.getFeedUrl());

                reader = new XmlReader(url);

                SyndFeed feed = new SyndFeedInput().build(reader);

                for (Object entryObj : feed.getEntries()) {

                    SyndEntry entry = (SyndEntry) entryObj;

                    Date publishedDate = entry.getPublishedDate();

                    /*
                     * Artikel ohne Published-Datum sowie zu alte Einträge
                     * werden übersprungen.
                     */
                    if (publishedDate == null || maxAgeDate.after(publishedDate))
                        continue;

                    /*
                     * Das Datum des Tages wird zur Anzeige des Alters in Tagen
                     * benötigt und es ist performanter dieses nur einmalig beim
                     * Eintrage zu berechnen statt beim Darstellen der Seite.
                     */
                    calendar.setTime(publishedDate);

                    Deal deal = new Deal(
                            seite,
                            Deal.getFilteredTitle(entry.getTitle()),
                            entry.getLink(),
                            publishedDate.getTime(),
                            (long) calendar.get(Calendar.DAY_OF_YEAR));

                    deals.add(deal);
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Parsen von '" + seite + "'.", e);
            } finally {
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Reader konnte nicht geschlossen werden.", ioe);
                }
            }
        }

        return deals;
    }

    private Set<Deal> filterNeueDeals(Set<Deal> deals, DatastoreService datastore) {

        Set<Deal> bekannteDeals = DealCache.getInstance().readFromCache(datastore);

        Set<Deal> neueDeals = new HashSet<Deal>();

        for (Deal deal : deals)
            if (!bekannteDeals.contains(deal))
                neueDeals.add(deal);

        return neueDeals;
    }

    private void storeDeals(DatastoreService datastore, Set<Deal> deals) {

        for (Deal deal : deals) {

            Entity dealEntity = new Entity(deal.getDatastoreKey());

            dealEntity.setProperty("siteName", deal.getSeite().getName());
            dealEntity.setProperty("dealTitle", deal.getTitel());
            dealEntity.setProperty("dealLink", deal.getLink());
            dealEntity.setProperty("pubTime", deal.getPubTime());
            dealEntity.setProperty("dayOfYear", deal.getDayOfYear());

            datastore.put(dealEntity);
        }

        /*
         * Erst in den Cache einfügen, was erfolgreich in die Datenbank
         * geschrieben wurde.
         */
        DealCache.getInstance().addToCache(deals);
    }

    private int deleteOldDeals(DatastoreService datastore, Date maxAgeDate) {

        List<Key> alteKeys = new ArrayList<Key>();

        Query.FilterPredicate ageFilter =
                new Query.FilterPredicate("pubTime", FilterOperator.LESS_THAN_OR_EQUAL, maxAgeDate.getTime());

        Query query = new Query("Deal").setFilter(ageFilter).setKeysOnly();
        List<Entity> dealEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity entity : dealEntities)
            alteKeys.add(entity.getKey());

        datastore.delete(alteKeys);

        return alteKeys.size();
    }

    private Date calcMaxAgeDate() {

        Calendar calendar = Calendar.getInstance(GERMAN_TIMEZONE);
        calendar.roll(Calendar.DAY_OF_YEAR, -MAX_AGE_IN_DAYS);
        Date maxAgeDate = calendar.getTime();

        return maxAgeDate;
    }

    /**
     * AppEngine kann nicht richtig mit Locales umgehen... Das hier ist
     * entsprechend ein Workaround.
     */
    public static Date correctDateForAppEngine(long time) {
        return new Date(time + GERMAN_TIMEZONE.getOffset(time));
    }
}
