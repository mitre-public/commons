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
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.Course.*;
import static org.mitre.caasd.commons.Spherical.EARTH_RADIUS_NM;
import static org.mitre.caasd.commons.maps.FeatureSet.noMapFeatures;
import static org.mitre.caasd.commons.maps.MapBuilder.newMapBuilder;
import static org.mitre.caasd.commons.maps.MapFeatures.circle;
import static org.mitre.caasd.commons.maps.TileAddress.tileAddressesSpanning;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * A MapImage is a Map made by (1) combining multiple tiles from a TileServer, and (2) taking a
 * square excerpt around the map's "center point".
 *
 * <p>A MapImage saves its "base image" so that it can be repeatedly "drawn on" without needing to
 * re-acquire map images from a tile server. This design supports efficiently using one MapImage to
 * draw multiple frames of an animation.
 */
public class MapImage {

    //@todo -- MapImage should support augmenting the "undecoratedImage".  This would have real performance impacts when you are going to render 10k frames and EVERY frame would need to redraw a constant MapFeatures like a scale, legend, etc.

    /** The center of the map (after combining and cropping). */
    final LatLong center;

    /** The top-left of the map (after combining and cropping). */
    final LatLong topLeft;

    /** The bottom-right of the map (after combining and cropping). */
    final LatLong bottomRight;

    /** For plotting convenience save the pixel coordinate of the upper left corner of the map */
    final PixelLatLong zeroPixel;

    final int zoomLevel;

    final int tileSize; //the width (in pixels) of a raw tile

    final BufferedImage undecoratedImage; //the combined and cropped image without any MapFeatures

    /**
     * Create a Map by downloading, combining, and then cropping multiple map tiles.  MapImages are
     * always square.
     *
     * @param tileServer Provides standard square map tile images
     * @param center     The center point for the rendered map (after cropping)
     * @param width      The width of the rendered map (after cropping)(i.e. 25 miles)
     */
    public MapImage(TileServer tileServer, LatLong center, Distance width) {
        this.center = requireNonNull(center);

        //Find the top-left and bottom-right corners of the map (ASSUMES the map is a square)
        Distance halfWidth = width.times(.5);
        this.topLeft = center.project(WEST, halfWidth).project(NORTH, halfWidth);
        this.bottomRight = center.project(EAST, halfWidth).project(SOUTH, halfWidth);

        this.tileSize = tileServer.maxTileSize();

        //Guess a good value for the zoom level.  Assume a "good map size" is 2 or 3 tiles wide & tall
        this.zoomLevel = computeZoomLevel(width);

        this.undecoratedImage = buildCroppedImage(
            tileServer, topLeft, bottomRight, zoomLevel
        );

        //Save the "global pixel coordinates" of the top left corner of the map.
        //This pixel coordinate becomes the "new (0,0) coordinate" in "pixel space" and allows us to locate where MapFeatures get draw
        this.zeroPixel = new PixelLatLong(topLeft, zoomLevel, tileSize);
    }

    /**
     * Create a Map by downloading, combining, and then cropping multiple map tiles. MapImages are
     * always square.
     *
     * @param tileServer    Provides standard square map tile images
     * @param center        The center point for the rendered map (after cropping)
     * @param widthInPixels The width of the rendered map in pixels (after cropping)
     * @param zoomLvl       The "zoom level" of the map.
     */
    public MapImage(TileServer tileServer, LatLong center, int widthInPixels, int zoomLvl) {
        this.center = requireNonNull(center);
        this.zoomLevel = zoomLvl;
        this.tileSize = tileServer.maxTileSize();

        int half = widthInPixels / 2;
        PixelLatLong cp = new PixelLatLong(center, zoomLvl, tileSize);

        this.topLeft = new PixelLatLong(cp.x() - half, cp.y() - half, zoomLvl, tileSize)
            .latLong();

        this.bottomRight = new PixelLatLong(cp.x() + half, cp.y() + half, zoomLvl, tileSize)
            .latLong();

        this.undecoratedImage = buildCroppedImage(tileServer, topLeft, bottomRight, zoomLvl);
        this.zeroPixel = new PixelLatLong(topLeft, zoomLvl, tileSize);
    }

    /**
     * Build the "image" part of a MapImage by (1) getting the necessary Map Tile images from the
     * TileServer, (2) combining those tiles into a single image, (3) cropping the region of
     * interest out of the combined image
     *
     * @param tileServer  The source of Map Tile images
     * @param topLeft     The LatLong of the top left corner of the desired image
     * @param bottomRight The LatLong of the bottom right corner of the desired image
     * @param zoomLevel   The zoom level for the map
     * @return A BufferedImage built from 1 or more tiles
     */
    private static BufferedImage buildCroppedImage(
        TileServer tileServer,
        LatLong topLeft,
        LatLong bottomRight,
        int zoomLevel
    ) {
        int tileSize = tileServer.maxTileSize();
        List<TileAddress> tilesInMap = tileAddressesSpanning(topLeft, bottomRight, zoomLevel);
        BufferedImage combinedTileImage = tileServer.downloadAndCombineTiles(tilesInMap);

        //When cropping the "image we want" out of the combinedTileImage we need to reference the top-left corner of the combinedTileImage
        PixelLatLong topLeftOfCombined = TileAddress.of(topLeft, zoomLevel).topLeftPixel(tileSize);
        PixelLatLong excerptTopLeft = new PixelLatLong(topLeft, zoomLevel, tileSize);
        PixelLatLong excerptBottomRight = new PixelLatLong(bottomRight, zoomLevel, tileSize);

        BufferedImage undecoratedImage = combinedTileImage.getSubimage(
            excerptTopLeft.x(topLeftOfCombined),
            excerptTopLeft.y(topLeftOfCombined),
            (int) (excerptBottomRight.x() - excerptTopLeft.x()),
            (int) (excerptBottomRight.y() - excerptTopLeft.y())
        );

        return undecoratedImage;
    }

