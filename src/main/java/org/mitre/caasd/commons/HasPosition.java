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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

public interface HasPosition {

    LatLong latLong();

    default double latitude() {
        return latLong().latitude();
    }

    default double longitude() {
        return latLong().longitude();
    }

    /**
     * @param other An object with a known LatLong
     *
     * @return The distance in Nautical Miles the provided object
     */
    default double distanceInNmTo(HasPosition other) {
        checkNotNull(other, "Cannot compute the distance to a null HasPosition object");
        return latLong().distanceInNM(other.latLong());
    }

    default double courseInDegrees(HasPosition that) {
        return Spherical.courseInDegrees(
                this.latitude(), this.longitude(),
                that.latitude(), that.longitude());
    }

    default Course courseTo(HasPosition that) {
        return Course.ofDegrees(courseInDegrees(that));
    }

    default HasPosition projectOut(Double course, Double distance) {
        return () -> Spherical.projectOut(latitude(), longitude(), course, distance);
    }

    default double distanceInRadians(HasPosition that) {
        return Spherical.distanceInRadians(this.distanceInNmTo(that));
    }

    static Double maxLatitude(Collection<? extends HasPosition> locations) {
        checkInput(locations);

        return locations.stream().map(hasPosition -> hasPosition.latitude()).reduce(-Double.MAX_VALUE, Math::max);
    }

    static Double minLatitude(Collection<? extends HasPosition> locations) {
        checkInput(locations);

        return locations.stream().map(hasPosition -> hasPosition.latitude()).reduce(Double.MAX_VALUE, Math::min);
    }

    static Double maxLongitude(Collection<? extends HasPosition> locations) {
        checkInput(locations);

        return locations.stream().map(hasPosition -> hasPosition.longitude()).reduce(-Double.MAX_VALUE, Math::max);
    }

    static Double minLongitude(Collection<? extends HasPosition> locations) {
        checkInput(locations);

        return locations.stream().map(hasPosition -> hasPosition.longitude()).reduce(Double.MAX_VALUE, Math::min);
    }

    static void checkInput(Collection<? extends HasPosition> locations) {
        checkNotNull(locations, "The Collection of HasPositions cannot be null");
        checkArgument(!locations.isEmpty(), "The Collection of HasPositions cannot be empty");
    }

    static HasPosition from(Double lat, Double lon) {
        return () -> new LatLong(lat, lon);
    }

    static HasPosition from(LatLong location) {
        return () -> location;
    }
}
