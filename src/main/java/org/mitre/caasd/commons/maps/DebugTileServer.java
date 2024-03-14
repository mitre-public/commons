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

import static java.awt.Font.PLAIN;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * This "TileServer" returns Map Tile images that describe themselves for debugging purposes.
 */
public class DebugTileServer implements TileServer {

    int tileSize = 512;

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
     * @param ta This TileAddress is ignored (and only provided due to interface definition)
     *
     * @return A 512x512 BufferedImage in which every pixel is set to the same color
     */
    @Override
    public BufferedImage downloadMap(TileAddress ta) {

        // return a black tile with a white outline...
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, tileSize, tileSize);

        // draw "x=943  y=1651  zoom=12" on the top of the image
        String msg = "x=" + ta.xIndex() + "  y=" + ta.yIndex() + "  zoom=" + ta.zoomLevel();
        g.setFont(new Font("Avenir", PLAIN, 32));
        g.drawString(msg, 30, 30);

        return img;
    }
}
