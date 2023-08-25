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

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Streams.stream;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.Course.EAST;
import static org.mitre.caasd.commons.Course.NORTH;
import static org.mitre.caasd.commons.TimeWindow.enclosingWindow;
import static org.mitre.caasd.commons.maps.FeatureSetBuilder.newFeatureSetBuilder;
import static org.mitre.caasd.commons.maps.MapBuilder.newMapBuilder;
import static org.mitre.caasd.commons.maps.MapFeatures.circle;
import static org.mitre.caasd.commons.maps.MapFeatures.filledCircle;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.PositionRecord;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.math.locationfit.LocalPolyInterpolator;
import org.mitre.caasd.commons.testing.TestLocationDatum;

class MapBuilderTest {

    @TempDir
    public File tempDir;

    @Test
    public void widthCannotBeSetTwice() {

        Distance dist = Distance.ofNauticalMiles(10);
        int pixelWidth = 1200;
        int zoomLevel = 13;

        assertThrows(
            IllegalStateException.class,
            () -> newMapBuilder().width(dist).width(dist)
        );

        assertThrows(
            IllegalStateException.class,
            () -> newMapBuilder().width(pixelWidth, zoomLevel).width(pixelWidth, zoomLevel)
        );

        assertThrows(
            IllegalStateException.class,
            () -> newMapBuilder().width(dist).width(pixelWidth, zoomLevel)
        );

        assertThrows(
            IllegalStateException.class,
            () -> newMapBuilder().width(pixelWidth, zoomLevel).width(dist)
        );
    }

    @Test
    public void plotMap_exactly600Pixels() {

        LatLong home = LatLong.of(32.8968, -97.0380);

        BufferedImage newMapImage = newMapBuilder()
            .solidBackground(Color.BLUE)
            .center(home)
            .width(600, 14)
            .addFeatures(mapFeatures(home))
            .toImage();  //use .toFile(someFile); to see the plot

        assertThat(newMapImage.getWidth(), is(600));
        assertThat(newMapImage.getHeight(), is(600));
    }

    @Test
    public void widthMustBeSet() {

        assertThrows(
            NullPointerException.class,
            () -> newMapBuilder()
                .solidBackground(Color.BLUE)
                .center(LatLong.of(32.8968, -97.0380))
                .toImage()
        );
    }

    @Test
    public void plotMapOnSolidBackground() throws Exception {

        LatLong home = LatLong.of(32.8968, -97.0380);

        BufferedImage newMapImage = newMapBuilder()
            .solidBackground(Color.BLUE)
            .center(home)
            .width(Distance.ofNauticalMiles(10))
            .addFeatures(mapFeatures(home))
            .toImage();  //use .toFile(someFile); to see the plot

        BufferedImage expectedMapImage = ImageIO.read(
            FileUtils.getResourceFile("randomWalkOnSolidBackground.png")
        );

        verifyImagesMatch(newMapImage, expectedMapImage);
    }

    @Test
    public void plotDotsOnSolidBackground() throws Exception {
        /*
         * This test shows how LatLong datasets can be drawn as a collection of many individual
         * MapFeatures OR as one "composite" MapFeature
         */

        //render these LatLongs as separate MapFeatures
        List<LatLong> blueDots = Stream.of(
            LatLong.of(32.8968, -97.0380),
            LatLong.of(32.8968, -97.0380).project(EAST, Distance.ofMiles(1)),
            LatLong.of(32.8968, -97.0380).project(EAST, Distance.ofMiles(2)),
            LatLong.of(32.8968, -97.0380).project(EAST, Distance.ofMiles(3)),
            LatLong.of(32.8968, -97.0380).project(EAST, Distance.ofMiles(4))
        ).collect(toList());

        //render the LatLong as one group
        List<LatLong> redDots = Stream.of(
            LatLong.of(32.8968, -97.0380),
            LatLong.of(32.8968, -97.0380).project(NORTH, Distance.ofMiles(1)),
            LatLong.of(32.8968, -97.0380).project(NORTH, Distance.ofMiles(2)),
            LatLong.of(32.8968, -97.0380).project(NORTH, Distance.ofMiles(3)),
            LatLong.of(32.8968, -97.0380).project(NORTH, Distance.ofMiles(4))
        ).collect(toList());

        BufferedImage newMapImage = newMapBuilder()
            .solidBackground(Color.BLACK)
            .center(LatLong.of(32.8968, -97.0380))   //the center of the random distribution we are drawing LatLongs from
            .width(800, 10)
            //render each LatLong in this list separately
            .addFeatures(blueDots, loc -> filledCircle(loc, Color.BLUE, 6))
            //render this list of LatLongs as a single MapFeaure
            .addFeature(toMapFeature(redDots))
            .toImage();

        BufferedImage expectedMapImage = ImageIO.read(
            FileUtils.getResourceFile("blueAndRedDots.png")
        );

        verifyImagesMatch(newMapImage, expectedMapImage);
    }

