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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.fileutil.FileLineIterator;

import com.esri.core.geometry.ogc.OGCGeometry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GeometryTest {

    static String MICHIGAN_1 = "org/mitre/caasd/commons/geometry/michiganCities.geojson";
    static String MICHIGAN_2 = "org/mitre/caasd/commons/geometry/michiganCities2.geojson";

    public static LatLong lansing = LatLong.of(42.7325, -84.5555);
    static LatLong cadillac = LatLong.of(44.2520, -85.4012);
    static LatLong traverseCity = LatLong.of(44.7631, -85.6206);
    static LatLong mackinacIsland = LatLong.of(45.8492, -84.6189);
    static LatLong mackinawCity = LatLong.of(45.7775, -84.7271);

    static Iterator<String> lines1() {
        return new FileLineIterator(getResourceFile(MICHIGAN_1));
    }

    static Iterator<String> lines2() {
        return new FileLineIterator(getResourceFile(MICHIGAN_2));
    }

    private Geometry.Props props() {
        TimeWindow window = TimeWindow.of(Instant.EPOCH, Instant.now());

        return new Geometry.Props.Builder()
            .setFloor(0.0f)
            .setCeiling(1000.0f)
            .setTimes(Collections.singleton(window))
            .build();
    }

    @Test
    public void testFailOnlyGeo() {
        ToOGC proj = ToOGC.WGS84();
        String json = proj.point(lansing).asGeoJson();

        assertThrows(GeometryException.class,
            () -> Geometry.fromFeature(json)
        );
    }

    @Test
    public void testFailNoProps() {
        Gson gson = new Gson();
        JsonObject ele = new JsonObject();
        ele.add("properties", props().toJsonElement());
        String json = gson.toJson(ele);
        assertThrows(GeometryException.class,
            () -> Geometry.fromFeature(json)
        );
    }

    @Test
    public void testFailNoGeo() {
        Gson gson = new Gson();
        JsonObject ele = new JsonObject();
        ele.add("geometry", ToOGC.toJsonElement(ToOGC.WGS84().point(lansing)));
        String json = gson.toJson(ele);
        assertThrows(GeometryException.class,
            () -> Geometry.fromFeature(json)
        );
    }

    @Test
    public void testCeiling() {
        Geometry.Props props = new Geometry.Props.Builder()
            .setCeiling(1000.0f)
            .build();

        assertTrue(props.contains((Float) null));
        assertTrue(props.contains(1000.0f));
        assertTrue(props.contains(0.0f));
        assertFalse(props.contains(1001.0f));
    }

    @Test
    public void testFloor() {
        Geometry.Props props = new Geometry.Props.Builder()
            .setFloor(1000.0f)
            .build();

        assertTrue(props.contains((Float) null));
        assertTrue(props.contains(1000.0f));
        assertFalse(props.contains(0.0f));
        assertTrue(props.contains(1001.0f));
    }

    @Test
    public void testTimes() {
        Instant start = Instant.parse("2017-01-01T00:00:00.00Z");
        Instant end = Instant.parse("2017-01-02T00:00:00.00Z");

        TimeWindow win = TimeWindow.of(start, end);

        Geometry.Props props = new Geometry.Props.Builder()
            .setTimes(Collections.singleton(win))
            .build();

        assertTrue(props.contains((Instant) null));
        assertTrue(props.contains(start));
        assertTrue(props.contains(end));
        assertTrue(props.contains(Instant.parse("2017-01-01T12:00:00.00Z")));
        assertFalse(props.contains(Instant.parse("2017-01-02T12:00:00.00Z")));
        assertFalse(props.contains(Instant.parse("2016-12-31T12:00:00.00Z")));
    }

    @Test
    public void testGeoRW() {

        ToOGC proj = ToOGC.WGS84();
        OGCGeometry geo = proj.point(lansing);

        // build geometry from transient components
        Geometry geometry = new Geometry.Builder()
            .setProps(props())
            .setGeometry(geo)
            .build();

        // write and then re-read
        geometry = Geometry.fromFeature(geometry.toJson());

        // check props consistency
        assertEquals(geometry.props().floor(), new Float(0.0f));
        assertEquals(geometry.props().ceiling(), new Float(1000.0f));
        assertEquals(geometry.props().times().size(), 1);

        // check geometry consistency
        assertTrue(geometry.ogc().contains(geo));
        assertTrue(geo.contains(geometry.ogc()));
        assertFalse(geometry.ogc().contains(proj.point(cadillac)));
    }

    @Test
    public void testLansing() {
        String box = lines1().next();

        Geometry lansingBbox = Geometry.fromFeature(box);
        ToOGC projector = lansingBbox.projector();

        assertEquals(projector.spatialReference().getID(), 4326);
        assertEquals(lansingBbox.geojson(), box);

        assertTrue(lansingBbox.ogc().contains(projector.point(lansing)));
        assertFalse(lansingBbox.ogc().contains(projector.point(cadillac)));

        assertEquals(lansingBbox.props().floor(), new Float(860.0f));
        assertNull(lansingBbox.props().ceiling());
    }

    @Test
    public void testCadillac() {
        Iterator<String> geometries = lines1();
        geometries.next();
        String box = geometries.next();

        Geometry cadillacBbox = Geometry.fromFeature(box);
        ToOGC projector = cadillacBbox.projector();

        assertTrue(cadillacBbox.ogc().contains(projector.point(cadillac)));
        assertFalse(cadillacBbox.ogc().contains(projector.point(lansing)));

        assertNull(cadillacBbox.props().floor());
        assertEquals(cadillacBbox.props().ceiling(), new Float(10000.0f));
    }

    @Test
    public void testTraverseCity() {
        Iterator<String> geometries = lines1();
        geometries.next();
        geometries.next();
        String box = geometries.next();

        Geometry traverseBbox = Geometry.fromFeature(box);
        ToOGC projector = traverseBbox.projector();

        assertTrue(traverseBbox.ogc().contains(projector.point(traverseCity)));
        assertFalse(traverseBbox.ogc().contains(projector.point(lansing)));

        assertEquals(traverseBbox.props().ceiling(), new Float(100000.0f));
        assertNull(traverseBbox.props().floor());
    }

    @Test
    public void testMackina_c_w_Feature() {
        Iterator<String> geometries = lines2();
        String box = geometries.next();
        assertThrows(GeometryException.class,
            () -> Geometry.fromFeature(box)
        );
    }

    @Test
    public void testMackina_c_w_FeatureCol() {
        Iterator<String> geometries = lines2();
        String box = geometries.next();

        List<Geometry> geos = Geometry.fromFeatureColl(box);

        assertEquals(geos.size(), 2);
        Geometry mIsl = geos.get(0);
        Geometry mCity = geos.get(1);

        ToOGC projector = mIsl.projector();

        assertTrue(mIsl.ogc().contains(projector.point(mackinacIsland)));
        assertTrue(mCity.ogc().contains(projector.point(mackinawCity)));
    }
}
