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

package org.mitre.caasd.commons.math.locationfit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.Double.parseDouble;
import static java.lang.Math.abs;
import static java.time.Instant.EPOCH;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.caasd.commons.Time.compareByTime;
import static org.mitre.caasd.commons.Time.durationBtw;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceAsFile;
import static org.mitre.caasd.commons.math.locationfit.LatLongFitter.unMod;
import static org.mitre.caasd.commons.util.NeighborIterator.newNeighborIterator;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.mitre.caasd.commons.*;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.testing.TestLocationDatum;
import org.mitre.caasd.commons.util.IterPair;
import org.mitre.caasd.commons.util.NeighborIterator;

import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.StatsAccumulator;
import org.junit.jupiter.api.Test;

public class LocalPolyInterpolatorTest {

    /**
     * @todo -- Weave in a plotting tool so that these tests automatically generate images that
     *     show the altitude, LatLong, Speed, and Course data.
     */
    @Test
    public void basicUsage() {

        LocalPolyInterpolator qi = new LocalPolyInterpolator(Duration.ofMinutes(1));

        List<TestLocationDatum> testData = testPoints();
        Collections.sort(testData, (a, b) -> a.time().compareTo(b.time()));

        List<PositionRecord<TestLocationDatum>> wrappedTestData = asRecords(testData);

        TimeWindow range = TimeWindow.of(
                testData.get(0).time(), testData.get(testData.size() - 1).time());

        int NUM_SAMPLES = 300;

        List<KineticRecord<TestLocationDatum>> interpolatedPoints = newArrayList();

        for (int i = 0; i < NUM_SAMPLES; i++) {

            Instant sampleTime = range.instantWithin(i * 1.0 / (double) NUM_SAMPLES);
            KineticRecord<TestLocationDatum> approximation =
                    qi.floorInterpolate(wrappedTestData, sampleTime).get();

            interpolatedPoints.add(approximation);
        }

        Collections.sort(interpolatedPoints, compareByTime());

        validateLatLongs(testData, interpolatedPoints);
        validateAltitudes(testData, interpolatedPoints);
        validateSpeeds(testData, interpolatedPoints);
        validateCourses(testData, interpolatedPoints);
        validateTurnRates(interpolatedPoints);
        validateTurnRateAndCurvatureAreConsistent(interpolatedPoints);
    }

    @Test
    public void basicUsage_trackCrossesInternationalDateLine() {
        // same test as above, but with a track that goes over the international date line

        LocalPolyInterpolator qi = new LocalPolyInterpolator(Duration.ofMinutes(1));

        List<TestLocationDatum> testData = testPoints_crossInternationalDateLine();
        Collections.sort(testData, (a, b) -> a.time().compareTo(b.time()));

        List<PositionRecord<TestLocationDatum>> wrappedTestData = asRecords(testData);

        TimeWindow range = TimeWindow.of(
                testData.get(0).time(), testData.get(testData.size() - 1).time());

        int NUM_SAMPLES = 300;

        List<KineticRecord<TestLocationDatum>> interpolatedPoints = newArrayList();

        for (int i = 0; i < NUM_SAMPLES; i++) {

            Instant sampleTime = range.instantWithin(i * 1.0 / (double) NUM_SAMPLES);
            KineticRecord<TestLocationDatum> approximation =
                    qi.floorInterpolate(wrappedTestData, sampleTime).get();

            interpolatedPoints.add(approximation);
        }

        Collections.sort(interpolatedPoints, compareByTime());

        validateLatLongs(testData, interpolatedPoints);
        validateAltitudes(testData, interpolatedPoints);
        validateSpeeds(testData, interpolatedPoints);
        validateCourses(testData, interpolatedPoints);
        validateTurnRates(interpolatedPoints);
        validateTurnRateAndCurvatureAreConsistent(interpolatedPoints);
    }

    private void validateTurnRateAndCurvatureAreConsistent(List<KineticRecord<TestLocationDatum>> interpolatedPoints) {
        /*
         * Positive Turn rates should yield positive turn radii (i.e. clock wise turning)
         * Negative Turn rates should yield negative turn radii (i.e. counter clock wise turning)
         */
        int count = 0;
        for (KineticRecord<TestLocationDatum> pt : interpolatedPoints) {
            if (pt.kinetics().turnRate() > 0) {
                assertThat(pt.kinetics().turnRadius().isPositive(), is(true));
                count++;
            }

            if (pt.kinetics().turnRate() < 0) {
                assertThat(pt.kinetics().turnRadius().isNegative(), is(true));
                count++;
            }
        }
        assertThat(count > 100, is(true));
    }

