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
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.maps.TileAddress.cornerFinder;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;

import org.mitre.caasd.commons.LatLong;

public interface TileServer {

    int maxZoomLevel();

    int maxTileSize();

    URL getUrlFor(TileAddress ta);

    default BufferedImage downloadMap(TileAddress ta) {

        URL url = getUrlFor(ta);

        // Todo -- log this download...

        try {
            InputStream inStream = url.openConnection().getInputStream();
            return ImageIO.read(inStream);
        } catch (IOException e) {
            throw demote("Could not query: " + url.getQuery(), e);
        }
    }

    /**
     * Determine which Tile contains the provided LatLong, get a URL for that Tile, then open a
     * connection to the provided URL, then parse out an Image from the InputStream found.
     */
    default BufferedImage downloadMap(LatLong pointInTile, int zoom) throws IOException {
        TileAddress tile = TileAddress.of(pointInTile, zoom);
        return downloadMap(tile);
    }

    default BufferedImage downloadAndCombineTiles(List<TileAddress> tiles) {
        requireNonNull(tiles);
        checkArgument(!tiles.isEmpty());

        TileAddress topLeftTile = tiles.stream().min(cornerFinder()).get();
        TileAddress bottomRightTile = tiles.stream().max(cornerFinder()).get();

        int size = maxTileSize();

        BufferedImage combined = new BufferedImage(
                size * (bottomRightTile.xIndex() - topLeftTile.xIndex() + 1),
                size * (bottomRightTile.yIndex() - topLeftTile.yIndex() + 1),
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = (Graphics2D) combined.getGraphics();

        int xOffset = topLeftTile.xIndex();
        int yOffset = topLeftTile.yIndex();

        for (TileAddress tile : tiles) {
            BufferedImage img = downloadMap(tile); // these queries are launch in serial...!
            // compute the "row/column" indices used to assemble the combined image
            int imgCol = tile.xIndex() - xOffset;
            int imgRow = tile.yIndex() - yOffset;
            g.drawImage(img, imgCol * size, imgRow * size, size, size, null);
        }

        return combined;
    }

    //    static double wrapLongitude(double lon) {
    //        double z = ((lon + 180.0) % 360.0);
    //        if (z < 0) {
    //            z += 360.0;
    //        }
    //        return z - 180.0;
    //    }

}
