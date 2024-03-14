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

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Course.Unit.DEGREES;
import static org.mitre.caasd.commons.Course.Unit.RADIANS;
import static org.mitre.caasd.commons.Course.angleBetween;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CourseTest {

    private static final double TOLERANCE = 0.0000001;

    @Test
    public void testConstructor() {
        Course oneDegree = new Course(1.0, DEGREES);
        assertThat(oneDegree.inDegrees(), closeTo(1.0, TOLERANCE));
        assertThat(oneDegree.nativeUnit(), is(DEGREES));

        Course twoRadian = new Course(2.0, RADIANS);
        assertThat(twoRadian.inRadians(), closeTo(2.0, TOLERANCE));
        assertThat(twoRadian.nativeUnit(), is(RADIANS));
    }

    @Test
    public void testConstructionViaOfDegrees() {
        Course oneDegree = Course.ofDegrees(1.0);
        assertThat(oneDegree.inDegrees(), closeTo(1.0, TOLERANCE));
        assertThat(oneDegree.nativeUnit(), is(DEGREES));
    }

    @Test
    public void testConstructionViaOfRadians() {
        Course twoRadian = Course.ofRadians(2.0);
        assertThat(twoRadian.inRadians(), closeTo(2.0, TOLERANCE));
        assertThat(twoRadian.nativeUnit(), is(RADIANS));
    }

    @Test
    public void testUnitConversion() {

        Course ninetyDegrees = Course.ofDegrees(90.0);
        assertThat(ninetyDegrees.inRadians(), closeTo(PI / 2.0, TOLERANCE));

        Course threeSixtyDegrees = Course.ofDegrees(360.0);
        assertThat(threeSixtyDegrees.inRadians(), closeTo(2.0 * PI, TOLERANCE));

        Course zeroDegrees = Course.ofDegrees(0);
        assertThat(zeroDegrees.inRadians(), closeTo(0.0, TOLERANCE));

        Course twoRadian = Course.ofRadians(2.0);
        assertThat(twoRadian.inDegrees(), closeTo(2.0 * 180.0 / PI, TOLERANCE));

        Course piRadian = Course.ofRadians(PI);
        assertThat(piRadian.inDegrees(), closeTo(180.0, TOLERANCE));

        Course zeroRadians = Course.ofRadians(0.0);
        assertThat(zeroRadians.inDegrees(), closeTo(0.0, TOLERANCE));
    }

    @Test
    public void testUnitConversion_negatives() {

        Course ninetyDegrees = Course.ofDegrees(-90.0);
        assertThat(ninetyDegrees.inRadians(), closeTo(-PI / 2.0, TOLERANCE));

        Course threeSixtyDegrees = Course.ofDegrees(-360.0);
        assertThat(threeSixtyDegrees.inRadians(), closeTo(-2.0 * PI, TOLERANCE));

        Course zeroDegrees = Course.ofDegrees(-0);
        assertThat(zeroDegrees.inRadians(), closeTo(-0.0, TOLERANCE));

        Course twoRadian = Course.ofRadians(-2.0);
        assertThat(twoRadian.inDegrees(), closeTo(-2.0 * 180.0 / PI, TOLERANCE));

        Course piRadian = Course.ofRadians(-PI);
        assertThat(piRadian.inDegrees(), closeTo(-180.0, TOLERANCE));

        Course zeroRadians = Course.ofRadians(-0.0);
        assertThat(zeroRadians.inDegrees(), closeTo(-0.0, TOLERANCE));
    }

    @Test
    public void testNegate() {
        Course ninetyDegrees = Course.ofDegrees(90.0);
        Course negativeNinetyDegrees = Course.ofDegrees(-90.0);

        assertThat(ninetyDegrees.negate(), is(negativeNinetyDegrees));
        assertThat(negativeNinetyDegrees.negate(), is(ninetyDegrees));
    }

    @Test
    public void testAbs() {
        Course ninetyDegrees = Course.ofDegrees(90.0);
        Course negativeNinetyDegrees = Course.ofDegrees(-90.0);

        assertThat(ninetyDegrees.abs(), is(ninetyDegrees));
        assertThat(negativeNinetyDegrees.abs(), is(ninetyDegrees));
    }

    @Test
    public void testToString_int() {
        assertEquals("90deg", Course.ofDegrees(90.0).toString(0));
        assertEquals("3rad", Course.ofRadians(PI).toString(0));
    }

    @Test
    public void testToString() {
        assertEquals("90deg", Course.ofDegrees(90.0).toString());
        assertEquals("3.14159rad", Course.ofRadians(PI).toString());
    }

    @Test
    public void testIsPositive() {
        assertThat(Course.ofDegrees(20).isPositive(), is(true));
        assertThat(Course.ofDegrees(-20).isPositive(), is(false));
        assertThat(Course.ofDegrees(0).isPositive(), is(false));

        assertThat(Course.ofRadians(1).isPositive(), is(true));
        assertThat(Course.ofRadians(-1).isPositive(), is(false));
        assertThat(Course.ofRadians(0).isPositive(), is(false));
    }

    @Test
    public void testIsNegative() {
        assertThat(Course.ofDegrees(20).isNegative(), is(false));
        assertThat(Course.ofDegrees(-20).isNegative(), is(true));
        assertThat(Course.ofDegrees(0).isNegative(), is(false));

        assertThat(Course.ofRadians(1).isNegative(), is(false));
        assertThat(Course.ofRadians(-1).isNegative(), is(true));
        assertThat(Course.ofRadians(0).isNegative(), is(false));
    }

    @Test
    public void testTimes() {
        assertThat(Course.ofDegrees(20).times(5), is(Course.ofDegrees(100)));
        assertThat(Course.ofDegrees(20).times(-5), is(Course.ofDegrees(-100)));
        assertThat(Course.ofDegrees(20).times(0.5), is(Course.ofDegrees(10)));
        assertThat(Course.ofDegrees(20).times(0), is(Course.ofDegrees(0)));
    }

    @Test
    public void testDividedBy() {
        Course piRadians = Course.ofRadians(PI);
        Course twoPiRadians = Course.ofRadians(2 * PI);
        Course negativeNinetyDegrees = Course.ofDegrees(-90.0);

        assertThat(piRadians.dividedBy(piRadians), is(1.0));
        assertThat(piRadians.dividedBy(twoPiRadians), is(0.5));
        assertThat(twoPiRadians.dividedBy(piRadians), is(2.0));
        assertThat(negativeNinetyDegrees.dividedBy(piRadians), is(-0.50));
        assertThat(piRadians.dividedBy(negativeNinetyDegrees), is(-2.0));
    }

    @Test
    public void testComparisionMethods() {
        Course zeroRadians = Course.ofRadians(0);
        Course zeroDegrees = Course.ofDegrees(0);

        assertThat(zeroRadians.isGreaterThanOrEqualTo(zeroDegrees), is(true));
        assertThat(zeroDegrees.isGreaterThanOrEqualTo(zeroRadians), is(true));
        assertThat(zeroRadians.isGreaterThan(zeroDegrees), is(false));
        assertThat(zeroDegrees.isGreaterThan(zeroRadians), is(false));

        assertThat(zeroRadians.isLessThanOrEqualTo(zeroDegrees), is(true));
        assertThat(zeroDegrees.isLessThanOrEqualTo(zeroRadians), is(true));
        assertThat(zeroRadians.isLessThan(zeroDegrees), is(false));
        assertThat(zeroDegrees.isLessThan(zeroRadians), is(false));

        Course oneDegree = Course.ofDegrees(1);

        assertThat(oneDegree.isGreaterThanOrEqualTo(zeroDegrees), is(true));
        assertThat(oneDegree.isGreaterThan(zeroDegrees), is(true));
        assertThat(oneDegree.isGreaterThanOrEqualTo(zeroDegrees), is(true));
        assertThat(oneDegree.isLessThanOrEqualTo(zeroDegrees), is(false));
        assertThat(oneDegree.isLessThan(zeroDegrees), is(false));
    }

    @Test
    public void testPlus() {
        Course oneRadian = Course.ofRadians(1);
        Course ninetyDegrees = Course.ofDegrees(90);

        // native unit is taken from first argument
        assertThat(oneRadian.plus(ninetyDegrees).nativeUnit(), is(RADIANS));
        assertThat(ninetyDegrees.plus(oneRadian).nativeUnit(), is(DEGREES));

        // result is the same regardless of order
        assertThat(oneRadian.plus(ninetyDegrees).inRadians(), is(1.0 + PI / 2.0));
        assertThat(ninetyDegrees.plus(oneRadian).inRadians(), is(1.0 + PI / 2.0));
    }

    @Test
    public void testMinus() {
        Course oneRadian = Course.ofRadians(1);
        Course ninetyDegrees = Course.ofDegrees(90);

        // native unit is taken from first argument
        assertThat(oneRadian.minus(ninetyDegrees).nativeUnit(), is(RADIANS));
        assertThat(ninetyDegrees.minus(oneRadian).nativeUnit(), is(DEGREES));

        // order is reflected
        assertThat(oneRadian.minus(ninetyDegrees).inRadians(), is(1.0 - PI / 2.0));
        assertThat(ninetyDegrees.minus(oneRadian).inRadians(), is(PI / 2.0 - 1.0));
    }

    @Test
    public void testCompareTo() {
        // test via List sorting...

        Course negativeOneRadian = Course.ofRadians(-1.0);
        Course tenDegrees = Course.ofDegrees(10.0);
        Course twentyDegrees = Course.ofDegrees(20.0);
        Course twohundredDegrees = Course.ofDegrees(200.0);
        Course oneRadian = Course.ofRadians(1.0);
        Course twoPiRadians = Course.ofRadians(2 * PI);

        Course[] courses =
                new Course[] {oneRadian, twentyDegrees, tenDegrees, twoPiRadians, negativeOneRadian, twohundredDegrees};

        Arrays.sort(courses);

        assertThat(courses[0], is(negativeOneRadian));
        assertThat(courses[1], is(tenDegrees));
        assertThat(courses[2], is(twentyDegrees));
        assertThat(courses[3], is(oneRadian));
        assertThat(courses[4], is(twohundredDegrees));
        assertThat(courses[5], is(twoPiRadians));
    }

    @Test
    public void testAngleDifference() {
        assertThat(angleBetween(Course.ofDegrees(5.0), Course.ofDegrees(355.0)), is(Course.ofDegrees(10)));

        assertThat(angleBetween(Course.ofDegrees(355.0), Course.ofDegrees(5.0)), is(Course.ofDegrees(-10)));
    }

    @Test
    public void testHashcode() {
        Course negativeOneRadian = Course.ofRadians(-1.0);
        Course tenDegrees = Course.ofDegrees(10.0);
        Course twentyDegrees = Course.ofDegrees(20.0);
        Course twohundredDegrees = Course.ofDegrees(200.0);
        Course oneRadian = Course.ofRadians(1.0);
        Course twoPiRadians = Course.ofRadians(2 * PI);

        Set<Integer> hashes = new HashSet<>();
        hashes.add(negativeOneRadian.hashCode());
        hashes.add(tenDegrees.hashCode());
        hashes.add(twentyDegrees.hashCode());
        hashes.add(twohundredDegrees.hashCode());
        hashes.add(oneRadian.hashCode());
        hashes.add(twoPiRadians.hashCode());

        assertThat(hashes, hasSize(6));
    }

    @Test
    public void testEquals() {
        Course zeroDegrees = Course.ofDegrees(0);
        Course oneDegree = Course.ofDegrees(1);

        assertThat(zeroDegrees.equals(oneDegree), is(false));
        assertThat(oneDegree.equals(zeroDegrees), is(false));
        assertThat(oneDegree.equals(null), is(false));
        assertThat(oneDegree.equals("not a course"), is(false));
    }

    @Test
    public void equalsReflectsTheUnit() {

        Course zeroDegrees = Course.ofDegrees(0);
        Course zeroRadians = Course.ofRadians(0);

        // both ARE ZERO
        assertThat(zeroDegrees.isZero(), is(true));
        assertThat(zeroRadians.isZero(), is(true));

        // they are not equal
        assertThat(zeroRadians.equals(zeroDegrees), is(false));
    }

    @Test
    public void testSin() {
        assertThat(Course.ofDegrees(-90).sin(), is(-1.0));
        assertThat(Course.ofDegrees(-45).sin(), closeTo(-sqrt(2.0) / 2.0, TOLERANCE));
        assertThat(Course.ofDegrees(0).sin(), is(0.0));
        assertThat(Course.ofDegrees(45).sin(), closeTo(sqrt(2.0) / 2.0, TOLERANCE));
        assertThat(Course.ofDegrees(90).sin(), is(1.0));
    }

    @Test
    public void testCos() {
        assertThat(Course.ofDegrees(-90).cos(), closeTo(0.0, TOLERANCE));
        assertThat(Course.ofDegrees(-45).cos(), closeTo(sqrt(2.0) / 2.0, TOLERANCE));
        assertThat(Course.ofDegrees(0).cos(), is(1.0));
        assertThat(Course.ofDegrees(45).cos(), closeTo(sqrt(2.0) / 2.0, TOLERANCE));
        assertThat(Course.ofDegrees(90).cos(), closeTo(0.0, TOLERANCE));
        assertThat(Course.ofDegrees(135).cos(), closeTo(-sqrt(2.0) / 2.0, TOLERANCE));
        assertThat(Course.ofDegrees(180).cos(), is(-1.0));
    }

    @Test
    public void testTan() {
        assertThat(Course.ofDegrees(-45).tan(), closeTo(-1.0, TOLERANCE));
        assertThat(Course.ofDegrees(0).tan(), is(0.0));
        assertThat(Course.ofDegrees(45).tan(), closeTo(1.0, TOLERANCE));
        assertThat(Course.ofDegrees(90).tan(), greaterThan(2E14)); // HUGE number...but won't be INFINITY
        assertThat(Course.ofDegrees(135).tan(), closeTo(-1, TOLERANCE));
    }

    @Test
    public void testZeroConstant() {

        assertTrue(Course.ZERO.isZero());
        assertThat(Course.ZERO.inDegrees(), closeTo(Course.of(0.0, DEGREES).inDegrees(), 1E-10));
    }

    //	/**
    //	 * Test of between method, of class Course.
    //	 */
    //	@Test
    //	public void testBetween() {
    //		System.out.println("between");
    //		Course one = null;
    //		Course two = null;
    //		Course expResult = null;
    //		Course result = Course.between(one, two);
    //		assertEquals(expResult, result);
    //		// TODO review the generated test code and remove the default call to fail.
    //		fail("The test case is a prototype.");
    //	}

    //
    //	/**
    //	 * Test of isZero method, of class Course.
    //	 */
    //	@Test
    //	public void testIsZero() {
    //		System.out.println("isZero");
    //		Course instance = null;
    //		boolean expResult = false;
    //		boolean result = instance.isZero();
    //		assertEquals(expResult, result);
    //		// TODO review the generated test code and remove the default call to fail.
    //		fail("The test case is a prototype.");
    //	}
    //
    //
    //	/**
    //	 * Test of angleDifference method, of class Course.
    //	 */
    //	@Test
    //	public void testAngleDifference_Double_Double() {
    //		System.out.println("angleDifference");
    //		Double hdg = null;
    //		Double hdg0 = null;
    //		Double expResult = null;
    //		Double result = Course.angleDifference(hdg, hdg0);
    //		assertEquals(expResult, result);
    //		// TODO review the generated test code and remove the default call to fail.
    //		fail("The test case is a prototype.");
    //	}
    //
    //	/**
    //	 * Test of angleDifference method, of class Course.
    //	 */
    //	@Test
    //	public void testAngleDifference_Double() {
    //		System.out.println("angleDifference");
    //		Double dz = null;
    //		Double expResult = null;
    //		Double result = Course.angleDifference(dz);
    //		assertEquals(expResult, result);
    //		// TODO review the generated test code and remove the default call to fail.
    //		fail("The test case is a prototype.");
    //	}

}