    private void validateLatLongs(List<TestLocationDatum> testData, List<KineticRecord<TestLocationDatum>> fitData) {

        StatsAccumulator stats = new StatsAccumulator();

        for (KineticRecord<TestLocationDatum> pt : fitData) {
            Pair<TestLocationDatum, TestLocationDatum> floorAndCeiling = floorAndCeiling(testData, pt.time());

            double distToFloor =
                    pt.latLong().distanceTo(latLongFor(floorAndCeiling.first())).inNauticalMiles();
            double distToCeiling = pt.latLong()
                    .distanceTo(latLongFor(floorAndCeiling.second()))
                    .inNauticalMiles();

            stats.add(distToFloor);
            stats.add(distToCeiling);
        }

        // perform unit tests on the aggregate statistics.
        assertThat("There are at least 100 samples", stats.count() > 100, is(true));
        assertThat("The average distance is small", stats.mean() < .075, is(true));
        assertThat("No single sample has a large distance error", stats.max() < 2, is(true));
    }

    private void validateAltitudes(List<TestLocationDatum> testData, List<KineticRecord<TestLocationDatum>> fitData) {
        // Aggregate statistics on the difference between the raw data and the fit data
        StatsAccumulator stats = new StatsAccumulator();

        for (KineticRecord<TestLocationDatum> pt : fitData) {
            Pair<TestLocationDatum, TestLocationDatum> floorAndCeiling = floorAndCeiling(testData, pt.time());

            double deltaToFirst = abs(pt.altitude().inFeet() - altitudeOf(floorAndCeiling.first()));
            double deltaToSecond = abs(pt.altitude().inFeet() - altitudeOf(floorAndCeiling.second()));

            stats.add(deltaToFirst);
            stats.add(deltaToSecond);
        }

        // perform unit tests on the aggregate statistics.
        assertThat("There are at least 100 samples", stats.count() > 100, is(true));
        assertThat("The average delta is small", stats.mean() < 50, is(true));
        assertThat("No single sample has a large difference in altitude", stats.max() < 250, is(true));
    }

    private void validateSpeeds(List<TestLocationDatum> testData, List<KineticRecord<TestLocationDatum>> fitData) {

        // Aggregate statistics on the difference between the raw data and the fit data
        StatsAccumulator stats = new StatsAccumulator();

        for (KineticRecord<TestLocationDatum> pt : fitData) {
            Pair<TestLocationDatum, TestLocationDatum> floorAndCeiling = floorAndCeiling(testData, pt.time());

            double deltaToFirst = abs(pt.kinetics().speed().inKnots()
                    - floorAndCeiling.first().speed().inKnots());
            double deltaToSecond = abs(pt.kinetics().speed().inKnots()
                    - floorAndCeiling.second().speed().inKnots());

            stats.add(deltaToFirst);
            stats.add(deltaToSecond);
        }

        assertThat("There are at least 100 samples", stats.count() > 100, is(true));
        assertThat("The average delta is small", stats.mean() < 4, is(true));
        assertThat("No single sample has a large difference in speed", stats.max() < 35, is(true));
    }

    private void validateCourses(List<TestLocationDatum> testData, List<KineticRecord<TestLocationDatum>> samples) {

        // Aggregate statistics on the difference between the raw data and the fit data
        StatsAccumulator stats = new StatsAccumulator();
        int badCount = 0;
        Course THRESHOLD = Course.ofDegrees(30);

        for (KineticRecord<TestLocationDatum> pt : samples) {
            Pair<TestLocationDatum, TestLocationDatum> floorAndCeiling = floorAndCeiling(testData, pt.time());

            Course deltaToFirst = Course.angleBetween(
                    floorAndCeiling.first().course(), pt.kinetics().course());

            Course deltaToSecond = Course.angleBetween(
                    pt.kinetics().course(), floorAndCeiling.second().course());

            // This is more a measurement of how poorly the raw course data matches the raw LatLong data
            if (deltaToFirst.isGreaterThan(THRESHOLD) || deltaToSecond.isGreaterThan(THRESHOLD)) {
                badCount++;
            }

            stats.add(deltaToFirst.inDegrees());
            stats.add(deltaToSecond.inDegrees());
        }

        assertThat("There are at least 100 samples", stats.count() > 100, is(true));
        assertThat("The average delta is small", stats.mean() < 2, is(true));
        assertThat("Few samples are bad", badCount < 3, is(true));
    }

