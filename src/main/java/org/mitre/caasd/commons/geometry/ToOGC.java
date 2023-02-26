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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.LatLong;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPoint;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Geospatial transforms. In general these handle the conversion of 2D XY points in ESRI to the OGC
 * projections based on various coordinate reference systems.
 * <p>
 * The default for this class is to use the WGS84 Ellipsoid, but this can be built from any geojson
 * input where a different coordinate system is directly referenced in the json.
 */
public class ToOGC {
    private static final SpatialReference WGS84 = SpatialReference.create(4326);
    /**
     * The coordinate reference system to use in projecting the provided values.
     */
    private final SpatialReference spatialReference;

    private ToOGC(SpatialReference proj) {
        this.spatialReference = proj;
    }

    public SpatialReference spatialReference() {
        return spatialReference;
    }

    static Point esri(LatLong location) {
        return new Point(location.longitude(), location.latitude());
    }

    static Point esri(Double lat, Double lon) {
        return esri(LatLong.of(lat, lon));
    }

    static Point esri(HasPosition pos) {
        checkNotNull(pos);
        return esri(pos.latLong());
    }

    public OGCPoint point(LatLong location) {
        checkNotNull(location);
        return new OGCPoint(
            esri(location.latitude(), location.longitude()),
            spatialReference
        );
    }

    public OGCPoint point(Double lat, Double lon) {
        return point(LatLong.of(lat, lon));
    }

    public OGCPoint point(HasPosition pos) {
        return point(pos.latLong());
    }

    public OGCPolygon polygon(List<? extends HasPosition> points) {
        List<Point> pts = Lists.transform(points, ToOGC::esri);
        Polygon p = new Polygon();
        p.startPath(pts.get(0));
        pts.forEach(p::lineTo);
        p.lineTo(pts.get(0));
        return new OGCPolygon(p, spatialReference);
    }

    public OGCGeometry parse(JsonObject obj) {
        return parse(obj.toString());
    }

    public OGCGeometry parse(String geojson) {
        OGCGeometry geometry = OGCGeometry.fromGeoJson(geojson);
        geometry.setSpatialReference(spatialReference);
        return geometry;
    }

    public static JsonElement toJsonElement(OGCGeometry geo) {
        return new JsonParser().parse(geo.asGeoJson());
    }

    public static ToOGC from(JsonObject obj) {
        return from(obj.toString());
    }

    /**
     * Parses a GeoJson geometry string into an OGC object. This method will use the reference
     * system specified in the Json unless not present in which case the reference system is assumed
     * to be WGS84.
     */
    public static ToOGC from(String geojson) {
        OGCGeometry geometry = OGCGeometry.fromGeoJson(geojson);
        return null != geometry.getEsriSpatialReference()
            ? new ToOGC(geometry.getEsriSpatialReference())
            : WGS84();
    }

    public static ToOGC withReference(SpatialReference ref) {
        Preconditions.checkArgument(ref.getID() > 0);
        return new ToOGC(ref);
    }

    /**
     * This will perform all conversions WRT the WGS84 ellipsoid.
     */
    public static ToOGC WGS84() {
        return withReference(WGS84);
    }
}
