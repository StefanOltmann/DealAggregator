package de.stefan_oltmann.deals;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class DisplayDealsServlet extends HttpServlet {

    private static final TimeZone GERMAN_TIMEZONE = TimeZone.getTimeZone("Europe/Berlin");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        if (req.getParameter("timeinfo") != null) {

            resp.setContentType("text/plain");

            resp.getWriter().println("TimeZone ###\n" + TimeZone.getDefault());
            resp.getWriter().println("Locale ###\n" + Locale.getDefault());
            resp.getWriter().println("Current Time Millis ###\n" + System.currentTimeMillis());
            resp.getWriter().println("Date ###\n" + new Date().toString());
            resp.getWriter().println("Calendar (German Timezone) ###\n" + Calendar.getInstance(GERMAN_TIMEZONE));

            return;
        }

        DatumsFilter datumsFilter = DatumsFilter.HEUTE;

        {
            String datumFilterString = req.getParameter("von");

            if (datumFilterString != null) {

                if (datumFilterString.equals("gestern"))
                    datumsFilter = DatumsFilter.GESTERN;
                else if (datumFilterString.equals("aelter"))
                    datumsFilter = DatumsFilter.AELTER;
            }
        }

        DateFormat dateFormatFull = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.GERMANY);
        DateFormat dateFormatTime = DateFormat.getTimeInstance(
                DateFormat.SHORT, Locale.GERMANY);

        Calendar calendar = Calendar.getInstance(GERMAN_TIMEZONE);
        int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        Date currentDate = RefreshDealsServlet.correctDateForAppEngine(System.currentTimeMillis());

        resp.setContentType("text/html");
        resp.setCharacterEncoding("utf-8");

        PrintWriter writer = resp.getWriter();

        writer.println("<html>");
        writer.println("<head>");
        writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        writer.println("<title>Deals @ " + dateFormatFull.format(currentDate) + "</title>");
        writer.println("</head>");

        writer.println("<body>");

        writer.println("<table width=700><tr><td><center><font size=20>Aktuelle Deals</font></center></td></tr></table>");

        writer.println("<table border=1><tr>");
        writer.println("<td width=230><center>" + (datumsFilter == DatumsFilter.HEUTE ? "<b>heute</b>" : "<a href=?von=heute>heute</a>") + "</center></td>");
        writer.println("<td width=230><center>" + (datumsFilter == DatumsFilter.GESTERN ? "<b>gestern</b>" : "<a href=?von=gestern>gestern</a>") + "</center></td>");
        writer.println("<td width=230><center>" + (datumsFilter == DatumsFilter.AELTER ? "<b>vorgestern & älter</b>" : "<a href=?von=aelter>vorgestern & älter</a>") + "</center></td>");
        writer.println("</tr></table>");

        writer.println("<br>");

        writer.println("<table width=700 cellpadding=4>");

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Set<Deal> alleDeals = DealCache.getInstance().readFromCache(datastore);

        List<Deal> alleDealsSorted = new ArrayList<Deal>(alleDeals);
        java.util.Collections.sort(alleDealsSorted, Deal.PUBTIME_COMPARATOR);

        for (Deal deal : alleDealsSorted) {

            long daysSince = currentDayOfYear - deal.getDayOfYear();

            if (datumsFilter == DatumsFilter.HEUTE && daysSince != 0)
                continue;
            if (datumsFilter == DatumsFilter.GESTERN && daysSince != 1)
                continue;
            if (datumsFilter == DatumsFilter.AELTER && daysSince < 2)
                continue;

            Date date = RefreshDealsServlet.correctDateForAppEngine(deal.getPubTime());

            writer.println("<tr>"
                    + "<td><a href=\"" + deal.getLink() + "\" target=\"_blank\"><b><font size=+1>" + deal.getTitel() + "</font></b></a><br>"
                    + "<font size=-1 color=gray><i>" + generateAlterString(daysSince) + " um " + dateFormatTime.format(date) + " auf " + deal.getSeite().getName() + "</font></i></td></tr>");
        }

        writer.println("</table>");
        writer.println("</html>");
    }

    private static String generateAlterString(long daysSince) {

        if (daysSince == 0)
            return "heute";
        else if (daysSince == 1)
            return "gestern";
        else if (daysSince == 2)
            return "vor zwei Tagen";
        else
            return "vor " + daysSince + " Tagen";
    }

    public static enum DatumsFilter {

        HEUTE,
        GESTERN,
        AELTER;
    }

    public static Set<Deal> findAllDeals(DatastoreService datastore) {

        Set<Seite> seiten = SeitenCache.getInstance().readFromCache(datastore);

        Map<String, Seite> seitenMap = new HashMap<String, Seite>();
        for (Seite seite : seiten)
            seitenMap.put(seite.getName(), seite);

        Set<Deal> deals = new HashSet<Deal>();

        Query query = new Query("Deal");

        List<Entity> dealEntities = datastore.prepare(query).asList(FetchOptions.Builder.withChunkSize(500));

        for (Entity entity : dealEntities) {

            String siteName = (String) entity.getProperty("siteName");

            Seite seite = seitenMap.get(siteName);

            /* Keine Deals gelöschter Seiten ausweisen. */
            if (seite == null)
                continue;

            String title = (String) entity.getProperty("dealTitle");
            String link = (String) entity.getProperty("dealLink");
            Long pubTime = (Long) entity.getProperty("pubTime");
            Long dayOfYear = (Long) entity.getProperty("dayOfYear");

            deals.add(new Deal(seite, title, link, pubTime, dayOfYear));
        }

        return deals;
    }

}
