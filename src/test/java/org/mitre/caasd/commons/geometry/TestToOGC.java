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
import static org.mitre.caasd.commons.geometry.GeometryTest.lansing;

import org.junit.jupiter.api.Test;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPoint;

public class TestToOGC {

    private ToOGC proj = ToOGC.WGS84();

    @Test
    public void testSR() {
        ToOGC proj = ToOGC.WGS84();
        OGCGeometry point = proj.point(lansing);

        assertEquals(proj.spatialReference().getID(), 4326);
        assertEquals(point.getEsriSpatialReference().getID(), 4326);
    }

    @Test
    public void testConverters() {
        OGCPoint point = proj.point(lansing);
        Point esri = ToOGC.esri(lansing);

        // ensure point coords are swapped
        assertEquals(point.X(), lansing.longitude(), 0.0000001f);
        assertEquals(point.Y(), lansing.latitude(), 0.0000001f);
        assertEquals(esri.getX(), lansing.longitude(), 0.0000001f);
        assertEquals(esri.getY(), lansing.latitude(), 0.0000001f);
    }
}
