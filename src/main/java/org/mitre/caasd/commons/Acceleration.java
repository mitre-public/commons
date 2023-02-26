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
import static java.util.Objects.requireNonNull;
import java.io.Serializable;
import java.time.Duration;


/**
 * This Acceleration class is intended to make working with Accelerations less error prone because
 * (1) all Acceleration objects are immutable and (2) the unit is always required and always
 * accounted for.
 *
 * <p>This class is similar in spirit and design to java.time.Instant, java.time.Duration,
 * Distance, and Speed (in this package).
 */
public class Acceleration implements Serializable, Comparable<Acceleration> {

    private static final long serialVersionUID = 4844288921728246175L;

    public static final Acceleration ZERO = Acceleration.of(Speed.ZERO, Duration.ofSeconds(1L));

    /** At construction the "speedDelta/timeDelta" input is normalized to "speedDelta/1_second". */
    private final Speed speedDeltaPerSecond;

    private Acceleration() {
        /*
         * This constructor supports Avro's reflection-based object instantiation. This constructor
         * is private to prevent "standard users" from seeing it.
         *
         * Note, tools that use reflection (e.g. Avro) are the only users who will benefit from this
         * constructor. Those tools use reflection magic to build the object, then they use more
         * reflection magic to mutate the values inside the "Immutable object".
         */

        this(Speed.ZERO, Duration.ofSeconds(1L));
    }

    /**
     * Create an immutable Acceleration object that "normalizes" the provided change in speed to a
     * "per-second" rate.
     *
     * @param speedDelta  The change in a speed measurement
     * @param elapsedTime The length of time over which the observed speedDelta occurred
     */
    public Acceleration(Speed speedDelta, Duration elapsedTime) {
        requireNonNull(speedDelta);
        requireNonNull(elapsedTime);
        checkArgument(!elapsedTime.isNegative());
        checkArgument(!elapsedTime.isZero());
        double msElapsed = elapsedTime.toMillis();
        double numSeconds = msElapsed / 1_000.0;
        this.speedDeltaPerSecond = speedDelta.times(1.0 / numSeconds);
    }

    /**
     * Create an immutable Acceleration object that reflects an "already normalized" "per-second"
     * change Speed change.
     *
     * @param speedDeltaPerSecond The rate of change in a speed quantity. This rate of change is
     *                            pre-normalized to reflect "speedDelta/oneSecond"
     */
    public Acceleration(Speed speedDeltaPerSecond) {
        this(speedDeltaPerSecond, Duration.ofSeconds(1L));
    }

    @Override
    public int compareTo(Acceleration acceleration) {
        return this.speedDeltaPerSecond.compareTo(acceleration.speedDeltaPerSecond);
    }

    /**
     * Create an immutable Acceleration object that "normalizes" the provided change in speed to a
     * "per-second" rate.
     *
     * @param changeInSpeed The change in a speed measurement
     * @param elapsedTime   The length of time over which the observed speedDelta occurred
     */
    public static Acceleration of(Speed changeInSpeed, Duration elapsedTime) {
        return new Acceleration(changeInSpeed, elapsedTime);
    }

    /**
     * Create an immutable Acceleration object that reflects an "already normalized" "per-second"
     * change Speed change.
     *
     * @param speedDeltaPerSecond The rate of change in a speed quantity. This rate of change is
     *                            pre-normalized to reflect "speedDelta/oneSecond"
     */
    public static Acceleration of(Speed speedDeltaPerSecond) {
        return new Acceleration(speedDeltaPerSecond);
    }

    public boolean isLessThan(Acceleration other) {
        return this.speedDeltaPerSecond.isLessThan(other.speedDeltaPerSecond);
    }

    public boolean isLessThanOrEqualTo(Acceleration other) {
        return this.speedDeltaPerSecond.isLessThanOrEqualTo(other.speedDeltaPerSecond);
    }

    public boolean isGreaterThan(Acceleration other) {
        return this.speedDeltaPerSecond.isGreaterThan(other.speedDeltaPerSecond);
    }

    public boolean isGreaterThanOrEqualTo(Acceleration other) {
        return this.speedDeltaPerSecond.isGreaterThanOrEqualTo(other.speedDeltaPerSecond);
    }

    public Acceleration times(double scalar) {
        return new Acceleration(this.speedDeltaPerSecond.times(scalar), Duration.ofSeconds(1L));
    }

    /** @return The absolute value of this Acceleration. */
    public Acceleration abs() {
        return (this.isNegative())
            ? this.times(-1.0)
            : this;
    }

    /**
     * @return The amount a Speed changes in one second when this acceleration is applied to it.
     * In other words, return "This Acceleration * 1 second"
     */
    public Speed speedDeltaPerSecond() {
        return this.speedDeltaPerSecond;
    }

    public boolean isPositive() {
        return speedDeltaPerSecond.isPositive();
    }

    public boolean isNegative() {
        return speedDeltaPerSecond.isNegative();
    }

    public boolean isZero() {
        return speedDeltaPerSecond.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Acceleration that = (Acceleration) o;

        return speedDeltaPerSecond.equals(that.speedDeltaPerSecond);
    }

    @Override
    public int hashCode() {
        return speedDeltaPerSecond.hashCode();
    }
}
