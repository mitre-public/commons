/*
 *    Copyright 2022 The MITRE Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mitre.caasd.commons;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A CachingCleaner is a DataCleaner that caches its most recent results. The caching behavior is
 * provided by Google's LoadingCache class.
 *
 * @param <T> The type being cleaned.
 */
public class CachingCleaner<T> implements DataCleaner<T> {

    private final DataCleaner<T> cleaner;

    private final LoadingCache<T, Optional<T>> cache;

    /**
     * Add Caching behavior to this DataCleaner. A non-expiring LoadingCache (from Google's guava
     * project) with a maximum size is used by default.
     *
     * @param cleaner   A DataCleaner
     * @param cacheSize The maximum number of (T, cleanedT) pairs results stored in the cache.
     */
    public CachingCleaner(DataCleaner<T> cleaner, int cacheSize) {
        this(cleaner, makeCacheFor(cleaner, cacheSize));
    }

    /**
     * Add Caching behavior to this DataCleaner. An auto-expiring LoadingCache (from Google's guava
     * project) with a maximum size is used by default.
     *
     * @param cleaner    A DataCleaner
     * @param cacheSize  The maximum number of (T, cleanedT) pairs results stored in the cache.
     * @param expiration The duration after which a (T, cleanedT) value pair expires in the cache.
     */
    public CachingCleaner(DataCleaner<T> cleaner, int cacheSize, Duration expiration) {
        this(cleaner, makeExpiringCacheFor(cleaner, cacheSize, expiration));
    }

    /**
     * Combine this DataCleaner and this loading cache
     *
     * @param cleaner The DataCleaner used to load the provided cache
     * @param cache   A cache that recomputes a cleaned {@literal Optional<T>} on every cache miss
     *                (using the {@literal DataCleaner<T>} provided above). This cache can be
     *                manually configured as desired (rather than using the default configuration).
     */
    public CachingCleaner(DataCleaner<T> cleaner, LoadingCache<T, Optional<T>> cache) {
        this.cleaner = checkNotNull(cleaner);
        this.cache = checkNotNull(cache);
    }

    public DataCleaner<T> cleaner() {
        return this.cleaner;
    }

    public Cache<T, Optional<T>> cache() {
        return cache;
    }

    @Override
    public Optional<T> clean(T data) {
        try {
            return cache.get(data);
        } catch (ExecutionException ex) {
            throw demote(ex); // rethrow as DemotedException
        }
    }

    private static <T> LoadingCache<T, Optional<T>> makeCacheFor(DataCleaner<T> cleaner, int cacheSize) {

        return CacheBuilder.newBuilder().maximumSize(cacheSize).recordStats().build(makeCacheLoaderFrom(cleaner));
    }

    private static <T> LoadingCache<T, Optional<T>> makeExpiringCacheFor(
            DataCleaner<T> cleaner, int cacheSize, Duration expiration) {

        return CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterAccess(expiration.toMillis(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build(makeCacheLoaderFrom(cleaner));
    }

    /**
     * Convert this DataCleaner into a CacheLoader.
     *
     * @param <T>     The type being cleaned
     * @param cleaner A DataCleaner
     *
     * @return A CacheLoader that can be used as a component in a LoadingCache.
     */
    public static <T> CacheLoader<T, Optional<T>> makeCacheLoaderFrom(DataCleaner<T> cleaner) {

        return new CacheLoader<T, Optional<T>>() {
            @Override
            public Optional<T> load(T item) throws Exception {
                return cleaner.clean(item);
            }
        };
    }
}
