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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.Course.EAST;
import static org.mitre.caasd.commons.Course.NORTH;
import static org.mitre.caasd.commons.Course.SOUTH;
import static org.mitre.caasd.commons.Course.WEST;
import static org.mitre.caasd.commons.maps.FeatureSetBuilder.newFeatureSetBuilder;
import static org.mitre.caasd.commons.maps.MapBoxApi.Style.DARK;
import java.awt.Color;
import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

class MapImageTest {

    @TempDir
    public File tempDir;

    @Disabled
    @Test
    public void drawSimpleMapWithNoFeatures() {
        //Disabled because (1) downloaded map data takes a while, (2) tile server changes output...so we can't test against image equality

        MapImage map = new MapImage(
            new MapBoxApi(DARK),  //tile server
            LatLong.of(38.9223, -77.2016), //center point
            Distance.ofNauticalMiles(2.5) //map width
        );

        map.plotToFile(new File("mapWithoutDecoration.jpg"));
    }


    @Disabled
    @Test
    public void drawMapWithAdditionalFeatures() {
        //Disabled because (1) downloaded map data takes a while, (2) tile server changes output...so we can't test against image equality

        LatLong lostDog = LatLong.of(38.9223, -77.2016);

        MapImage map = new MapImage(
            new MapBoxApi(DARK),  //tile server
            lostDog, //center point
            Distance.ofNauticalMiles(2.5) //map width
        );

        //create a list of MapFeatures that need to be drawn...
        FeatureSet features = newFeatureSetBuilder()
            .addCircle(lostDog, Color.RED, 30, 4.0f)
            .addLine(
                lostDog.project(NORTH, Distance.ofNauticalMiles(1.0)),
                lostDog.project(SOUTH, Distance.ofNauticalMiles(1.0)),
                Color.MAGENTA,
                1.f
            )
            .addFilledShape(
                boxAround(lostDog),
                new Color(255, 255, 255, 25)) //use Alpha channel for transparency!
            .build();

        map.plotToFile(features, new File("mapWithDecoration.jpg"));
    }


    @Disabled
    @Test
    public void drawMovingDot() {
        //Disabled because (1) downloaded map data takes a while, (2) tile server changes output...so we can't test against image equality

        LatLong lostDog = LatLong.of(38.9223, -77.2016);

        MapImage map = new MapImage(
            new MapBoxApi(DARK),  //tile server
            lostDog, //center point
            Distance.ofNauticalMiles(2.5) //map width
        );

        for (int i = 0; i < 10; i++) {
            drawMovieFrame(lostDog, map, i);
        }
    }

    private void drawMovieFrame(LatLong lostDog, MapImage map, int i) {

        FeatureSet features = newFeatureSetBuilder()
            .addCircle(lostDog, Color.RED, 30, 4.0f)
            .setStrokeWidth(3.0f)
            .addCircle(lostDog.project(NORTH, Distance.ofNauticalMiles(.1).times(i)), Color.BLUE,
                40)
            .build();

        map.plotToFile(features, new File("movingDot_" + i + ".jpg"));
    }


    //find a "diamond of LatLongs" around a center point
    private List<LatLong> boxAround(LatLong center) {
        return newArrayList(
            center.project(NORTH, Distance.ofNauticalMiles(1)),
            center.project(EAST, Distance.ofNauticalMiles(1)),
            center.project(SOUTH, Distance.ofNauticalMiles(1)),
            center.project(WEST, Distance.ofNauticalMiles(1))
        );
    }

    @Test
    public void mapImagesDefinedByPixelSizeAreThatPixelSize() {

        int WIDTH = 640;

        MapImage map = new MapImage(
            new DebugTileServer(),
            LatLong.of(38.9223, -77.2016), //center point
            WIDTH,
            12 //zoom
        );

        assertThat(map.plot().getWidth(), is(WIDTH));
        assertThat(map.plot().getHeight(), is(WIDTH));

        map.plotToFile(new File(tempDir, "640x640Map.jpg"));
    }

}