    /** Find the zoom level where a given distance DOES NOT fit in a single tile. */
    private static int computeZoomLevel(Distance width) {

        //At Zoom Level 0 the distance shown in a tile is the circumference of earth
        Distance currentDistance = Distance.ofNauticalMiles(2 * PI * EARTH_RADIUS_NM);
        int zoomLevel = 0;

        while (width.isLessThan(currentDistance)) {
            zoomLevel++;
            currentDistance = currentDistance.times(.5);
        }

        return zoomLevel;
    }


    /** @return This MapImage as a BufferedImage. */
    public BufferedImage plot() {
        return copyOfImage();
    }

    /** @return This MapImage, with additional MapFeatures, as a BufferedImage. */
    public BufferedImage plot(Iterable<MapFeature> features) {
        //copy the "raw map"...then add these features...
        BufferedImage copy = copyOfImage();
        Graphics2D g = (Graphics2D) copy.getGraphics();

        for (MapFeature f : features) {
            f.drawOn(g, zeroPixel);
        }

        return copy;
    }

    /** @return This MapImage, with additional MapFeatures, as a BufferedImage. */
    public BufferedImage plot(FeatureSet... sets) {
        //copy the "raw map"...then add these features...
        BufferedImage copy = copyOfImage();
        Graphics2D g = (Graphics2D) copy.getGraphics();

        for (FeatureSet set : sets) {
            for (MapFeature mp : set) {
                mp.drawOn(g, zeroPixel);
            }
        }

        return copy;
    }

    /** Write this MapImage to a ".png" or ".jpg" file. */
    public void plotToFile(File targetFile) {
        plotToFile(noMapFeatures(), targetFile);
    }

    /** Write this MapImage, with additional MapFeatures, to a ".png" or ".jpg" file. */
    public void plotToFile(Iterable<MapFeature> features, File targetFile) {
        requireNonNull(features);
        requireNonNull(targetFile);

        writeImageToFile(plot(features), targetFile);
    }


    /** Write this MapImage, with additional MapFeatures, to a ".png" or ".jpg" file. */
    public void plotToFile(File targetFile, FeatureSet... sets) {
        requireNonNull(sets);
        requireNonNull(targetFile);

        writeImageToFile(plot(sets), targetFile);
    }

    /** Draw an image to a ".png" or ".jpg" file. */
    public static void writeImageToFile(BufferedImage img, File targetFile) {
        requireNonNull(img);
        requireNonNull(targetFile);

        String filename = targetFile.getName();
        checkArgument(
            filename.endsWith(".jpg") || filename.endsWith(".png"),
            "Must write to a \".jpg\" file or a \".png\" file"
        );

        //Extract the "jpg" or "png" (accidentally adding the leading "." will cause a failure)
        String imageFormat = filename.substring(filename.lastIndexOf(".") + 1);

        try {
            ImageIO.write(img, imageFormat, targetFile);
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    /**
     * A quick shortcut for "drawing dots on a map". The map image is always centered around the
     * average input LatLong. The dots are always Green, 6 pixels in diameter, and plotted on a
     * dark-mode map.
     *
     * <p>This method is backed by a MapBuilder and some LatLong to MapFeature conversion.
     *
     * <p>Consider adding a call to "writeImageToFile(mapImage, new File("quickMap.jpg"));"
     *
     * @param locations Some LatLong data
     * @param mapWidth  The output image will zoom out enough to show at least this distance.
     * @return An image of a map
     */
    public static BufferedImage plotLocationData(List<LatLong> locations, Distance mapWidth) {

        LatLong center = LatLong.avgLatLong(locations.toArray(new LatLong[0]));

        List<MapFeature> pointData = locations.stream()
            .map(loc -> circle(loc, Color.GREEN, 6, 1.0f))
            .collect(Collectors.toList());

        return newMapBuilder()
            .mapBoxDarkMode()
            .center(center)
            .width(mapWidth)
            .addFeatures(pointData)
            .toImage();
    }

    private BufferedImage copyOfImage() {

        BufferedImage copy = new BufferedImage(
            undecoratedImage.getWidth(),
            undecoratedImage.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR
        );
        Graphics2D g = (Graphics2D) copy.getGraphics();

        g.drawImage(undecoratedImage, null, null);

        return copy;
    }
}