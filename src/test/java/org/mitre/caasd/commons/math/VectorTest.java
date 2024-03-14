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

import static java.lang.Math.sqrt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class VectorTest {

    @Test
    public void cannotBuildVectorWithNoComponents() {

        // Vector must have at least one component
        assertThrows(IllegalArgumentException.class, () -> new Vector(new double[0]));
    }

    @Test
    public void cannotBuildVectorWithNoComponents_factory() {

        // Vector must have at least one component
        assertThrows(IllegalArgumentException.class, () -> Vector.of());
    }

    @Test
    public void testBothConstructors() {
        Vector vector1 = new Vector(new double[] {3.0, 2.2, -1.9});
        Vector vector2 = Vector.of(3.0, 2.2, -1.9);
        assertArrayEquals(vector1.components(), vector2.components(), 0.001);
    }

    @Test
    public void testComponents() {
        double[] components = {0.0, 1.0, 2.0};
        Vector vector = Vector.of(components);
        assertArrayEquals(components, vector.components(), 0.001);
    }

    @Test
    public void testDimension() {
        Vector vector = Vector.of(-1.1, 2.2, 3, 5.5);
        assertEquals(4, vector.dimension());
    }

    @Test
    public void testPlus() {
        Vector vector1 = Vector.of(0.0, 1.0);
        Vector vector2 = Vector.of(-1.0, -1.0);
        assertArrayEquals(new double[] {-1.0, 0.0}, vector1.plus(vector2).components(), 0.001);
    }

    @Test
    public void testAddingVectorsOfDifferentDimensions() {
        Vector vector1 = Vector.of(0.0, 1.0);
        Vector vector2 = Vector.of(-1.0, -1.0, 33.5);

        // Cannot add vectors of different dimensions.
        assertThrows(IllegalArgumentException.class, () -> vector1.plus(vector2));
    }

    @Test
    public void testMinus() {
        Vector vector1 = Vector.of(0.0, 1.0);
        Vector vector2 = Vector.of(-1.0, -1.0);
        assertArrayEquals(new double[] {1.0, 2.0}, vector1.minus(vector2).components(), 0.001);
    }

    @Test
    public void testMultipleSubtractions() {
        Vector vector1 = Vector.of(0.0, 0.0);
        Vector vector2 = Vector.of(1.0, 1.0);
        Vector vector3 = Vector.of(2.0, 2.0);
        assertArrayEquals(
                new double[] {-3.0, -3.0}, vector1.minus(vector2).minus(vector3).components(), 0.001);
    }

    @Test
    public void testSubtractingVectorsOfDifferentDimensions() {
        Vector vector1 = Vector.of(0.0, 1.0);
        Vector vector2 = Vector.of(-1.0, -1.0, 33.5);

        // Cannot subtract vectors of different dimensions.
        assertThrows(IllegalArgumentException.class, () -> vector1.minus(vector2));
    }

    @Test
    public void testScaledBy() {
        Vector vector = Vector.of(1.0, 2.0, -3.0);
        assertArrayEquals(new double[] {-2.0, -4.0, 6.0}, vector.times(-2.0).components(), 0.001);
    }

    @Test
    public void testDot() {
        Vector vector1 = Vector.of(2.0, 1.0);
        Vector vector2 = Vector.of(-2.0, 1.0);
        assertEquals(-3.0, vector1.dot(vector2), 0.001);
    }

    @Test
    public void testDotVectorsOfDifferentDimensions() {
        Vector vector1 = Vector.of(0.0, 1.0);
        Vector vector2 = Vector.of(-1.0, -1.0, 33.5);

        // Cannot take dot product of vectors of different dimensions.
        assertThrows(IllegalArgumentException.class, () -> vector1.dot(vector2));
    }

    @Test
    public void testMagnitude() {
        Vector vector = Vector.of(2.0, -10.0, -11.0);
        double expected = sqrt(4.0 + 100.0 + 121.0);
        assertEquals(expected, vector.magnitude(), 0.01);
    }

    @Test
    public void cannotAlterVectorComponents() {
        Vector vec = Vector.of(1.0, 2.0, 3.0);

        double[] data = vec.components();

        double originalValue = data[1]; // this operation should do nothing to original vec
        data[1] = 66.0;

        double[] dataAfterChange = vec.components();
        assertEquals(originalValue, dataAfterChange[1], 0.000001);
    }

    @Test
    public void canAccessAnyParticularComponent() {
        Vector vec = Vector.of(1.0, 2.0, 3.0);
        assertThat(vec.component(0), is(1.0));
        assertThat(vec.component(1), is(2.0));
        assertThat(vec.component(2), is(3.0));
    }

    @Test
    public void invalidComponentAccessFails_under() {
        Vector vec = Vector.of(1.0, 2.0, 3.0);

        assertThrows(IndexOutOfBoundsException.class, () -> vec.component(-1));
    }

    @Test
    public void invalidComponentAccessFails_over() {
        Vector vec = Vector.of(1.0, 2.0, 3.0);

        assertThrows(IndexOutOfBoundsException.class, () -> vec.component(4));
    }
}