    private void validateTurnRates(List<KineticRecord<TestLocationDatum>> interpolatedPoints) {

        NeighborIterator<KineticRecord<TestLocationDatum>> pairIterator = newNeighborIterator(interpolatedPoints);

        PairedStatsAccumulator stats = new PairedStatsAccumulator();

        while (pairIterator.hasNext()) {
            IterPair<KineticRecord<TestLocationDatum>> pair = pairIterator.next();

            double newCourse = pair.current().kinetics().course().inDegrees();
            double oldCourse = pair.prior().kinetics().course().inDegrees();

            // ignore big changes (like from 1 degree to 359 degree or vice versa)
            if (abs(newCourse - oldCourse) > 200) {
                continue;
            }

            Duration timeDelta = durationBtw(pair.prior().time(), pair.current().time());

            double courseDelta = newCourse - oldCourse; // estimated over ~5 second time horizon
            double turnRate = pair.prior().kinetics().turnRate(); // instantaneous approximation

            stats.add(courseDelta, turnRate);
        }

        double correlation = stats.pearsonsCorrelationCoefficient();
        double slope = stats.leastSquaresFit().slope();

        assertThat("The correlation is VERY strong", correlation > .95, is(true));
        assertThat("The slope is positive", slope > 0.05, is(true));
        // i.e. positive courseDeltas are associated with positive turn rates (and vice versa)
    }

    public static Pair<TestLocationDatum, TestLocationDatum> floorAndCeiling(
            List<TestLocationDatum> points, Instant time) {

        TestLocationDatum ceiling = null;
        TestLocationDatum floor = null;

        for (TestLocationDatum pt : points) {

            if (pt.time().isBefore(time) || pt.time().equals(time)) {
                floor = pt;
            }
            if (pt.time().isAfter(time) && ceiling == null) {
                ceiling = pt;
            }
        }

        return Pair.of(floor, ceiling);
    }

    public List<TestLocationDatum> testPoints() {
        File file =
                getResourceAsFile(LocalPolyInterpolator.class, "oneTrack.txt").get();

        return FileUtils.fileLines(file).stream()
                .map(datum -> TestLocationDatum.parse(datum))
                .sorted((a, b) -> a.time().compareTo(b.time()))
                .collect(toList());
    }

    public List<TestLocationDatum> testPoints_crossInternationalDateLine() {

        List<TestLocationDatum> regularData = testPoints();

        List<LatLong> locations = regularData.stream()
                .map(x -> LatLong.of(x.latitude(), x.longitude()))
                .collect(toList());

        // we want the altered track to have its average longitude at exactly 180.0
        // this means some data will be on each side of international date-line
        double avgLongitude = LatLong.avgLatLong(locations).longitude();
        double delta = 180 - avgLongitude;

        // cannot just add a delta, that leads to illegal longitude values
        // hence unMod(x.longitude() + delta) instead of just "x.longitude() + delta"
        List<TestLocationDatum> altered = regularData.stream()
                .map(x -> new TestLocationDatum(
                        LatLong.of(x.latitude(), unMod(x.longitude() + delta)),
                        x.time(),
                        x.altitude(),
                        x.speed(),
                        x.course()))
                .collect(toList());

        return altered;
    }

    public List<PositionRecord<TestLocationDatum>> asRecords(List<TestLocationDatum> points) {

        return points.stream()
                .map(pt -> new PositionRecord<>(
                        pt,
                        new Position(
                                pt.time().toEpochMilli(),
                                pt.latitude(),
                                pt.longitude(),
                                pt.altitude().inFeet())))
                .collect(toList());
    }

