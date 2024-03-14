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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

class PixelLatLongTest {


    @Test
    public void basicPixelLatLongConstruction() {

        LatLong dfw = LatLong.of(32.897480, -97.040443);
        int zoom = 13;
        int tileSize = 512;

        PixelLatLong pll = new PixelLatLong(dfw, zoom, tileSize);

        assertThat(pll.x(), closeTo(966549, 5.0));
        assertThat(pll.y(), closeTo(1690888, 5.0));
    }

    @Test
    public void circularPixelLatLongConstruction() {

        LatLong dfw = LatLong.of(32.897480, -97.040443);
        int zoom = 13;
        int tileSize = 512;

        PixelLatLong pll = new PixelLatLong(dfw, zoom, tileSize);

        PixelLatLong round2 = new PixelLatLong(
            pll.x(), pll.y(), zoom, tileSize
        );

        assertThat(round2.latLong().latitude(), closeTo(32.897480, 0.001));
        assertThat(round2.latLong().longitude(), closeTo(-97.040443, 0.001));

        assertThat(round2.x(), closeTo(966549, 5.0));
        assertThat(round2.y(), closeTo(1690888, 5.0));

        assertThat(pll.x(), closeTo(round2.x(), 1.0));
        assertThat(pll.y(), closeTo(round2.y(), 1.0));
    }
}