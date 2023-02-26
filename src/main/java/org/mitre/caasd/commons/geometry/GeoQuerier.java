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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.LatLong;

import com.esri.core.geometry.ogc.OGCGeometry;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.collect.ImmutableMap;
import rx.Observable;

/**
 * Reads the bounding rectangles of all the geometries into an {@link RTree} for quick lookup and
 * then runs the explicit ESRI WGS84 projection containment on the subselection of {@link Geometry}s
 * in the general region of the fusion candidate.
 */
public class GeoQuerier {

    /** {@link Geometry}s keyed by their hashcode. */
    private final ImmutableMap<Integer, Geometry> geometries;

    /** RTree of bounding rectangle for the geometries to their hash codes. */
    private final RTree<Integer, Rectangle> rtree;

    private GeoQuerier(Iterable<Geometry> inputGeometry) {
        checkNotNull(inputGeometry);
        this.geometries = mapByHashcode(inputGeometry);
        this.rtree = populateRTree(this.geometries);
    }

    //static contructor helper method has no local fields
    private static ImmutableMap<Integer, Geometry> mapByHashcode(Iterable<Geometry> geometries) {
        ImmutableMap.Builder<Integer, Geometry> mapBuilder = new ImmutableMap.Builder<>();
        for (Geometry geometry : geometries) {
            mapBuilder.put(geometry.hashCode(), geometry);
        }
        return mapBuilder.build();
    }

    //static contructor helper method has no local fields
    private static RTree<Integer, Rectangle> populateRTree(ImmutableMap<Integer, Geometry> geometries) {

        RTree<Integer, Rectangle> tree = RTree.minChildren(3).maxChildren(6).create();

        for (Map.Entry<Integer, Geometry> entry : geometries.entrySet()) {
            //whoa...this add operation appears to create a branch new tree
            tree = tree.add(entry.getKey(), entry.getValue().exteriorRectangle());
        }
        return tree;
    }

    public static GeoQuerier with(Iterable<Geometry> geometries) {
        return new GeoQuerier(geometries);
    }

    public ImmutableMap<Integer, Geometry> geometries() {
        return geometries;
    }

    private boolean contains(Geometry geometry, LatLong location, Float z, Instant tau) {
        Geometry.Props props = geometry.props();
        OGCGeometry ogc = geometry.ogc();
        ToOGC proj = geometry.projector();
        return ogc.contains(proj.point(location))
            && props.contains(z)
            && props.contains(tau);
    }

    public List<Geometry> query(LatLong location) {

        Observable<Entry<Integer, Rectangle>> entries = rtree.search(
            Geometries.point(location.longitude(), location.latitude())
        );
        List<Geometry> geos = new ArrayList<>();
        entries.subscribe(e -> geos.add(geometries.get(e.value())));
        return geos;
    }

    public List<Geometry> query(HasPosition location) {
        return query(location.latLong());
    }

    public List<Geometry> query(double lat, double lon) {
        return query(LatLong.of(lat, lon));
    }

    /**
     * Performs the fusion returning a list of Object::hashCode's pointing to the geometries
     * determined to be query-able with the point.
     */
    public List<Geometry> containing(LatLong location, Float z, Instant tau) {
        List<Geometry> queried = query(location);
        return queried.stream()
            .filter(g -> contains(g, location, z, tau))
            .collect(Collectors.toList());
    }
}