    @Test
    public void cannotExtrapolateBeyondSourceData() {

        LocalPolyInterpolator qi = new LocalPolyInterpolator(Duration.ofMinutes(1));

        List<TestLocationDatum> testData = testPoints();

        TimeWindow range = TimeWindow.of(
                testData.get(0).time(), testData.get(testData.size() - 1).time());

        Instant beforeStart = range.start().minusMillis(1L);
        Instant start = range.start();
        Instant end = range.end();
        Instant afterEnd = range.end().plusMillis(1L);

        List<PositionRecord<TestLocationDatum>> wrappedTestData = asRecords(testData);
        Optional<KineticRecord<TestLocationDatum>> before = qi.floorInterpolate(wrappedTestData, beforeStart);
        Optional<KineticRecord<TestLocationDatum>> atStart = qi.floorInterpolate(wrappedTestData, start);
        Optional<KineticRecord<TestLocationDatum>> atEnd = qi.floorInterpolate(wrappedTestData, end);
        Optional<KineticRecord<TestLocationDatum>> after = qi.floorInterpolate(wrappedTestData, afterEnd);

        assertThat(before.isPresent(), is(false)); // no result provided outside TimeWindow
        assertThat(atStart.isPresent(), is(true));
        assertThat(atEnd.isPresent(), is(true));
        assertThat(after.isPresent(), is(false)); // no result provided outside TimeWindow
    }

    private static LatLong latLongFor(TestLocationDatum hit) {
        return LatLong.of(hit.latitude(), hit.longitude());
    }

    private static double altitudeOf(TestLocationDatum hit) {
        return hit.altitude().inFeet();
    }

    @Test
    public void basicUsage_withoutAltitudeData() {

        LocalPolyInterpolator qi = new LocalPolyInterpolator(Duration.ofMinutes(1), 3, true);

        List<TestLocationDatum> testData = testPoints();
        Collections.sort(testData, Comparator.comparing(TestLocationDatum::time));

        List<PositionRecord<TestLocationDatum>> wrappedTestData = asRecords(testData);

        TimeWindow range = TimeWindow.of(
                testData.get(0).time(), testData.get(testData.size() - 1).time());

        int NUM_SAMPLES = 300;

        List<KineticRecord<TestLocationDatum>> interpolatedPoints = newArrayList();

        for (int i = 0; i < NUM_SAMPLES; i++) {

            Instant sampleTime = range.instantWithin(i * 1.0 / (double) NUM_SAMPLES);
            KineticRecord<TestLocationDatum> approximation =
                    qi.floorInterpolate(wrappedTestData, sampleTime).get();

            interpolatedPoints.add(approximation);
        }

        // Verify all altitude data is ignored..
        for (KineticRecord<TestLocationDatum> kr : interpolatedPoints) {
            assertThat(kr.altitude(), is(Distance.ZERO_FEET));
            assertThat(kr.kinetics().climbRate(), is(Speed.ZERO_FEET_PER_MIN));
        }
    }

    private static class Dummy {}

    @Test
    public void verifyAccelerationDeduction() {

        // Numerically produce some LatLong data that shows an accelerating object..
        // Then verify the acceleration is correct

        Acceleration ACCEL = Acceleration.of(Speed.ofKnots(2.0)); // 1 knot per sec

        List<PositionRecord<Dummy>> records = createDataShowingConstantAcceleration(ACCEL);

        LocalPolyInterpolator interpolator = new LocalPolyInterpolator(Duration.ofMinutes(1), 7, true);

        KineticRecord<Dummy> kinetics =
                interpolator.floorInterpolate(records, EPOCH.plusMillis(2500L)).get();

        Acceleration deducedAccel = kinetics.kinetics().acceleration();

        // Approximation error can be as large a 1% of the input ACCEL value
        Speed threshold = ACCEL.speedDeltaPerSecond().times(.01);

        // Difference between input ACCEL and deduced value
        Speed error = ACCEL.speedDeltaPerSecond()
                .minus(deducedAccel.speedDeltaPerSecond())
                .abs();

        assertThat(error.isLessThan(threshold), is(true));
    }

