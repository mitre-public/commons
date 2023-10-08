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
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.mitre.caasd.commons.LatLong;

/**
 * A TileAddress is the (x, y, zoom level) triplet of a Map Tile.
 */
public class TileAddress {

    /** The x coordinate of a tile in a tile grid. */
    private final int xIndex;

    /** The y coordinate of a tile in a tile grid. */
    private final int yIndex;

    private final int zoomLevel;

    public TileAddress(int x, int y, int zoomLevel) {
        checkArgument(x >= 0);
        this.xIndex = x;
        this.yIndex = y;
        this.zoomLevel = zoomLevel;
    }

    public int xIndex() {
        return xIndex;
    }

    public int yIndex() {
        return yIndex;
    }

    public int zoomLevel() {
        return zoomLevel;
    }

    /**
     * Get an excerpt of a URL that is usually used to download tiles from a tileServer.
     *
     * @return "{zoomLevel}/{xIndex}/{yIndex}"
     */
    public String tileUrlComponent() {
        return (zoomLevel + "/" + xIndex + "/" + yIndex);
    }

    @Override
    public String toString() {
        return zoomLevel + "-" + xIndex + "-" + yIndex;
    }

    public PixelLatLong topLeftPixel(int tileSize) {

        return new PixelLatLong(
            xIndex * tileSize, yIndex * tileSize,
            zoomLevel, tileSize
        );
    }

    public PixelLatLong bottomRightPixel(int tileSize) {

        return new PixelLatLong(
            xIndex * tileSize + tileSize, yIndex * tileSize + tileSize,
            zoomLevel, tileSize
        );
    }

    /** @return The TileAddress that contains a specific LatLong location. */
    public static TileAddress of(LatLong loc, int zoom) {
        int scale = 1 << zoom;
        double xFraction = (loc.longitude() + 180) / 360;
        double yFraction =
            (1 - log(tan(toRadians(loc.latitude())) + 1 / cos(toRadians(loc.latitude()))) / PI) / 2;

        int xTile = (int) floor(xFraction * scale);
        int yTile = (int) floor(yFraction * scale);


        if (xTile < 0) {
            xTile = 0;
        }
        if (xTile >= scale) {
            xTile = (scale - 1);
        }
        if (yTile < 0) {
            yTile = 0;
        }
        if (yTile >= scale) {
            yTile = (scale - 1);
        }

        return new TileAddress(xTile, yTile, zoom);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TileAddress that = (TileAddress) o;
        return xIndex == that.xIndex && yIndex == that.yIndex && zoomLevel == that.zoomLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xIndex, yIndex, zoomLevel);
    }

    /** @return The TileAddress that contains a specific LatLong at a given zoom level. */
    public static TileAddress getTileAddress(double lat, double lon, int zoom) {
        return TileAddress.of(LatLong.of(lat, lon), zoom);
    }


    /**
     * @return a {@code Comparator<TileAddress>} That makes can sort TileAddress and easily identify
     *     the topLeft and bottomRight TileAddress.
     */
    public static Comparator<TileAddress> cornerFinder() {
        return (TileAddress o1, TileAddress o2) -> {
            int thisSum = o1.xIndex + o1.yIndex;
            int thatSum = o2.xIndex + o2.yIndex;

            return Integer.compare(thisSum, thatSum);
        };
    }


    /**
     * Assemble a list of all the Tiles that will appear between these two tiles
     *
     * @param topLeft     The "first tile" in a contiguous rectangular region of tiles
     * @param bottomRight The "last tile" in a contiguous rectangular region of tiles
     * @param zoom        The zoom level of all the tiles
     * @return A List of TileAddress in the contiguous rectangular region specified by the params
     */
    public static List<TileAddress> tileAddressesSpanning(
        TileAddress topLeft,
        TileAddress bottomRight,
        int zoom
    ) {
        List<TileAddress> tiles = newArrayList();
        for (int x = topLeft.xIndex(); x <= bottomRight.xIndex(); x++) {
            for (int y = topLeft.yIndex(); y <= bottomRight.yIndex(); y++) {
                tiles.add(new TileAddress(x, y, zoom));
            }
        }
        return tiles;
    }

    /**
     * Assemble a list of all the Tiles that will appear between these two LatLong
     *
     * @param topLeft     The LatLong at the top left of a map
     * @param bottomRight The LatLong at the bottom right of a map
     * @param zoomLevel   The zoom level of all the tiles
     * @return A List of TileAddress in the contiguous rectangular region specified by the params
     */
    public static List<TileAddress> tileAddressesSpanning(
        LatLong topLeft,
        LatLong bottomRight,
        int zoomLevel
    ) {
        return tileAddressesSpanning(
            TileAddress.of(topLeft, zoomLevel),
            TileAddress.of(bottomRight, zoomLevel),
            zoomLevel
        );
    }
}
