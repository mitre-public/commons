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

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.mitre.caasd.commons.TimeWindow;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A Geometry is a simple wrapper around a geojson string.
 * <p>
 * This class is effectively equivalent to a GeoJSON Feature type.
 */
public class Geometry implements Serializable {

    /**
     * The feature this Geo object is wrapping. e.g.
     * <p>
     * {"type": "Feature", properties" : {}, "geometry" : {}}
     * <p>
     * Where properties contains the CDA properties outlined in the class {@link Geo.Props}.
     * <p>
     * Where the geometry conforms to all typical geojson conventions and may be a polygon, geometry
     * collection, multi-polygon, etc.
     */
    private final String geojson;

    /** Cached parsed properties section of the {@link Geometry#geojson}. */
    private transient Props props;

    /** Cached ESRI WGS84 Projected geometry for faster lookups. */
    private transient OGCGeometry ogc;

    /** Bounding rectangle. */
    private transient Rectangle boundingBox;

    /** The OGC projection conversion object for this system. */
    private transient ToOGC projector;

    private Geometry(Builder bldr) {
        this.props = bldr.props;
        this.ogc = bldr.geometry;
        this.geojson = (null == bldr.geojson)
            ? toJson()
            : bldr.geojson;
    }

    public String geojson() {
        return geojson;
    }

    private static JsonObject properties(String json) {
        JsonElement ele = new JsonParser()
            .parse(json)
            .getAsJsonObject()
            .get("properties");
        if (null == ele || ele.isJsonNull() || !ele.isJsonObject()) {
            throw new GeometryException("Cannot build geo object with null or non-object properties: " + json);
        } else {
            return ele.getAsJsonObject();
        }
    }

    private static JsonObject geometry(String json) {
        JsonElement ele = new JsonParser()
            .parse(json)
            .getAsJsonObject()
            .get("geometry");
        if (null == ele || ele.isJsonNull() || !ele.isJsonObject()) {
            throw new GeometryException("Cannot build geo object with null or non-object geometry: " + json);
        } else {
            return ele.getAsJsonObject();
        }
    }

    public ToOGC projector() {
        if (null == projector) {
            projector = ToOGC.from(geometry(geojson));
        }
        return projector;
    }

    public Props props() {
        if (null == props) {
            props = Props.parse(properties(geojson));
        }
        return props;
    }

    /** Returns the geometry derived from the input GeoJson string returned by getGeoJson. */
    public OGCGeometry ogc() {
        if (ogc == null) {
            ogc = projector().parse(geometry(geojson));
        }
        return ogc;
    }

    /**
     * Returns the axis aligned 2D bounding box for the airspace via the ESRI envelope class. The 2D
     * envelope is used in lieu of a typical bounding box for the default loose containment check.
     */
    Rectangle exteriorRectangle() {
        if (boundingBox == null) {
            Envelope2D env = new Envelope2D();
            ogc().getEsriGeometry().queryLooseEnvelope2D(env);
            Double minLon = env.getLowerLeft().x;
            Double minLat = env.getLowerLeft().y;
            Double maxLon = env.getUpperRight().x;
            Double maxLat = env.getUpperRight().y;
            boundingBox = Geometries.rectangle(minLon, minLat, maxLon, maxLat);
        }
        return boundingBox;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Geometry && ((Geometry) that).geojson().equals(geojson);
    }

    @Override
    public int hashCode() {
        return geojson.hashCode();
    }

    public String toJson() {
        if (null == geojson) {
            Gson gson = new Gson();
            JsonObject ele = new JsonObject();
            ele.addProperty("type", "Feature");
            ele.add("properties", gson.toJsonTree(props()));
            // oof
            ele.add("geometry", ToOGC.toJsonElement(ogc));
            return gson.toJson(ele);
        } else {
            return geojson;
        }
    }

    public static Geometry fromFeature(String feature) {
        return new Builder().setGeoJson(feature).build();
    }

    public static List<Geometry> fromFeatureColl(String fcoll) {
        JsonObject obj = new JsonParser().parse(fcoll).getAsJsonObject();
        JsonArray features = obj.getAsJsonArray("features");
        return IntStream.range(0, features.size())
            .mapToObj(i -> fromFeature(features.get(i).toString()))
            .collect(Collectors.toList());
    }

    /**
     * Interface to access to X-Y portion of the Geo object.
     */
    public interface XY {

        Geometry geo();

        default Boolean contains(Double lat, Double lon) {
            return geo().exteriorRectangle().contains(lat, lon)
                && geo().ogc().contains(geo().projector().point(lat, lon));
        }
    }

    /**
     * Interface for access to the floor/ceiling portion of the Geo object.
     */
    public interface Z {

        Geometry geo();

        default boolean contains(Float z) {
            return geo().props().contains(z);
        }
    }

    /**
     * Interface for access to the temporal portion of the Geo object.
     */
    public interface Temporal {

        Geometry geo();

        default boolean contains(Instant tau) {
            return geo().props().contains(tau);
        }
    }

    public static class Props {

        private final Float floor;
        private final Float ceiling;
        private final Set<TimeWindow> times;

        private Props(Builder bldr) {
            this.floor = bldr.floor;
            this.ceiling = bldr.ceiling;
            this.times = bldr.times;
        }

        public Float floor() {
            return floor;
        }

        public Float ceiling() {
            return ceiling;
        }

        public boolean contains(Float z) {
            return null == z || ((null == floor() || floor() <= z) && (null == ceiling() || ceiling() >= z));
        }

        public Set<TimeWindow> times() {
            return times;
        }

        public boolean contains(Instant tau) {
            return null == times() || null == tau || times().stream().anyMatch(i -> i.contains(tau));
        }

        public JsonElement toJsonElement() {
            return new Gson().toJsonTree(this);
        }

        public String toJson() {
            return new Gson().toJson(this);
        }

        public Builder toBuilder() {
            return new Props.Builder()
                .setFloor(floor)
                .setCeiling(ceiling)
                .setTimes(times);
        }

        public static Props parse(JsonObject obj) {
            return new Gson().fromJson(obj, Props.class);
        }

        public static Props parse(String props) {
            return new Gson().fromJson(props, Props.class);
        }

        public static class Builder {

            private Float floor;
            private Float ceiling;
            private Set<TimeWindow> times = new HashSet<>();

            public Builder setFloor(Float floor) {
                this.floor = floor;
                return this;
            }

            public Builder setCeiling(Float ceiling) {
                this.ceiling = ceiling;
                return this;
            }

            public Builder setTimes(Set<TimeWindow> times) {
                this.times = times;
                return this;
            }

            public Builder addTime(Instant start, Instant end) {
                this.times.add(new TimeWindow(start, end));
                return this;
            }

            public Builder addTimes(List<TimeWindow> itvs) {
                this.times.addAll(itvs);
                return this;
            }

            public Props build() {
                return new Props(this);
            }
        }
    }

    public static class Builder {

        private String geojson;
        private Props props;
        private OGCGeometry geometry;

        public Builder setGeoJson(String geojson) {
            this.geojson = geojson;
            return this;
        }

        public Builder setProps(Props props) {
            this.props = props;
            return this;
        }

        public Builder setProps(String json) {
            return setProps(Props.parse(json));
        }

        public Builder setGeometry(OGCGeometry geo) {
            this.geometry = geo;
            return this;
        }

        public Builder setGeometry(String json) {
            return setGeometry(json);
        }

        public Geometry build() {
            if (null != geojson) {
                // check parsable
                Preconditions.checkNotNull(properties(geojson));
                Preconditions.checkNotNull(geometry(geojson));
            }
            return new Geometry(this);
        }
    }
}