    private List<PositionRecord<Dummy>> createDataShowingConstantAcceleration(Acceleration rate) {

        Duration TIME_STEP = Duration.ofMillis(250L);
        double scalar = ((double) TIME_STEP.toMillis()) / 1000.0;

        LinkedList<Position> locationData = newLinkedList();
        locationData.add(new Position(EPOCH, LatLong.of(38.9223, -77.2016)));
        Speed currentSpeed = Speed.ofKnots(0.0);

        for (int i = 0; i < 1_000; i++) {
            Position last = locationData.getLast();
            currentSpeed = currentSpeed.plus(rate.speedDeltaPerSecond().times(scalar));
            Distance dist = currentSpeed.times(TIME_STEP);
            LatLong nextLatLong = last.latLong().project(Course.EAST, dist);

            Position next = new Position(last.time().plus(TIME_STEP), nextLatLong);
            locationData.addLast(next);
        }

        return locationData.stream()
                .map(pos -> new PositionRecord<>(new Dummy(), pos))
                .collect(toList());
    }

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS X").withZone(ZoneOffset.UTC);

    /**
     * @param dateString The "date portion". For example the "10/18/2016" in
     *                   "10/18/2016,00:57:12.962"
     * @param timeString The "time portion" of a Nop Message. For example the "00:57:12.962" in
     *                   "10/18/2016,00:57:12.962"
     *
     * @return The Instant corresponding to the date and time (Z time is assume)
     */
    public static Instant parseTime(String dateString, String timeString) {

        ZonedDateTime zdt = ZonedDateTime.parse(dateString.replace("-", "/") + " " + timeString + " Z", DATE_FORMATTER);
        return Instant.from(zdt);
    }

    static PositionRecord<Dummy> parseTestInput(String input) {
        // Expected input = "07/17/2022,18:44:33.930,036.89253,-112.35783"
        // Expected input = "date,time,lat,long"
        String[] tokens = input.split(",");

        Position pos = new Position(
                parseTime(tokens[0], tokens[1]).toEpochMilli(), parseDouble(tokens[2]), parseDouble(tokens[3]));

        return PositionRecord.of(new Dummy(), pos);
    }

    @Test
    public void brokenExample_duplicate_time_and_position_1() {

        // FAILURE LOGS =
        //
        // Longitude is out of range: -222.96536332927727
        // Could not interpolate input data.
        //        sampleTime= 2022-07-17T18:44:40.600Z
        // 07/17/2022,18:44:33.930,036.89253,-112.35783
        // 07/17/2022,18:44:47.270,036.89253,-112.35783
        // 07/17/2022,18:44:47.270,036.89253,-112.35783

        PositionRecord<Dummy> p1 = parseTestInput("07/17/2022,18:44:33.930,036.89253,-112.35783");
        PositionRecord<Dummy> p2 = parseTestInput("07/17/2022,18:44:47.270,036.89253,-112.35783");
        PositionRecord<Dummy> p3 = parseTestInput("07/17/2022,18:44:47.270,036.89253,-112.35783");
        // notice, p2 and p3 are the same! -- Our raw data contains a duplicate, how should this be handled
        LocalPolyInterpolator interpolator = new LocalPolyInterpolator(Duration.ofSeconds(60), 3, true);

        Optional<KineticRecord<Dummy>> result =
                interpolator.floorInterpolate(newArrayList(p1, p2, p3), Instant.parse("2022-07-17T18:44:40.600Z"));

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void brokenExample_duplicate_time_and_position_2() {

        // FAILURE LOGS =
        //
        //    Latitude is out of range: 96.0
        //    Could not interpolate input data.
        //        sampleTime= 2022-07-17T18:55:52Z
        // 07/17/2022,18:55:38.713,037.59117,-119.19317
        // 07/17/2022,18:55:38.713,037.59117,-119.19317
        // 07/17/2022,18:56:14.713,037.57589,-119.09622

        PositionRecord<Dummy> p1 = parseTestInput("07/17/2022,18:55:38.713,037.59117,-119.19317");
        PositionRecord<Dummy> p2 = parseTestInput("07/17/2022,18:55:38.713,037.59117,-119.19317");
        PositionRecord<Dummy> p3 = parseTestInput("07/17/2022,18:56:14.713,037.57589,-119.09622");
        // notice, p2 and p3 are the same! -- Our raw data contains a duplicate, how should this be handled
        LocalPolyInterpolator interpolator = new LocalPolyInterpolator(Duration.ofSeconds(60), 3, true);

        Optional<KineticRecord<Dummy>> result =
                interpolator.floorInterpolate(newArrayList(p1, p2, p3), Instant.parse("2022-07-17T18:55:52Z"));

        assertThat(result.isPresent(), is(false));
    }
}
