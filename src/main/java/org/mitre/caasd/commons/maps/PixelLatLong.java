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
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.util.Objects.requireNonNull;

import org.mitre.caasd.commons.LatLong;

/**
 * A PixelLatLong "links" a LatLong to a pixel position in a MapTile Layer.
 *
 * <p>This class assumes map tiles obey standard conventions (e.g. tiles are square, they have
 * x/y/zoom coordinates)
 *
 * <p>potentially helpful resource: https://docs.microsoft.com/en-us/azure/azure-maps/zoom-levels-and-tile-grid?tabs=csharp
 */
public class PixelLatLong {

    private final LatLong location;

    /** The "global pixel x-coordinate for a LatLong". Stored as double to prevent data loss. */
    private final double pixelX;

    /** The "global pixel y-coordinate for a LatLong". Stored as double to prevent data loss. */
    private final double pixelY;

    /** The zoom of the map tile layer" */
    private final int zoom;

    /** The size of the map tiles (in pixels) */
    private final int tileSize;

    /**
     * Decorate a LatLong so that it can uniquely reference a pixel in a map tile.
     *
     * @param position A LatLong location
     * @param zoom The zoom level of the map tile (google map tile zoom level)
     * @param tileSize The size of the map tiles (in pixels)
     */
    public PixelLatLong(LatLong position, int zoom, int tileSize) {
        requireNonNull(position);
        checkArgument(0 <= zoom && zoom <= 25);
        checkArgument(32 <= tileSize && tileSize <= 1024);

        this.location = position;
        this.zoom = zoom;
        this.tileSize = tileSize;

        double x = (position.longitude() + 180) / 360;
        double sinLatitude = sin(position.latitude() * Math.PI / 180);
        double y = 0.5 - log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        double mapSize = mapSize(zoom, tileSize);

        this.pixelX = clip(x * mapSize + 0.5, 0, mapSize - 1);
        this.pixelY = clip(y * mapSize + 0.5, 0, mapSize - 1);
    }

    /**
     * Decorate a pixel in a map tile with a uniquely referenced LatLong.
     *
     * @param x The x coordinate of a pixel (assumes global pixel indexing for the whole map layer)
     * @param y The x coordinate of a pixel (assumes global pixel indexing for the whole map layer)
     * @param zoom The zoom level of the map tile (google map tile zoom level)
     * @param tileSize The size of the map tiles (in pixels)
     */
    public PixelLatLong(double x, double y, int zoom, int tileSize) {
        this.zoom = zoom;
        this.tileSize = tileSize;

        int mapSize = mapSize(zoom, tileSize);

        checkArgument(0 <= x && x <= mapSize);
        checkArgument(0 <= y && y <= mapSize);

        //first use the pixel coordinates to generate a LatLong
        double x_for_longitude = (clip(x, 0, mapSize - 1) / mapSize) - 0.5;
        double y_for_latitude = 0.5 - (clip(y, 0, mapSize - 1) / mapSize);

        this.location = LatLong.of(
            90 - 360 * atan(exp(-y_for_latitude * 2 * PI)) / PI,
            360 * x_for_longitude
        );

        //now use the LatLong to (re)find the pixel coordinate...?
        double x2 = (location.longitude() + 180) / 360;
        double sinLatitude = sin(location.latitude() * Math.PI / 180);
        double y2 = 0.5 - log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        this.pixelX = clip(x2 * mapSize + 0.5, 0, mapSize - 1);
        this.pixelY = clip(y2 * mapSize + 0.5, 0, mapSize - 1);
    }

    /**
     * The "global" x-coordinate of a pixel in a MapTile system (i.e. the pixel value reflects ALL
     * possible map tiles not just this one particular tile (or even aggregate tile).  This value
     * is returned as a double (and not an int that maps to a pixel) to prevent data loss
     */
    public double x() {
        return pixelX;
    }

    /**
     * The "global" y-coordinate of a pixel in a MapTile system (i.e. the pixel value reflects ALL
     * possible map tiles not just this one particular tile (or even aggregate tile).  This value
     * is returned as a double (and not an int that maps to a pixel) to prevent data loss
     */
    public double y() {
        return pixelY;
    }

    /**
     * Convenience function to find the x-coordinate of this PixelLatLong when it appears on excerpt
     * of a mapping layer (i.e. when the local pixel coordinate system does not start at (0,0))
     *
     * @param zeroPixelAnchor The PixelLatLong corresponding to the 0,0 pixel of an Image (i.e. "top
     *                        left" of a map image)
     * @return A shifted x coordinate suitable for map drawing (aka: (int)(this.x() -
     * zeroPixelAnchor.x()))
     */
    public int x(PixelLatLong zeroPixelAnchor) {
        requireNonNull(zeroPixelAnchor);
        checkArgument(zeroPixelAnchor.tileSize == this.tileSize);
        checkArgument(zeroPixelAnchor.zoom == this.zoom);

        return (int) (this.x() - zeroPixelAnchor.x());
    }

    /**
     * Convenience function to find the y-coordinate of this PixelLatLong when it appears on excerpt
     * of a mapping layer (i.e. when the local pixel coordinate system does not start at (0,0))
     *
     * @param zeroPixelAnchor The PixelLatLong corresponding to the 0,0 pixel of an Image (i.e. "top
     *                        left" of a map image)
     * @return A shifted y coordinate suitable for map drawing (aka: (int)(this.y() -
     * zeroPixelAnchor.y()))
     */
    public int y(PixelLatLong zeroPixelAnchor) {
        requireNonNull(zeroPixelAnchor);
        checkArgument(zeroPixelAnchor.tileSize == this.tileSize);
        checkArgument(zeroPixelAnchor.zoom == this.zoom);

        return (int) (this.y() - zeroPixelAnchor.y());
    }

    /** The LatLong this pixel maps to. */
    public LatLong latLong() {
        return location;
    }

    public int zoom() {
        return zoom;
    }

    public int tileSize() {
        return tileSize;
    }

    public TileAddress hostTile() {
        return TileAddress.of(location, zoom);
    }
    /**
     * Calculates width and height of the map in pixels at a specific zoom level from -180 degrees to 180 degrees.
     *
     * @param zoom Zoom Level to calculate width at
     * @param tileSize The size of the tiles in the tile pyramid.
     * @return Width and height of the map in pixels
     */
    public static int mapSize(double zoom, int tileSize) {
        return (int) ceil(tileSize * pow(2, zoom));
    }

    /**
     * Clips a number to the specified minimum and maximum values.
     *
     * @param n The number to clip.
     * @param minValue Minimum allowable value
     * @param maxValue Maximum allowable value
     * @return The clipped value
     */
    private static double clip(double n, double minValue, double maxValue) {
        return min(max(n, minValue), maxValue);
    }

    @Override
    public String toString() {
        return location.toString() + " --> " + this.pixelX + ", " + this.pixelY;
    }
}
