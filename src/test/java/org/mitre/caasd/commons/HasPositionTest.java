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

package org.mitre.caasd.commons;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mitre.caasd.commons.HasPosition.maxLatitude;
import static org.mitre.caasd.commons.HasPosition.maxLongitude;
import static org.mitre.caasd.commons.HasPosition.minLatitude;
import static org.mitre.caasd.commons.HasPosition.minLongitude;

import java.util.List;

import org.junit.jupiter.api.Test;

public class HasPositionTest {

    @Test
    public void testDistanceInNmTo() {

        PositionHaver one = new PositionHaver(LatLong.of(0.0, 0.0));
        PositionHaver two = new PositionHaver(LatLong.of(1.0, 1.0));

        double EXPECTED_DIST_IN_KM = 157.2;
        double KM_PER_NM = 1.852;
        double expectedDistance = EXPECTED_DIST_IN_KM / KM_PER_NM;
        double actualDistance = one.distanceInNmTo(two);

        double TOLERANCE = 0.1;

        assertEquals(expectedDistance, actualDistance, TOLERANCE);
    }

    @Test
    public void testMinMaxMethods() {

        PositionHaver v1 = new PositionHaver(LatLong.of(40.75, -73.9));
        PositionHaver v2 = new PositionHaver(LatLong.of(40.75, -74.1));
        PositionHaver v3 = new PositionHaver(LatLong.of(40.7, -74.1));
        PositionHaver v4 = new PositionHaver(LatLong.of(40.7, -73.9));

        List<PositionHaver> points = newArrayList(v1, v2, v3, v4);

        double TOLERANCE = 0.001;
        assertEquals(40.7, minLatitude(points), TOLERANCE);
        assertEquals(40.75, maxLatitude(points), TOLERANCE);
        assertEquals(-74.1, minLongitude(points), TOLERANCE);
        assertEquals(-73.9, maxLongitude(points), TOLERANCE);
    }

    public static class PositionHaver implements HasPosition {

        LatLong location;

        PositionHaver(LatLong location) {
            this.location = location;
        }

        @Override
        public LatLong latLong() {
            return location;
        }
    }
}
