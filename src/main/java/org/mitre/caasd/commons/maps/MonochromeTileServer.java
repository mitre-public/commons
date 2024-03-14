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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * This "TileServer" returns Map Tile images that are a solid color.
 *
 * <p>Simple tiles like this can be useful when you just want a plain background color.
 *
 * <p>Simple tiles like this can be useful when writing unit tests that (A) run quickly because they
 * don't access remote assets or the file system and (B) are perfectly repeatable because the images
 * returned do not slowly change as the underlying map data is updated from time to time.
 */
public class MonochromeTileServer implements TileServer {

    private final Color color;

    private final int tileSize = 512;


    public MonochromeTileServer(Color color) {
        requireNonNull(color);
        this.color = color;
    }

    @Override
    public int maxZoomLevel() {
        return 20;
    }

    @Override
    public int maxTileSize() {
        return 512;
    }

    @Override
    public URL getUrlFor(TileAddress ta) {
        throw new UnsupportedOperationException(
            "URLs are not provided because single color tiles can be created directly");
    }

    /**
     * This method doesn't actually "download" a Map from a remote resource.  It generates the
     * BufferedImage on demand
     *
     * @param tile This TileAddress is ignored (and only provided due to interface definition)
     * @return A BufferedImage in which every pixel is set to the same color
     */
    @Override
    public BufferedImage downloadMap(TileAddress tile) {

        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(color);
        g.fillRect(0, 0, tileSize, tileSize);

        return img;
    }
}
