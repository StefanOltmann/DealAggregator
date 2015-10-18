package de.stefan_oltmann.deals;

import java.util.HashSet;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;

public abstract class AbstractCache<T> {

    private long lastRefreshed;
    private Set<T> entities = new HashSet<T>();

    abstract public int getMaxCacheAgeMs();

    public Set<T> readFromCache(DatastoreService datastore) {

        if (entities.isEmpty() || System.currentTimeMillis() > lastRefreshed + getMaxCacheAgeMs()) {

            entities.clear();
            entities.addAll(readFromDataStore(datastore));
            lastRefreshed = System.currentTimeMillis();
        }

        return entities;
    }

    abstract protected Set<T> readFromDataStore(DatastoreService datastore);

    public void addToCache(Set<T> entities) {
        this.entities.addAll(entities);
    }

}
