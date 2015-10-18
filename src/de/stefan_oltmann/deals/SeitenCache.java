package de.stefan_oltmann.deals;

import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;

public class SeitenCache extends AbstractCache<Seite> {

    private static SeitenCache instance;

    public static SeitenCache getInstance() {

        if (instance == null)
            instance = new SeitenCache();

        return instance;
    }

    /**
     * 3 Stunden Delay für eine neue Seite (selten)
     */
    @Override
    public int getMaxCacheAgeMs() {
        return 2 * 60 * 60 * 1000;
    }

    @Override
    protected Set<Seite> readFromDataStore(DatastoreService datastore) {
        return RefreshDealsServlet.findAllSeiten(datastore);
    }

}
