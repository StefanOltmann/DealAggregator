package de.stefan_oltmann.deals;

import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;

public class DealCache extends AbstractCache<Deal> {

    private static DealCache instance;

    public static DealCache getInstance() {

        if (instance == null)
            instance = new DealCache();

        return instance;
    }

    /*
     * 3 Tage Seiten cachen
     */
    @Override
    public int getMaxCacheAgeMs() {
        return 3 * 24 * 60 * 60 * 1000;
    }

    @Override
    protected Set<Deal> readFromDataStore(DatastoreService datastore) {
        return DisplayDealsServlet.findAllDeals(datastore);
    }

}