    MapFeature toMapFeature(List<LatLong> locations) {

        List<MapFeature> asManyFeatures = locations.stream()
            .map(loc -> circle(loc, Color.RED, 6, 1.0f))
            .collect(toList());

        MapFeature asOneFeature = MapFeatures.compose(asManyFeatures);

        return asOneFeature;
    }


    @Disabled //may not work on Bamboo due to fonts???
    @Test
    public void plotMapOnDebugBackground() throws Exception {

        LatLong home = LatLong.of(32.8968, -97.0380);

        BufferedImage newMapImage = newMapBuilder()
            .debugTiles()
            .center(home)
            .width(Distance.ofNauticalMiles(10))
            .addFeatures(mapFeatures(home))
            .toImage();  //use .toFile(someFile); to see the plot

        BufferedImage expectedMapImage = ImageIO.read(
            FileUtils.getResourceFile("randomWalkOnDebugBackground.png")
        );

        verifyImagesMatch(newMapImage, expectedMapImage);
    }


    /* Ensure size equivalence and pixel equivalence. */
    private void verifyImagesMatch(BufferedImage img1, BufferedImage img2) {

        assertThat(img1.getWidth(), is(img2.getWidth()));
        assertThat(img1.getHeight(), is(img2.getHeight()));

        for (int i = 0; i < img1.getWidth(); i++) {
            for (int j = 0; j < img1.getHeight(); j++) {
                assertThat(img1.getRGB(i, j), is(img2.getRGB(i, j)));
            }
        }
    }

    private FeatureSet mapFeatures(LatLong center) {
        return newFeatureSetBuilder()
            .addCircle(center, Color.RED, 30, 4.0f)
            .addPath(randomWalk(center), Color.GREEN, 2.0f)
            .build();
    }

    //generate some LatLong data so we can show how to plot a general path..
    List<LatLong> randomWalk(LatLong start) {

        LinkedList<LatLong> list = newLinkedList();
        list.add(start);
        Random rng = new Random(17L);
        int n = 100;

        for (int i = 0; i < n; i++) {
            Course direction = Course.of(rng.nextDouble() * 90.0, Course.Unit.DEGREES);
            LatLong next = list.getLast().project(direction, Distance.ofNauticalMiles(0.05));
            list.add(next);
        }

        return list;
    }


    @Test
    public void canBuildPlotOfTrack() {

        String dataFile = getProperty("user.dir") + "/src/test/resources/gentleErrorTrack.txt";

        Iterable<TestLocationDatum> dataSouce = TestLocationDatum.parseFile(new File(dataFile));

        List<PositionRecord<TestLocationDatum>> positions = stream(dataSouce)
            .map(datum -> PositionRecord.of(
                datum,
                new Position(
                    datum.time(),
                    LatLong.of(datum.latitude(), datum.longitude()),
                    datum.altitude())
                )
            )
            .collect(toList());

        List<MapFeature> circles = positions.stream()
            .map(pr -> filledCircle(pr.latLong(), Color.RED, 12))
            .collect(toList());

        MapFeature smoothedTrackPath = MapFeatures.path(
            fitLatLongs(positions),
            Color.cyan,
            2.0f
        );

//        LatLong home = LatLong.of(040.4827,-076.2183); //one bad point "in the loop"
        LatLong home = LatLong.of(040.23077, -075.28107); //one bad point "in the track

        newMapBuilder()
//            .mapBoxDarkMode()  //this requires having the Mapbox secret available (which might not work in CI)
            .tileSource(new MonochromeTileServer(Color.BLACK))
            .center(home)
            .width(Distance.ofNauticalMiles(7.5))
            .useLocalDiskCaching(Duration.ofDays(7))
            .addFeatures(circles)
            .addFeature(smoothedTrackPath)
            .toFile(new File(tempDir, "trackWithGentleError.png"));
    }

    private List<LatLong> fitLatLongs(List<PositionRecord<TestLocationDatum>> rawData) {

        LocalPolyInterpolator fitter = new LocalPolyInterpolator(Duration.ofSeconds(60));

        TimeWindow window = enclosingWindow(rawData);

        return stream(window.iterator(Duration.ofSeconds(2L)))
            .map(time -> fitter.floorInterpolate(rawData, time).get())
            .map(kr -> kr.latLong())
            .collect(toList());
    }

}