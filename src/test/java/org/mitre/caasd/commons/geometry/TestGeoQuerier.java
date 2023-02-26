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

package org.mitre.caasd.commons.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.geometry.GeometryTest.cadillac;
import static org.mitre.caasd.commons.geometry.GeometryTest.lansing;
import static org.mitre.caasd.commons.geometry.GeometryTest.traverseCity;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.LatLong;

import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.collect.Iterables;

public class TestGeoQuerier {

    private Iterable<Geometry> geometries() {
        return Iterables.transform(GeometryTest::lines1, Geometry::fromFeature);
    }

    @Test
    public void testExteriorRectangle() {
        // bounding rectangle of lansing area
        Rectangle rect = geometries().iterator().next().exteriorRectangle();

        LatLong loc = lansing;

        // in geojson lat/lon are swapped, e.g. x=lon y=lat
        assertFalse(rect.contains(loc.latitude(), loc.longitude()));
        assertTrue(rect.contains(loc.longitude(), loc.latitude()));
    }

    @Test
    public void testQuery_TraverseCity() {

        GeoQuerier querier = GeoQuerier.with(geometries());

        LatLong loc = traverseCity;
        List<Geometry> containing = querier.containing(loc, null, null);
        assertEquals(containing.size(), 1);

        containing = querier.containing(loc, 1310.0f, null);
        assertEquals(containing.size(), 1);

        containing = querier.containing(loc, 100001.0f, null);
        assertEquals(containing.size(), 0);
    }

    @Test
    public void testQuery_LansingAndCadillac() {

        GeoQuerier querier = GeoQuerier.with(geometries());

        // check both without altitude constraints
        List<Geometry> containing = querier.containing(cadillac, null, null);
        assertEquals(containing.size(), 2);

        containing = querier.containing(lansing, null, null);
        assertEquals(containing.size(), 2);

        // just cadillac
        containing = querier.containing(cadillac, 9000.0f, null);
        assertEquals(containing.size(), 1);

        containing = querier.containing(cadillac, 7000.0f, null);
        assertEquals(containing.size(), 2);

        // just lansing
        containing = querier.containing(lansing, 900.0f, null);
        assertEquals(containing.size(), 1);

        containing = querier.containing(lansing, 1100.0f, null);
        assertEquals(containing.size(), 2);
    }
}
