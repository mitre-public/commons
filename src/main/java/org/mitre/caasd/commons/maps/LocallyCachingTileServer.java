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

package org.mitre.caasd.commons.maps;

import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

import org.mitre.caasd.commons.fileutil.FileUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Add a local-disk-based cache that allows a TileServer to (1) cache maps across sessions, (2) work
 * properly offline (assuming map images were cached in the recent past) and (3) reduce execution
 * times by avoiding REST API calls to download maps we already have.
 */
public class LocallyCachingTileServer implements TileServer {

    //this cache is designed for session-to-session disk-based caching, not in memory caching.
    private static final int CACHE_SIZE = 64;

    private static final Duration DEFAULT_CACHE_LIFETIME = Duration.ofDays(7);

    private static final String DEFAULT_CACHE_DIR = "mapTileCache";

    //A TileServer we want to hit less often
    private final TileServer inner;

    //A LoadingCache that stores a few images in memory...but is really just a seem to weave in disk-base caching before delegating to the TileServer's REST API.
    private final LoadingCache<TileAddress, BufferedImage> cache;

    public LocallyCachingTileServer(TileServer server, Duration maxAge, String cacheDirectory) {
        requireNonNull(server);
        requireNonNull(cacheDirectory);
        this.inner = server;

        CacheLoader<TileAddress, BufferedImage> onCacheMiss = makeCacheLoader(
            inner,
            cacheDirectory,
            maxAge
        );

        this.cache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .recordStats()
            .build(onCacheMiss);
    }

    public LocallyCachingTileServer(TileServer server, Duration maxAg) {
        this(server, maxAg, DEFAULT_CACHE_DIR);
    }

    public LocallyCachingTileServer(TileServer server) {
        this(server, DEFAULT_CACHE_LIFETIME, DEFAULT_CACHE_DIR);
    }

    @Override
    public int maxZoomLevel() {
        return inner.maxZoomLevel();
    }

    @Override
    public int maxTileSize() {
        return inner.maxTileSize();
    }

    @Override
    public URL getUrlFor(TileAddress ta) {
        return inner.getUrlFor(ta);
    }

    /**
     * Intercept a TileServer's call to download a map from a REST API.  Instead of hitting a REST
     * API as a "first resort" look on disk for a copy of this image.  If a recent map image can't
     * be found download (and store) the needed map image from the REST-API
     */
    @Override
    public BufferedImage downloadMap(TileAddress ta) {

        try {
            return cache.get(ta);
        } catch (ExecutionException ex) {
            throw demote(ex);
        }
    }

    public Cache<TileAddress, BufferedImage> cache() {
        return cache;
    }

    /**
     * Create a CacheLoader that gets called when the **IN-MEMORY** cache misses.  This
     * CacheLoader will frequently fail-over to the disk-based cache.
     *
     * @param fallbackTileServer The tile server to retrieve maps from when all caching fails.
     * @param cacheDir Where cached images assets are stored
     * @param maxAge The maximum amount of time a cached map image is good for
     *
     * @return A CacheLoader to process "in memory cache misses"
     */
    public static CacheLoader<TileAddress, BufferedImage> makeCacheLoader(
        TileServer fallbackTileServer,
        String cacheDir,
        Duration maxAge
    ) {

        return new CacheLoader<TileAddress, BufferedImage>() {

            @Override
            public BufferedImage load(TileAddress ta) throws Exception {

                //We only get here if the "in-memory" cache failed...

                //so first we look for an image file "cacheDir/tileAddress.dat"...
                FileUtils.makeDirIfMissing(cacheDir);
                File cachedImageFile = new File(cacheDir + File.separator + ta.toString() + ".png");

                if (cachedImageFile.exists() && fileIsRecent(cachedImageFile, maxAge)) {
                    return ImageIO.read(cachedImageFile);
                }

                //download the map, store it to disk, return the image
                BufferedImage img = fallbackTileServer.downloadMap(ta);
                ImageIO.write(img, "png", cachedImageFile);
                return img;
            }
        };
    }

    private static boolean fileIsRecent(File file, Duration maxAge) {

        Instant lastModified = Instant.ofEpochMilli(file.lastModified());
        Duration fileAge = Duration.between(lastModified, Instant.now());

        return fileAge.toMillis() < maxAge.toMillis();
    }
}