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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;

/**
 * A KineticRecord is a piece of data and a corresponding 4-dimensional position with derivatives.
 *
 * <p>A core design goal of KineticRecord is to provide a way to "add position data" to an unknown
 * data type without altering the source data type. In other words, KineticRecord provides a
 * prebuilt way to use composition over inheritance.
 *
 * <p>KineticRecords can be built by numerically fitting the data provided by a sequence of
 * PositionRecords.
 *
 * @param <T> The datum type (e.g. a NOP Radar Hit, Asdex Point, or ADS-B Point)
 */
public class KineticRecord<T> implements HasTime, HasPosition {

    private final T datum;

    private final KineticPosition position;

    public KineticRecord(T item, KineticPosition position) {
        requireNonNull(item);
        requireNonNull(position);
        this.datum = item;
        this.position = position;
    }

    private KineticRecord() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent standard API users from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */
        this.datum = null;
        this.position = null;
    }

    public T datum() {
        return datum;
    }

    public KineticPosition kinetics() {
        return position;
    }

    @Override
    public Instant time() {
        return kinetics().time();
    }

    @Override
    public LatLong latLong() {
        return kinetics().latLong();
    }

    public Distance altitude() {
        return kinetics().altitude();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.datum);
        hash = 29 * hash + Objects.hashCode(this.position);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KineticRecord<?> other = (KineticRecord<?>) obj;
        if (!Objects.equals(this.datum, other.datum)) {
            return false;
        }
        if (!Objects.equals(this.position, other.position)) {
            return false;
        }
        return true;
    }
}
