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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.Distance.*;
import static org.mitre.caasd.commons.Distance.Unit.*;
import static org.mitre.caasd.commons.Speed.Unit.MILES_PER_HOUR;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DistanceTest {

    @Test
    public void testConstructor() {
        Distance dist = new Distance(1.0, NAUTICAL_MILES);

        double TOL = 0.00001;
        assertEquals(1.0, dist.inNauticalMiles(), TOL);
        assertEquals(1852.0, dist.inMeters(), 1852.0 * TOL);
        assertEquals(6076.12, dist.inFeet(), 6076.12 * TOL);
        assertEquals(1.15078, dist.inMiles(), 1.15078 * TOL);
    }

    @Test
    public void testOf() {

        Distance dist = Distance.of(1.0, METERS);

        double TOL = 0.00001;
        assertEquals(0.000539956803456, dist.inNauticalMiles(), 0.000539956803456 * TOL);
        assertEquals(1.0, dist.inMeters(), TOL);
        assertEquals(3.28084, dist.inFeet(), 3.28084 * TOL);
    }

    @Test
    public void testTheConverterMethod_in() {

        double TOL = 0.00001;

        Distance oneNm = Distance.ofNauticalMiles(1.0);
        assertEquals(1.0, oneNm.in(NAUTICAL_MILES), TOL);
        assertEquals(1852.0, oneNm.in(METERS), 1852.0 * TOL);
        assertEquals(6076.12, oneNm.in(FEET), 6076.12 * TOL);
        assertEquals(1.15078, oneNm.in(MILES), 1.15078 * TOL);

        Distance oneMeter = Distance.ofMeters(1.0);
        assertEquals(0.000539956803456, oneMeter.in(NAUTICAL_MILES), 0.000539956803456 * TOL);
        assertEquals(1.0, oneMeter.in(METERS), TOL);
        assertEquals(0.001, oneMeter.inKilometers(), TOL);
        assertEquals(3.28084, oneMeter.in(FEET), 3.28084 * TOL);
        assertEquals(0.000621371, oneMeter.in(MILES), 0.000621371 * TOL);
    }

    @Test
    public void testComparisonMethods() {

        Distance halfMeter = Distance.of(0.5, METERS);
        Distance oneMeter = Distance.of(1.0, METERS);
        Distance oneThousandMeters = Distance.of(1_000, METERS);
        Distance oneKiloMeter = Distance.of(1, KILOMETERS);

        assertTrue(halfMeter.isLessThan(oneMeter));
        assertTrue(halfMeter.isLessThanOrEqualTo(oneMeter));
        assertTrue(oneMeter.isGreaterThan(halfMeter));
        assertTrue(oneMeter.isGreaterThanOrEqualTo(halfMeter));

        assertTrue(oneKiloMeter.isGreaterThanOrEqualTo(oneThousandMeters));
        assertTrue(oneKiloMeter.isLessThanOrEqualTo(oneThousandMeters));
    }

    @Test
    public void testMiles() {
        Distance oneMile = Distance.ofMiles(1.0);
        assertEquals(1.0, oneMile.inMiles(), 0.00001);
        assertEquals(5_280.0, oneMile.inFeet(), 5_280.0 * 0.00001);
    }

    @Test
    public void testAbs() {
        Distance oneMeter = Distance.of(1, METERS);
        Distance negativeMeter = Distance.of(-1, METERS);

        assertTrue(negativeMeter.abs().equals(oneMeter));
        assertTrue(oneMeter.abs().equals(Distance.of(1.0, METERS)));
    }

    @Test
    public void testNegate() {
        Distance oneMeter = Distance.of(1, METERS);
        Distance negativeMeter = Distance.of(-1, METERS);

        assertTrue(negativeMeter.equals(oneMeter.negate()));
    }

    @Test
    public void testTimes() {
        Distance oneMeter = Distance.of(1, METERS);
        Distance halfMeter = oneMeter.times(0.5);

        double TOL = 0.00001;

        assertEquals(0.5, halfMeter.inMeters(), TOL);
    }

    @Test
    public void testPlus() {
        Distance oneFoot = Distance.ofFeet(1);
        Distance fiveHalvesFeet = Distance.of(2.5, FEET);

        Distance sum = oneFoot.plus(fiveHalvesFeet);

        double TOL = 0.00001;

        assertEquals(3.5, sum.inFeet(), TOL);
    }

    @Test
    public void testMinus() {
        Distance oneFoot = Distance.of(1, FEET);
        Distance fiveHalvesFeet = Distance.of(2.5, FEET);

        Distance sum = oneFoot.minus(fiveHalvesFeet);

        double TOL = 0.00001;

        assertEquals(-1.5, sum.inFeet(), TOL);
        assertTrue(sum.isNegative());
    }

    @Test
    public void testEquals() {

        Distance a = Distance.of(5, FEET);
        Distance b = Distance.of(5, FEET);
        Distance c = Distance.of(12, FEET); // same unit, different amount
        Distance d = Distance.of(5, NAUTICAL_MILES); // same amount, different unit
        Integer i = 12;

        assertTrue(a.equals(b));
        assertTrue(a.hashCode() == b.hashCode());
        assertTrue(a != b);

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertFalse(a.equals(null));
        assertFalse(a.equals(i));

        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
    }

    @Test
    public void testCompareTo() {
        Distance oneMeter = Distance.of(1.0, METERS);
        Distance zero = Distance.of(0, NAUTICAL_MILES);
        Distance negative1Feet = Distance.of(-1, FEET);
        Distance oneFoot = Distance.of(1.0, FEET);
        Distance oneNm = Distance.of(1.0, NAUTICAL_MILES);
        Distance fourFeet = Distance.of(4.0, FEET);
        Distance oneKm = Distance.ofKiloMeters(1.0);
        Distance fiveFeet = Distance.of(5.0, FEET);

        List<Distance> distances =
                newArrayList(oneMeter, zero, oneKm, negative1Feet, oneFoot, oneNm, fourFeet, fiveFeet);

        Collections.sort(distances);
        assertEquals(distances.get(0), negative1Feet);
        assertEquals(distances.get(1), zero);
        assertEquals(distances.get(2), oneFoot);
        assertEquals(distances.get(3), oneMeter);
        assertEquals(distances.get(4), fourFeet);
        assertEquals(distances.get(5), fiveFeet);
        assertEquals(distances.get(6), oneKm);
        assertEquals(distances.get(7), oneNm);
    }

    @Test
    public void testToStringWithCustomLength() {
        // confirm the number of digits is reflected
        assertEquals("1m", Distance.ofMeters(1).toString(0));
        assertEquals("1.0m", Distance.ofMeters(1).toString(1));
        assertEquals("1.00m", Distance.ofMeters(1).toString(2));

        assertEquals("1.00m", Distance.ofMeters(1).toString(2));
        assertEquals("1.00km", Distance.ofKiloMeters(1).toString(2));
        assertEquals("1.00NM", Distance.ofNauticalMiles(1).toString(2));
        assertEquals("1.00ft", Distance.ofFeet(1).toString(2));
    }

    @Test
    public void testToString() {
        assertEquals("1.00m", Distance.ofMeters(1).toString());
        assertEquals("1.00000km", Distance.ofKiloMeters(1).toString());
        assertEquals("1.00ft", Distance.ofFeet(1).toString());
        assertEquals("1.00000NM", Distance.ofNauticalMiles(1).toString());
    }

    @Test
    public void verifySerializability() {
        TestUtils.verifySerializability(Distance.of(12, FEET));
    }

    @Test
    public void testStaticFactory_DistanceBetween() {

        LatLong one = LatLong.of(1.0, 1.0);
        LatLong two = LatLong.of(2.0, 2.0);
        LatLong three = LatLong.of(1.0, 2.0);

        assertEquals(Distance.between(one, one), Distance.ofNauticalMiles(0));
        assertEquals(Distance.between(two, two), Distance.ofNauticalMiles(0));
        assertEquals(Distance.between(three, three), Distance.ofNauticalMiles(0));

        assertEquals(Distance.between(one, two), one.distanceTo(two));
        assertEquals(Distance.between(two, one), one.distanceTo(two));

        assertEquals(Distance.between(one, three), one.distanceTo(three));
        assertEquals(Distance.between(three, one), one.distanceTo(three));

        assertEquals(Distance.between(two, three), two.distanceTo(three));
        assertEquals(Distance.between(three, two), two.distanceTo(three));
    }

    @Test
    public void testDividedBy_distance() {

        Distance a = Distance.ofFeet(12);
        Distance b = Distance.ofFeet(24);

        double TOLERANCE = 0.0000001;
        assertEquals(a.dividedBy(b), 0.5, TOLERANCE);
        assertEquals(b.dividedBy(a), 2.0, TOLERANCE);
    }

    @Test
    public void testDividedBy_distance_differentUnits() {

        Distance a = Distance.ofFeet(12);
        Distance b = Distance.ofMeters(24);

        double TOLERANCE = 0.00001;
        assertEquals(a.dividedBy(b), 0.5 * (1.0 / 3.28084), TOLERANCE);
        assertEquals(b.dividedBy(a), 2.0 * 3.28084, TOLERANCE);
    }

    @Test
    public void testIsPositive() {
        Distance negativeOne = Distance.ofFeet(-1.0);
        assertFalse(negativeOne.isPositive());
        assertTrue(negativeOne.negate().isPositive());

        Distance zero = Distance.ofFeet(0.0);
        assertFalse(zero.isPositive());
        assertFalse(zero.negate().isPositive());

        Distance one = Distance.ofFeet(1.0);
        assertTrue(one.isPositive());
        assertFalse(one.negate().isPositive());
    }

    @Test
    public void testIsNegative() {
        Distance negativeOne = Distance.ofFeet(-1.0);
        assertTrue(negativeOne.isNegative());
        assertFalse(negativeOne.negate().isNegative());

        Distance zero = Distance.ofFeet(0.0);
        assertFalse(zero.isNegative());
        assertFalse(zero.negate().isNegative());

        Distance one = Distance.ofFeet(1.0);
        assertFalse(one.isNegative());
        assertTrue(one.negate().isNegative());
    }

    @Test
    public void testIsZero() {
        Distance negativeOne = Distance.ofFeet(-1.0);
        assertFalse(negativeOne.isZero());
        assertFalse(negativeOne.negate().isZero());

        Distance zero = Distance.ofFeet(0.0);
        assertTrue(zero.isZero());
        assertTrue(zero.negate().isZero());

        Distance one = Distance.ofFeet(1.0);
        assertFalse(one.isZero());
        assertFalse(one.negate().isZero());
    }

    @Test
    public void noNullInputToMean() {
        Distance[] distances = null;

        assertThrows(NullPointerException.class, () -> mean(distances));
    }

    @Test
    public void meanRequiresNonEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> mean(newArrayList()));
    }

    @Test
    public void meanWorksWhenAllUnitsAreTheSame() {
        Distance average = mean(Distance.ofFeet(22), Distance.ofFeet(12));

        assertThat(average, is(Distance.ofFeet(17)));
    }

    @Test
    public void meanWorkWhenUnitsAreDifferent() {
        Distance average = mean(
                Distance.ofFeet(22), Distance.ofFeet(12), Distance.ofMeters(1) // 3.28084 ft
                );

        assertThat(average.nativeUnit(), is(Distance.Unit.FEET));
        assertThat(average.inFeet(), closeTo(12.4269, 0.0001));
    }

    @Test
    public void sumDoesNotAcceptNullInput() {
        Distance[] distances = null;

        assertThrows(NullPointerException.class, () -> sum(distances));
    }

    @Test
    public void sumAllowsEmptyCollection() {
        Distance result = sum(newArrayList());
        assertThat(result.inMeters(), is(0.0));
    }

    @Test
    public void sumWorksWhenAllUnitsAreTheSame() {
        Distance sum = sum(Distance.ofFeet(22), Distance.ofFeet(12));

        assertThat(sum, is(Distance.ofFeet(34)));
    }

    @Test
    public void sumWorkWhenUnitsAreDifferent() {
        Distance average = sum(
                Distance.ofFeet(22), Distance.ofFeet(12), Distance.ofMeters(1) // 3.28084 ft
                );

        assertThat(average.nativeUnit(), is(Distance.Unit.FEET));
        assertThat(average.inFeet(), closeTo(37.28084, 0.0001));
    }

    @Test
    public void creatingSpeedByDividingByTime() {

        assertThat(Distance.ofMiles(1.0).dividedBy(Duration.ofHours(2)), is(Speed.of(0.5, MILES_PER_HOUR)));
    }

    @Test
    public void unitFromStringProperlyParsesUnits() {

        assertThat(unitFromString("ft"), is(Distance.Unit.FEET));
        assertThat(unitFromString("5.0ft"), is(Distance.Unit.FEET));

        assertThat(unitFromString("km"), is(KILOMETERS));
        assertThat(unitFromString("5.0km"), is(KILOMETERS));

        assertThat(unitFromString("m"), is(METERS));
        assertThat(unitFromString("5.0m"), is(METERS));

        assertThat(unitFromString("mi"), is(Distance.Unit.MILES));
        assertThat(unitFromString("5.0mi"), is(Distance.Unit.MILES));

        assertThat(unitFromString("NM"), is(NAUTICAL_MILES));
        assertThat(unitFromString("5.0NM"), is(NAUTICAL_MILES));

        assertThat(unitFromString(""), nullValue());
        assertThat(unitFromString("notAUnit"), nullValue());
    }

    @Test
    public void distanceFromString_noUnitFound() {
        assertThrows(IllegalArgumentException.class, () -> Distance.fromString("notAUnit"));
    }

    @Test
    public void distanceFromString_nullInput() {
        assertThrows(NullPointerException.class, () -> Distance.fromString(null));
    }

    @Test
    public void distanceFromString() {

        // can parse when there is no space
        assertThat(Distance.fromString("5.0ft"), is(Distance.of(5, FEET)));
        assertThat(Distance.fromString("5.0km"), is(Distance.of(5, KILOMETERS)));
        assertThat(Distance.fromString("5.0m"), is(Distance.of(5, METERS)));
        assertThat(Distance.fromString("5.0mi"), is(Distance.of(5, MILES)));
        assertThat(Distance.fromString("5.0NM"), is(Distance.of(5, NAUTICAL_MILES)));

        // can parse when there is a space
        assertThat(Distance.fromString("5.0 ft"), is(Distance.of(5, FEET)));
        assertThat(Distance.fromString("5.0 km"), is(Distance.of(5, KILOMETERS)));
        assertThat(Distance.fromString("5.0 m"), is(Distance.of(5, METERS)));
        assertThat(Distance.fromString("5.0 mi"), is(Distance.of(5, MILES)));
        assertThat(Distance.fromString("5.0 NM"), is(Distance.of(5, NAUTICAL_MILES)));

        // can parse different levels of accuracy
        assertThat(Distance.fromString("5 ft"), is(Distance.of(5, FEET)));
        assertThat(Distance.fromString("5.01 km"), is(Distance.of(5.01, KILOMETERS)));
        assertThat(Distance.fromString("5.12345 m"), is(Distance.of(5.12345, METERS)));
        assertThat(Distance.fromString("5E5 mi"), is(Distance.of(5E5, MILES)));
        assertThat(Distance.fromString("5.000001 NM"), is(Distance.of(5.000001, NAUTICAL_MILES)));

        // can parse zero and negative numbers
        assertThat(Distance.fromString("-5 ft"), is(Distance.of(-5, FEET)));
        assertThat(Distance.fromString("-5.01 km"), is(Distance.of(-5.01, KILOMETERS)));
        assertThat(Distance.fromString("-5.12345 m"), is(Distance.of(-5.12345, METERS)));
        assertThat(Distance.fromString("-5E5 mi"), is(Distance.of(-5E5, MILES)));
        assertThat(Distance.fromString("0 NM"), is(Distance.of(0, NAUTICAL_MILES)));
    }

    @Test
    public void min_returnsFirstArgumentWhenTied() {

        Distance oneHundred = Distance.ofFeet(100);
        Distance ondHundred_v2 = Distance.ofFeet(100);

        assertThat("These are not the same object, this should fail", oneHundred == ondHundred_v2, is(false));
        assertThat("But these objects are equal", oneHundred.equals(ondHundred_v2), is(true));

        assertThat(min(oneHundred, ondHundred_v2), is(oneHundred));
    }

    @Test
    public void min_returnsTheMin() {

        Distance one = Distance.ofFeet(100);
        Distance two = Distance.ofFeet(200);

        assertThat(min(one, two), is(one));
        assertThat(min(two, one), is(one));
    }

    @Test
    public void min_minOfEmptyArrayIsNull() {
        assertThat(min(new Distance[] {}), nullValue());
    }

    @Test
    public void min_minOfSingletonArrayIsFound() {
        assertThat(min(new Distance[] {Distance.ofFeet(100)}), is(Distance.ofFeet(100)));
    }

    @Test
    public void min_minOfLongArrayIsFound() {
        Distance[] testData_1 =
                new Distance[] {Distance.ofFeet(1000.0), Distance.ofMeters(1), Distance.ofNauticalMiles(1.0)};
        Distance[] testData_2 = new Distance[] {
            Distance.ofMeters(1), Distance.ofNauticalMiles(1.0), Distance.ofFeet(1000.0),
        };

        assertThat(min(testData_1), is(Distance.ofMeters(1)));
        assertThat(min(testData_2), is(Distance.ofMeters(1)));
    }

    @Test
    public void max_returnsFirstArgumentWhenTied() {

        Distance oneHundred = Distance.ofFeet(100);
        Distance ondHundred_v2 = Distance.ofFeet(100);

        assertThat("These are not the same object, this should fail", oneHundred == ondHundred_v2, is(false));
        assertThat("But these objects are equal", oneHundred.equals(ondHundred_v2), is(true));

        assertThat(max(oneHundred, ondHundred_v2), is(oneHundred));
    }

    @Test
    public void max_returnsTheMax() {

        Distance one = Distance.ofFeet(100);
        Distance two = Distance.ofFeet(200);

        assertThat(max(one, two), is(two));
        assertThat(max(two, one), is(two));
    }

    @Test
    public void max_maxOfEmptyArrayIsNull() {
        assertThat(max(new Distance[] {}), nullValue());
    }

    @Test
    public void max_maxOfSingletonArrayIsFound() {
        assertThat(max(new Distance[] {Distance.ofFeet(100)}), is(Distance.ofFeet(100)));
    }

    @Test
    public void max_maxOfLongArrayIsFound() {
        Distance[] testData_1 =
                new Distance[] {Distance.ofFeet(1000.0), Distance.ofMeters(1), Distance.ofNauticalMiles(1.0)};
        Distance[] testData_2 = new Distance[] {
            Distance.ofMeters(1), Distance.ofNauticalMiles(1.0), Distance.ofFeet(1000.0),
        };

        assertThat(max(testData_1), is(Distance.ofNauticalMiles(1)));
        assertThat(max(testData_2), is(Distance.ofNauticalMiles(1)));
    }

    @Test
    public void testZeroConstant() {

        assertTrue(Distance.ZERO.isZero());
        assertThat(Distance.ZERO.inFeet(), closeTo(Distance.of(0.0, FEET).inFeet(), 1E-10));
    }
}
