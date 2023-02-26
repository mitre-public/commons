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

package org.mitre.caasd.commons.math;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.math3.util.FastMath.sqrt;

import java.util.Arrays;

/**
 * A Vector is an Immutable class that, once defined, cannot have its values changed.
 * <p>
 * Notice, the methods plus(Vector other), minus(Vector other), and times(double scalar) all return
 * a new Vector. These methods do not alter the original Vector.
 * <p>
 * A Vector is thread-safe due to its immutability.
 */
public class Vector {

    private final double[] components;

    public Vector(double... components) {
        checkNotNull(components);
        checkArgument(components.length > 0, "Vectors must have at least one component.");
        this.components = components;
    }

    public static Vector of(double... components) {
        return new Vector(components);
    }

    public double[] components() {
        //return a defensive copy to ensure that users cannot alter the vector
        return Arrays.copyOf(components, components.length);
    }

    public double component(int i) {
        return components[i];
    }

    public int dimension() {
        return components.length;
    }

    public Vector plus(Vector other) {
        checkNotNull(other);
        checkDimensionsAreEqual(this, other);

        double[] sum = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            sum[i] = this.components[i] + other.components[i];
        }
        return Vector.of(sum);
    }

    public Vector minus(Vector other) {
        checkDimensionsAreEqual(this, other);

        double[] difference = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            difference[i] = this.components[i] - other.components[i];
        }
        return Vector.of(difference);
    }

    public Vector times(double scalar) {
        double[] rescaled = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            rescaled[i] = components[i] * scalar;
        }
        return Vector.of(rescaled);
    }

    public double dot(Vector other) {
        checkDimensionsAreEqual(this, other);

        double sum = 0;
        for (int i = 0; i < components.length; i++) {
            sum += components[i] * other.components[i];
        }
        return sum;
    }

    /** @return The magnitude (also known as the "Euclidean length" or "l-2 Norm") of this vector. */
    public double magnitude() {
        return sqrt(this.dot(this));
    }

    /** @throws IllegalArgumentException if these vectors have different lengths. */
    public static void checkDimensionsAreEqual(Vector v1, Vector v2) {
        checkArgument(
            v1.components.length == v2.components.length,
            "These vectors have different dimensions"
        );
    }
}
