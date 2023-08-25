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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * This class is just "syntactic sugar" that adds no new functionality but makes creating maps
 * easier to do.
 *
 * <p>A MapBuilder provides a fluent API to create 1 of 3 output types: a MapImage, a
 * BufferedImage, or a File (png or jpg only).
 *
 * <p>A raw MapImage does not contain "MapFeatures". A MapImage is merely an aggregation of Map
 * tile images from a Tile Server.  This MapBuilder class provides a single entry point that allows
 * you to specify the "background MapImage" AND the "Collection of MapFeatures" in one call chain.
 *
 * <p>Note: Generating a MapImage is best when you intend to repeatedly draw multiple related
 * images (e.g., when rendering a movie with an unchanging map background).
 *
 * <p>The MapBuilder "chain" supports turning on or off a local disk cache of map tile images.
 */
public class MapBuilder {

    private TileServer tileServer = null;
    private LatLong center = null;
    private Distance distanceWidth = null;
    private Integer pixelWidth = null;
    private Integer zoomLevel = null;
    private boolean useDiskCaching = false;
    private Duration cacheRetention = null;
    private final List<MapFeature> featureList = newArrayList();

    public static MapBuilder newMapBuilder() {
        return new MapBuilder();
    }

    public MapBuilder tileSource(TileServer source) {
        requireNonNull(source);
        checkState(this.tileServer == null, "The tileServer was already set");
        this.tileServer = source;
        return this;
    }

    public MapBuilder solidBackground(Color c) {
        return this.tileSource(new MonochromeTileServer(c));
    }

    public MapBuilder debugTiles() {
        return this.tileSource(new DebugTileServer());
    }

    public MapBuilder mapBoxDarkMode() {
        return this.tileSource(new MapBoxApi(MapBoxApi.Style.DARK));
    }

    public MapBuilder mapBoxSatelliteMode() {
        return this.tileSource(new MapBoxApi(MapBoxApi.Style.SATELLITE));
    }

    public MapBuilder mapBoxLightMode() {
        return this.tileSource(new MapBoxApi(MapBoxApi.Style.LIGHT));
    }

    public MapBuilder center(LatLong center) {
        requireNonNull(center, "map center cannot be null");
        this.center = center;
        return this;
    }

    /**
     * This creates a Map with an automatically set zoom-level. The zoom is selected so that the
     * input Distance is about 2 or 3 MapTiles wide.
     */
    public MapBuilder width(Distance width) {
        requireNonNull(width, "width cannot be null");
        checkState(isNull(distanceWidth), "width cannot be set twice, width(Distance) was already called");
        checkState(isNull(pixelWidth), "width cannot be set twice, width(pixelWidth, zoom) was already called");
        checkState(isNull(zoomLevel), "width cannot be set twice, width(pixelWidth, zoom) was already called");
        this.distanceWidth = width;
        return this;
    }

    /** This creates a Map with of a specific size (in pixels) and zoom level */
    public MapBuilder width(int widthInPixels, int zoomLevel) {
        checkArgument(widthInPixels > 0, "widthInPixels must be positive");
        checkArgument(zoomLevel >= 0, "zoomLevel must be at least 0");
        checkState(isNull(distanceWidth), "width cannot be set twice, width(Distance) was already called");
        checkState(isNull(pixelWidth), "width cannot be set twice, width(pixelWidth, zoom) was already called");
        this.pixelWidth = widthInPixels;
        this.zoomLevel = zoomLevel;
        return this;
    }

    public MapBuilder useLocalDiskCaching(Duration cacheRetention) {
        requireNonNull(cacheRetention);
        checkArgument(cacheRetention.getSeconds() > 0,
            "cacheRetention must be greater than 0 seconds");
        this.useDiskCaching = true;
        this.cacheRetention = cacheRetention;
        return this;
    }

    /** Add a single MapFeature to the fully rendered Map. */
    public MapBuilder addFeature(MapFeature feature) {
        requireNonNull(feature, "The MapFeature cannot be null");
        this.featureList.add(feature);
        return this;
    }

    /**
     * Add a MapFeature to the fully rendered Map. This is equivalent to
     * {@code addFeature(renderer.apply(obj));}
     */
    public <T> MapBuilder addFeature(T obj, Function<T, MapFeature> renderer) {
        requireNonNull(obj, "The obj to render cannot be null");
        requireNonNull(renderer, "The MapFeature renderer cannot be null");
        return addFeature(renderer.apply(obj));
    }

    /** Add one or more MapFeatures to the fully rendered Map. */
    public MapBuilder addFeatures(MapFeature... features) {
        requireNonNull(features, "The MapFeature varags cannot be null");
        Collections.addAll(featureList, features);
        return this;
    }

    /** Add one or more MapFeatures to the fully rendered Map. */
    public MapBuilder addFeatures(Iterable<MapFeature> features) {
        requireNonNull(features, "The Iterable<MapFeature> cannot be null");
        for (MapFeature feature : features) {
            featureList.add(feature);
        }
        return this;
    }

    /**
     * Add one or more MapFeatures to the fully rendered Map.  This method streams through the
     * object collection and converts each item to a MapFeature using the provided rendered.
     */
    public <T> MapBuilder addFeatures(Collection<T> objects, Function<T, MapFeature> renderer) {

        List<MapFeature> asMapFeatures = objects.stream()
            .map(renderer)
            .collect(Collectors.toList());

        return addFeatures(asMapFeatures);
    }

    /**
     * Build the MapImage ONLY.  It does not add any MapFeatures to MapImage (because drawing
     * MapFeature on a MapImage is a distinct step that comes AFTER the MapImage is created)
     */
    public MapImage buildWithoutFeatures() {
        requireNonNull(tileServer, "tileServer cannot be null");
        requireNonNull(center, "center cannot be null");

        //Width must be set be exactly once
        if(isNull(distanceWidth) && isNull(pixelWidth)) {
            throw new NullPointerException("The width not set");
        }

        //add caching if requested...
        TileServer source = (useDiskCaching)
            ? new LocallyCachingTileServer(tileServer, cacheRetention)
            : tileServer;

        if(nonNull(distanceWidth)) {
            return new MapImage(source, center, distanceWidth);
        } else {
            return new MapImage(source, center, pixelWidth, zoomLevel);
        }
    }

    /**
     * Build this MapImage AND plot all know MapFeatures onto it. Provide the output as a
     * BufferedImage.
     */
    public BufferedImage toImage() {
        return buildWithoutFeatures().plot(featureList);
    }

    /**
     * Build this MapImage AND plot all know MapFeatures onto it. Provide the output as a ".png" or
     * ".jpg" file.
     */
    public void toFile(File targetFile) {
        buildWithoutFeatures().plotToFile(featureList, targetFile);
    }
}
