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

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Distance.Unit.METERS;
import static org.mitre.caasd.commons.Distance.Unit.MILES;
import static org.mitre.caasd.commons.Speed.Unit.FEET_PER_MINUTE;
import static org.mitre.caasd.commons.Speed.Unit.FEET_PER_SECOND;
import static org.mitre.caasd.commons.Speed.Unit.KILOMETERS_PER_HOUR;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;
import static org.mitre.caasd.commons.Speed.Unit.METERS_PER_SECOND;
import static org.mitre.caasd.commons.Speed.Unit.MILES_PER_HOUR;
import static org.mitre.caasd.commons.Speed.unitFromString;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class SpeedTest {

    @Test
    public void testSpeedZeroTimeDelta() {
        //timeDelta must be positive
        assertThrows(IllegalArgumentException.class,
            () -> new Speed(
                Distance.ofNauticalMiles(1),
                Duration.ZERO
            )
        );
    }

    @Test
    public void testSpeedNegativeTimeDelta() {
        //timeDelta must be positive
        assertThrows(IllegalArgumentException.class,
            () -> new Speed(
                Distance.ofNauticalMiles(1),
                Duration.ofMinutes(1).negated()
            )
        );
    }

    @Test
    public void testInKnots() {
        Speed speed = new Speed(
            Distance.ofNauticalMiles(1),
            Duration.ofHours(1)
        );

        assertEquals(1.0, speed.inKnots(), 0.00001);
    }

    @Test
    public void testInKnots_2() {
        Speed speed = new Speed(
            Distance.ofNauticalMiles(20),
            Duration.ofHours(1)
        );

        assertEquals(20.0, speed.inKnots(), 0.00001);
    }

    @Test
    public void testInKnots_3() {
        Speed speed = new Speed(
            Distance.ofNauticalMiles(20),
            Duration.ofMinutes(10)
        );

        assertEquals(120.0, speed.inKnots(), 0.00001);
    }

    @Test
    public void testConversion() {
        //20 knots
        Speed speed = new Speed(
            Distance.ofNauticalMiles(20),
            Duration.ofHours(1)
        );

        double TOL = 0.00001;
        assertEquals(20.0, speed.inKnots(), 20.0 * TOL);
        assertEquals(37.04, speed.inKilometersPerHour(), 37.04 * TOL);
        assertEquals(10.2889, speed.inMetersPerSecond(), 10.2889 * TOL);
        assertEquals(23.0156, speed.inMilesPerHour(), 23.0156 * TOL);
    }

    @Test
    public void testLiterateConstruction() {
        Speed oneKnot = Speed.of(1, KNOTS);

        double TOL = 0.00001;
        assertEquals(1.0, oneKnot.inKnots(), TOL);
        assertEquals(1.15078, oneKnot.inMilesPerHour(), 1.15078 * TOL);
    }

    @Test
    public void testBothConstructionMethodsAgree() {
        Speed oneKnot = Speed.of(1, KNOTS);
        Speed oneKnotByDef = new Speed(Distance.ofNauticalMiles(1.0), Duration.ofHours(1));

        double TOL = 0.00001;
        assertEquals(oneKnot.inKnots(), oneKnotByDef.inKnots(), TOL);
        assertEquals(oneKnot.inMetersPerSecond(), oneKnotByDef.inMetersPerSecond(), TOL);
    }

    @Test
    public void verifySerializability() {
        TestUtils.verifySerializability(Speed.of(1, KNOTS));
    }

    @Test
    public void testBetween() {
        LatLong pos1 = LatLong.of(0.0, 0.0);
        Instant time1 = EPOCH;

        LatLong pos2 = LatLong.of(1.0, 1.0);
        Instant time2 = EPOCH.plus(Duration.ofHours(1));

        Distance dist = pos1.distanceTo(pos2);
        Duration timeDelta = Duration.ofHours(1);

        Speed manuallyBuiltSpeed = new Speed(dist, timeDelta);
        Speed autoSpeed = Speed.between(pos1, time1, pos2, time2);

        double TOL = 0.0001;
        assertEquals(manuallyBuiltSpeed.inKnots(), autoSpeed.inKnots(), TOL);
    }

    @Test
    public void equalsReducesUnits() {
        Speed oneMeterPerSecond = new Speed(Distance.of(1, METERS), Duration.ofSeconds(1));
        Speed oneMeterPerSecond2 = new Speed(Distance.of(20, METERS), Duration.ofSeconds(20));
        Speed oneMilePerSecond = new Speed(Distance.of(1, MILES), Duration.ofSeconds(1));

        assertEquals(oneMeterPerSecond, oneMeterPerSecond2);
        assertEquals(oneMeterPerSecond.hashCode(), oneMeterPerSecond2.hashCode());

        assertEquals(oneMeterPerSecond, oneMeterPerSecond);
        assertNotEquals(oneMeterPerSecond, "hello");
        assertNotEquals(oneMeterPerSecond, oneMilePerSecond);
        assertFalse(oneMilePerSecond.equals(null));
    }

    @Test
    public void testTimes_Distance() {

        Speed oneKnot = Speed.of(1.0, KNOTS);

        assertThat(
            oneKnot.times(Duration.ZERO),
            is(Distance.ofNauticalMiles(0))
        );

        assertThat(
            oneKnot.times(Duration.ofMinutes(30)),
            is(Distance.ofNauticalMiles(0.5))
        );
        assertThat(
            oneKnot.times(Duration.ofMinutes(60)),
            is(Distance.ofNauticalMiles(1))
        );
    }

    @Test
    public void times_scalar() {

        Speed oneKnot = Speed.of(1.0, KNOTS);

        assertThat(oneKnot.times(-5), is(Speed.of(-5.0, KNOTS)));
        assertThat(oneKnot.times(0), is(Speed.of(0.0, KNOTS)));
        assertThat(oneKnot.times(11.1), is(Speed.of(11.1, KNOTS)));
    }

    @Test
    public void plusAnotherSpeed() {

        Speed oneKnot = Speed.of(1.0, KNOTS);
        Speed threeFps = Speed.of(3.0, FEET_PER_SECOND);
        Speed fiveMph = Speed.of(5.0, MILES_PER_HOUR);
        Speed sevenKph = Speed.of(7.0, KILOMETERS_PER_HOUR);
        Speed elevenMps = Speed.of(11.0, METERS_PER_SECOND);

        double TOLERANCE = 0.0005;

        //3fps = 1.7774514038876894 knots
        assertThat(oneKnot.plus(threeFps).inKnots(), closeTo(2.7774514038876896, TOLERANCE));
        assertThat(threeFps.plus(oneKnot).inKnots(), closeTo(2.7774514038876896, TOLERANCE));

        //3fps = 2.0454545454545454 mph
        assertThat(fiveMph.plus(threeFps).inMilesPerHour(), closeTo(7.045454545454546, TOLERANCE));
        assertThat(threeFps.plus(fiveMph).inMilesPerHour(), closeTo(7.045454545454546, TOLERANCE));

        //5pmh = 8.04672 kph
        assertThat(fiveMph.plus(sevenKph).inKilometersPerHour(), closeTo(15.046720000000002, TOLERANCE));
        assertThat(sevenKph.plus(fiveMph).inKilometersPerHour(), closeTo(15.046720000000002, TOLERANCE));

        //7kph = 1.9444444444444444 mps
        assertThat(elevenMps.plus(sevenKph).inMetersPerSecond(), closeTo(12.944444444444445, TOLERANCE));
        assertThat(sevenKph.plus(elevenMps).inMetersPerSecond(), closeTo(12.944444444444445, TOLERANCE));
    }

    @Test
    public void minusAnotherSpeed() {

        Speed oneKnot = Speed.of(1.0, KNOTS);
        Speed threeFps = Speed.of(3.0, FEET_PER_SECOND);
        Speed fiveMph = Speed.of(5.0, MILES_PER_HOUR);
        Speed sevenKph = Speed.of(7.0, KILOMETERS_PER_HOUR);
        Speed elevenMps = Speed.of(11.0, METERS_PER_SECOND);

        double TOLERANCE = 0.0005;

        //3fps = 1.7774514038876894 knots
        assertThat(oneKnot.minus(threeFps).inKnots(), closeTo(-0.777451403887689, TOLERANCE));
        assertThat(threeFps.minus(oneKnot).inKnots(), closeTo(0.777451403887689, TOLERANCE));

        //3fps = 2.0454545454545454 mph
        assertThat(fiveMph.minus(threeFps).inMilesPerHour(), closeTo(2.9545454545454546, TOLERANCE));
        assertThat(threeFps.minus(fiveMph).inMilesPerHour(), closeTo(-2.954545454545455, TOLERANCE));

        //5pmh = 8.04672 kph
        assertThat(fiveMph.minus(sevenKph).inKilometersPerHour(), closeTo(1.0467199999999997, TOLERANCE));
        assertThat(sevenKph.minus(fiveMph).inKilometersPerHour(), closeTo(-1.0467199999999997, TOLERANCE));

        //7kph = 1.9444444444444444 mps
        assertThat(elevenMps.minus(sevenKph).inMetersPerSecond(), closeTo(9.055555555555555, TOLERANCE));
        assertThat(sevenKph.minus(elevenMps).inMetersPerSecond(), closeTo(-9.055555555555555, TOLERANCE));
    }

    @Test
    public void testDistanceCoveredIn_negativeDuration() {
        Speed oneKnot = Speed.of(1.0, KNOTS);

        //negative duration is forbidden
        assertThrows(IllegalArgumentException.class,
            () -> oneKnot.times(Duration.ofHours(-1))
        );
    }

    @Test
    public void testTimeToTravel() {

        Speed oneKnot = Speed.of(1.0, KNOTS);

        assertThat(
            "It should take 1 hour to travel 1 NM at 1 knot",
            Duration.ofHours(1), is(oneKnot.timeToTravel(Distance.ofNauticalMiles(1)))
        );

        assertThat(
            "It should take 2 hours to travel 2 NM at 1 knot",
            Duration.ofHours(2), is(oneKnot.timeToTravel(Distance.ofNauticalMiles(2)))
        );

        assertThat(
            "It should take 30 minutes to go 1 NM at 2 knots",
            Speed.of(2.0, KNOTS).timeToTravel(Distance.ofNauticalMiles(1)), is(Duration.ofMinutes(30))
        );
    }

    @Test
    public void testFeetPerSecondUnit() {

        //20 feet per second
        Speed speed = new Speed(Distance.ofFeet(20), Duration.ofSeconds(1));

        Duration travelTime = speed.timeToTravel(Distance.ofFeet(20));

        assertThat(Duration.ofSeconds(1), is(travelTime));

        //20 fps = 6.096 mps = 20 ft * 0.3048 meters-per-foot
        assertEquals(6.096, speed.inMetersPerSecond(), 0.0000001);
        //convert ft to nm and seconds to hours
        assertEquals(20.0 / Spherical.feetPerNM() * 60.0 * 60.0, speed.inKnots(), 0.0000001);
    }

    @Test
    public void testIsPositive() {
        Speed negativeOne = Speed.of(-1, KNOTS);
        assertFalse(negativeOne.isPositive());

        Speed zero = Speed.of(0.0, KNOTS);
        assertFalse(zero.isPositive());

        Speed one = Speed.of(1.0, KNOTS);
        assertTrue(one.isPositive());
    }

    @Test
    public void testIsNegative() {
        Speed negativeOne = Speed.of(-1, KNOTS);
        assertTrue(negativeOne.isNegative());

        Speed zero = Speed.of(0.0, KNOTS);
        assertFalse(zero.isNegative());

        Speed one = Speed.of(1.0, KNOTS);
        assertFalse(one.isNegative());
    }

    @Test
    public void testIsZero() {
        Speed negativeOne = Speed.of(-1, KNOTS);
        assertFalse(negativeOne.isZero());

        Speed zero = Speed.of(0.0, KNOTS);
        assertTrue(zero.isZero());

        Speed one = Speed.of(1.0, KNOTS);
        assertFalse(one.isZero());
    }

    @Test
    public void testComparisonMethods() {

        Speed halfMeterPerSec = Speed.of(0.5, METERS_PER_SECOND);
        Speed oneMeterPerSec = Speed.of(1.0, METERS_PER_SECOND);
        Speed oneThousandMetersPerSec = Speed.of(1_000, METERS_PER_SECOND);
        Speed oneKiloMeterPerSec = Speed.of(3_600, KILOMETERS_PER_HOUR);

        assertTrue(halfMeterPerSec.isLessThan(oneMeterPerSec));
        assertTrue(halfMeterPerSec.isLessThanOrEqualTo(oneMeterPerSec));
        assertTrue(oneMeterPerSec.isGreaterThan(halfMeterPerSec));
        assertTrue(oneMeterPerSec.isGreaterThanOrEqualTo(halfMeterPerSec));

        assertTrue(oneKiloMeterPerSec.isGreaterThanOrEqualTo(oneThousandMetersPerSec));
        assertTrue(oneKiloMeterPerSec.isLessThanOrEqualTo(oneThousandMetersPerSec));
    }

    @Test
    public void feetPerMinuteUnitAddedCorrectly() {

        //same speed -- two ways
        Speed sixtyFpm = Speed.of(60, FEET_PER_MINUTE);
        Speed oneFps = Speed.of(1, FEET_PER_SECOND);

        assertThat(sixtyFpm, is(oneFps));

        Duration oneHour = Duration.ofHours(1);

        assertThat(
            "You should go the same distance because you are going the same speed",
            sixtyFpm.times(oneHour),
            is(oneFps.times(oneHour))
        );

        Distance oneNauticalMile = Distance.ofNauticalMiles(1);

        assertThat(
            "It should take the same amount of time to travel 1 NM",
            sixtyFpm.timeToTravel(oneNauticalMile),
            is(oneFps.timeToTravel(oneNauticalMile))
        );

        assertThat(sixtyFpm.inFeetPerMinutes(), is(60.0));
        assertThat(sixtyFpm.inFeetPerSecond(), is(1.0));
    }

    @Test
    public void unitFromStringProperlyParsesUnits() {

        assertThat(unitFromString("fpm"), is(Speed.Unit.FEET_PER_MINUTE));
        assertThat(unitFromString("5.0fpm"), is(Speed.Unit.FEET_PER_MINUTE));

        assertThat(unitFromString("fps"), is(Speed.Unit.FEET_PER_SECOND));
        assertThat(unitFromString("5.0fps"), is(Speed.Unit.FEET_PER_SECOND));

        assertThat(unitFromString("kph"), is(Speed.Unit.KILOMETERS_PER_HOUR));
        assertThat(unitFromString("5.0kph"), is(Speed.Unit.KILOMETERS_PER_HOUR));

        assertThat(unitFromString("kn"), is(Speed.Unit.KNOTS));
        assertThat(unitFromString("5.0kn"), is(Speed.Unit.KNOTS));

        assertThat(unitFromString("mps"), is(Speed.Unit.METERS_PER_SECOND));
        assertThat(unitFromString("5.0mps"), is(Speed.Unit.METERS_PER_SECOND));

        assertThat(unitFromString("mph"), is(Speed.Unit.MILES_PER_HOUR));
        assertThat(unitFromString("5.0mph"), is(Speed.Unit.MILES_PER_HOUR));

        assertThat(unitFromString(""), nullValue());
        assertThat(unitFromString("notAUnit"), nullValue());
    }

    @Test
    public void speedFromString_noUnitFound() {
        assertThrows(IllegalArgumentException.class,
            () -> Speed.fromString("notAUnit")
        );
    }

    @Test
    public void speedFromString_nullInput() {
        assertThrows(NullPointerException.class,
            () -> Speed.fromString(null)
        );
    }

    @Test
    public void speedFromString() {

        //can parse when there is no space
        assertThat(Speed.fromString("5.0fpm"), is(Speed.of(5, FEET_PER_MINUTE)));
        assertThat(Speed.fromString("5.fps"), is(Speed.of(5, FEET_PER_SECOND)));
        assertThat(Speed.fromString("5.0kph"), is(Speed.of(5, KILOMETERS_PER_HOUR)));
        assertThat(Speed.fromString("5.0kn"), is(Speed.of(5, KNOTS)));
        assertThat(Speed.fromString("5.0mps"), is(Speed.of(5, METERS_PER_SECOND)));
        assertThat(Speed.fromString("5.0mph"), is(Speed.of(5, MILES_PER_HOUR)));

        //can parse when there is a space
        assertThat(Speed.fromString("5.0 fpm"), is(Speed.of(5, FEET_PER_MINUTE)));
        assertThat(Speed.fromString("5.0 fps"), is(Speed.of(5, FEET_PER_SECOND)));
        assertThat(Speed.fromString("5.0 kph"), is(Speed.of(5, KILOMETERS_PER_HOUR)));
        assertThat(Speed.fromString("5.0 kn"), is(Speed.of(5, KNOTS)));
        assertThat(Speed.fromString("5.0 mps"), is(Speed.of(5, METERS_PER_SECOND)));
        assertThat(Speed.fromString("5.0 mph"), is(Speed.of(5, MILES_PER_HOUR)));

        //can parse different levels of accuracy
        assertThat(Speed.fromString("5 fpm"), is(Speed.of(5, FEET_PER_MINUTE)));
        assertThat(Speed.fromString("5.01 fps"), is(Speed.of(5.01, FEET_PER_SECOND)));
        assertThat(Speed.fromString("5.12345 kph"), is(Speed.of(5.12345, KILOMETERS_PER_HOUR)));
        assertThat(Speed.fromString("5E5 kn"), is(Speed.of(5E5, KNOTS)));
        assertThat(Speed.fromString("5.000001 mph"), is(Speed.of(5.000001, MILES_PER_HOUR)));

        //can parse zero and negative numbers
        assertThat(Speed.fromString("-5 fpm"), is(Speed.of(-5, FEET_PER_MINUTE)));
        assertThat(Speed.fromString("-5.01 fps"), is(Speed.of(-5.01, FEET_PER_SECOND)));
        assertThat(Speed.fromString("-5.12345 kph"), is(Speed.of(-5.12345, KILOMETERS_PER_HOUR)));
        assertThat(Speed.fromString("-5E5 kn"), is(Speed.of(-5E5, KNOTS)));
        assertThat(Speed.fromString("0.0 mps"), is(Speed.of(0, METERS_PER_SECOND)));
        assertThat(Speed.fromString("-5.000001 mph"), is(Speed.of(-5.000001, MILES_PER_HOUR)));
    }

    @Test
    public void testToString_numDigits_speedUnit() {
        Speed oneKnot = Speed.of(1, KNOTS);
        assertThat("1.000kn", is(oneKnot.toString()));
        assertThat("1.0kn", is(oneKnot.toString(1, KNOTS)));
        assertThat("1.151mph", is(oneKnot.toString(3, MILES_PER_HOUR)));
        assertThat("0.51mps", is(oneKnot.toString(2, METERS_PER_SECOND)));
    }

    @Test
    public void toStringShouldReflectDeclaredUnit() {

        Speed oneKnot = Speed.of(1.0, KNOTS);
        Speed oneMeterPerSecond = Speed.of(1.0, METERS_PER_SECOND);
        Speed oneFootPerMinute = Speed.of(1.0, FEET_PER_MINUTE);
        Speed oneFootPerSecond = Speed.of(1.0, FEET_PER_SECOND);  //will be reported in

        assertThat(oneKnot.toString(), is("1.000kn"));
        assertThat(oneMeterPerSecond.toString(), is("1.000mps"));
        assertThat(oneFootPerMinute.toString(), is("1.000fpm"));
        assertThat(oneFootPerSecond.toString(), is("60.000fpm"));
    }

    @Test
    public void toStringAndParsingEquivalence() {

        Speed startingSpeed = Speed.of(1.23456, KNOTS);

        assertThat(startingSpeed.toString(1), is("1.2kn"));
        assertThat(startingSpeed.toString(5), is("1.23456kn"));

        assertThat(Speed.fromString("1.23456kn"), is(startingSpeed));
    }

    @Test
    public void testZeroConstant() {

        assertTrue(Speed.ZERO.isZero());
        assertEquals(Speed.ZERO.inFeetPerSecond(),
            Speed.of(0.0, FEET_PER_SECOND).inFeetPerSecond(), 1E-10);
    }

    @Test
    public void testAbsoluteValue() {

        Speed positiveSpeed = Speed.of(45.6, KNOTS);
        Speed zeroSpeed = Speed.of(0, METERS_PER_SECOND);
        Speed negativeSpeed = Speed.of(-12.3, FEET_PER_MINUTE);

        assertThat(Speed.of(45.6, KNOTS), is(positiveSpeed.abs()));
        assertThat(Speed.of(0, METERS_PER_SECOND), is(zeroSpeed.abs()));
        assertThat(Speed.of(12.3, FEET_PER_MINUTE), is(negativeSpeed.abs()));
    }

}
