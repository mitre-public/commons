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
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.fileutil.FileUtils;

public class TimeWindowTest {

    @Test
    public void testConstructor() {
        /*
         * Confirm the constructor and the data access methods are properly aligned.
         */
        TimeWindow sample = new TimeWindow(
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(1)
        );

        assertThat(Instant.EPOCH, is(sample.start()));

        assertThat(Instant.EPOCH.plusSeconds(1), is(sample.end()));
    }

    @Test
    public void testSingleInstantWindow() {
        //this should be possible
        TimeWindow sample = new TimeWindow(Instant.EPOCH, Instant.EPOCH);

        assertTrue(sample.contains(Instant.EPOCH));
        assertTrue(sample.duration().equals(Duration.ZERO));
    }

    @Test
    public void testConstructorWithBadInput_1() {

        try {
            TimeWindow.of(Instant.EPOCH.plusSeconds(1), Instant.EPOCH);
            fail("The above call should fail because the inputs are out of order");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains(
                "The start of a TimeWindow cannot come after the end of a TimeWindow"
            ));
        }
    }

    @Test
    public void testConstructorWithBadInput_2() {

        try {
            TimeWindow.of(null, Instant.EPOCH);
            fail("The above call should fail because the 1st input is null");
        } catch (NullPointerException npe) {
            assertTrue(npe.getMessage().contains(
                "The start of the time window cannot be null"
            ));
        }
    }

    @Test
    public void testConstructorWithBadInput_3() {

        try {
            TimeWindow.of(Instant.EPOCH, null);
            fail("The above call should fail because the 2nd input is null");
        } catch (NullPointerException npe) {
            assertTrue(npe.getMessage().contains(
                "The end of the time window cannot be null"
            ));
        }
    }

    @Test
    public void testConstructorOfZeroLengthWindow() {
        //verify that this is doable
        assertDoesNotThrow(
            () -> TimeWindow.of(Instant.EPOCH, Instant.EPOCH)
        );
    }

    @Test
    public void testStaticConstructor() {
        TimeWindow sample = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));

        assertEquals(
            Instant.EPOCH,
            sample.start()
        );

        assertEquals(
            Instant.EPOCH.plusSeconds(1),
            sample.end()
        );
    }

    @Test
    public void testContains() {
        TimeWindow instance = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(120));

        assertThat("The beginning is contained", instance.contains(Instant.EPOCH), is(true));
        assertThat("The end is contained", instance.contains(Instant.EPOCH.plusSeconds(120)), is(true));
        assertThat(
            "Just prior to the beginning is not contained",
            instance.contains(Instant.EPOCH.minusNanos(1)), is(false)
        );
        assertThat("Just after the end is not contained",
            instance.contains(Instant.EPOCH.plusSeconds(120).plusNanos(1)), is(false)
        );
    }

    @Test
    public void testSerialization() {

        TimeWindow original = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(120));

        String fileName = "testTimeWindow.ser";

        assertThat(
            "The file we are serilizing to should not exists yet",
            new File(fileName).exists(), is(false)
        );

        FileUtils.serialize(original, fileName);

        assertThat(
            "The file we serilized to should now exist",
            new File(fileName).exists(), is(true)
        );

        TimeWindow deserializedTimeWindow = (TimeWindow) FileUtils.deserialize(new File(fileName));

        //confirm a fields are the same..
        assertEquals(original.start(), deserializedTimeWindow.start());
        assertEquals(original.end(), deserializedTimeWindow.end());

        new File(fileName).delete();
    }

    @Test
    public void testLength() {
        TimeWindow original = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(120));

        assertEquals(
            120,
            original.duration().getSeconds()
        );
        assertEquals(
            120 * 1000,
            original.duration().toMillis()
        );
    }

    @Test
    public void testOverlapsWith() {

        TimeWindow all = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(120));
        TimeWindow firstHalf = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        TimeWindow secondHalf = TimeWindow.of(Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120));
        TimeWindow subset = TimeWindow.of(Instant.EPOCH.plusSeconds(1), Instant.EPOCH.plusSeconds(119));

        assertTrue(all.overlapsWith(firstHalf));
        assertTrue(firstHalf.overlapsWith(all));

        assertTrue(all.overlapsWith(secondHalf));
        assertTrue(secondHalf.overlapsWith(all));

        assertThat("these windows share an endpoint", secondHalf.overlapsWith(firstHalf), is(true));
        assertThat("these windows share an endpoint", firstHalf.overlapsWith(secondHalf), is(true));

        assertTrue(all.overlapsWith(subset));
        assertTrue(subset.overlapsWith(all));
    }

    @Test
    public void testGetOverlapWith() {

        TimeWindow all = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(120));
        TimeWindow firstHalf = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        TimeWindow secondHalf = TimeWindow.of(Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120));
        TimeWindow subset = TimeWindow.of(Instant.EPOCH.plusSeconds(1), Instant.EPOCH.plusSeconds(119));

        assertEquals(
            TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            all.getOverlapWith(firstHalf).get()
        );
        assertEquals(
            TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            firstHalf.getOverlapWith(all).get()
        );

        assertEquals(
            TimeWindow.of(Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120)),
            all.getOverlapWith(secondHalf).get()
        );
        assertEquals(
            TimeWindow.of(Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120)),
            secondHalf.getOverlapWith(all).get()
        );

        assertTrue(firstHalf.getOverlapWith(secondHalf).isPresent());
        assertTrue(secondHalf.getOverlapWith(firstHalf).isPresent());

        assertEquals(
            TimeWindow.of(Instant.EPOCH.plusSeconds(1), Instant.EPOCH.plusSeconds(119)),
            all.getOverlapWith(subset).get()
        );
        assertEquals(
            TimeWindow.of(Instant.EPOCH.plusSeconds(1), Instant.EPOCH.plusSeconds(119)),
            subset.getOverlapWith(all).get()
        );
    }

    @Test
    public void testBug16_timeWindowsThatShareAnEndpointShouldOverlap() {

        TimeWindow window1 = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        TimeWindow window2 = TimeWindow.of(Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120));

        //do this twice, changing argument order each time
        assertThat(
            "The overlap contains a single instant, but it does exist",
            window1.getOverlapWith(window2).isPresent(), is(true)
        );
        assertThat(
            "The overlap contains a single instant, but it does exist",
            window2.getOverlapWith(window1).isPresent(), is(true)
        );
        //do this twice, changing argument order each time
        assertThat(
            "The overlap contains a single instant, but it does exist",
            window1.getOverlapWith(window2).get().duration(), is(Duration.ZERO)
        );
        assertThat(
            "The overlap contains a single instant, but it does exist",
            window2.getOverlapWith(window1).get().duration(), is(Duration.ZERO)
        );
    }

    @Test
    public void testEquals() {

        TimeWindow item = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        TimeWindow copy = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        TimeWindow diff1 = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(2));
        TimeWindow diff2 = TimeWindow.of(Instant.EPOCH.minusSeconds(1), Instant.EPOCH.plusSeconds(1));

        assertTrue(item.equals(item));
        assertTrue(item.equals(copy));
        assertTrue(copy.equals(item));

        assertFalse(item.equals(diff1));
        assertFalse(diff1.equals(copy));

        assertFalse(item.equals(diff2));
        assertFalse(diff2.equals(copy));

        assertFalse(item.equals("not a TimeWindow"));

        assertFalse(item.equals(null));
    }

    @Test
    public void testHashcode() {

        TimeWindow item = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        TimeWindow copy = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        TimeWindow diff1 = TimeWindow.of(Instant.EPOCH, Instant.EPOCH.plusSeconds(2));
        TimeWindow diff2 = TimeWindow.of(Instant.EPOCH.minusSeconds(2), Instant.EPOCH.plusSeconds(1));

        assertTrue(item.hashCode() == copy.hashCode());
        assertFalse(item.hashCode() == diff1.hashCode());
        assertFalse(item.hashCode() == diff2.hashCode());
    }

    @Test
    public void testConsistencyBtwContainsAndOverlaps() {

        Instant time1 = Instant.EPOCH;
        Instant time2 = Instant.EPOCH.plusSeconds(30);
        Instant time3 = Instant.EPOCH.plusSeconds(60);

        TimeWindow window1 = TimeWindow.of(time1, time2);
        TimeWindow window2 = TimeWindow.of(time2, time3);

        assertTrue(window1.contains(time1));
        assertTrue(window1.contains(time2));

        assertTrue(window2.contains(time2));
        assertTrue(window2.contains(time3));

        assertThat(
            "If both windows include time2 then both TimeWindows should overlap",
            window1.overlapsWith(window2), is(true)
        );
    }

    @Test
    public void testToFractionOfRange() {

        Instant time1 = Instant.EPOCH;
        Instant time2 = Instant.EPOCH.plusSeconds(30);

        TimeWindow window = TimeWindow.of(time1, time2);
        double TOLERANCE = 0.00001;

        assertEquals(window.toFractionOfRange(time1), 0.0, TOLERANCE);
        assertEquals(window.toFractionOfRange(time2), 1.0, TOLERANCE);
        assertEquals(window.toFractionOfRange(Instant.EPOCH.plusSeconds(15)), 0.5, TOLERANCE);

        assertEquals(window.toFractionOfRange(Instant.EPOCH.plusSeconds(45)), 1.5, TOLERANCE);
        assertEquals(window.toFractionOfRange(Instant.EPOCH.minusSeconds(45)), -1.5, TOLERANCE);
        assertEquals(window.toFractionOfRange(Instant.EPOCH.minusSeconds(30)), -1.0, TOLERANCE);
    }

    @Test
    public void testInstantWithin() {

        Instant time1 = Instant.EPOCH;
        Instant time2 = Instant.EPOCH.plusSeconds(30);

        TimeWindow window = TimeWindow.of(time1, time2);

        assertEquals(
            Instant.EPOCH,
            window.instantWithin(0.0)
        );
        assertEquals(
            Instant.EPOCH.plusSeconds(30),
            window.instantWithin(1.0)
        );
        assertEquals(
            Instant.EPOCH.plusSeconds(15),
            window.instantWithin(0.5)
        );
    }

    @Test
    public void testInstantWithinOnBadInput() {

        TimeWindow window = TimeWindow.of(
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(30)
        );

        try {
            window.instantWithin(1.1);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("no greater than 1"));
        }

        try {
            window.instantWithin(-0.1);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("at least 0"));
        }
    }

    @Test
    public void isEmpty_onZeroDurationWindow() {

        TimeWindow window = TimeWindow.of(
            Instant.EPOCH,
            Instant.EPOCH
        );

        assertThat(window.isEmpty(), is(true));
    }

    @Test
    public void isEmpty_onNonZeroDurationWindow() {

        TimeWindow window = TimeWindow.of(
            Instant.EPOCH,
            Instant.EPOCH.plusMillis(1)
        );

        assertThat(window.isEmpty(), is(false));
    }

    @Test
    public void pad_increaseDurationByTwicePadding() {

        TimeWindow tw = TimeWindow.of(EPOCH, EPOCH.plusSeconds(10));
        TimeWindow padded = tw.pad(Duration.ofSeconds(2));

        assertThat(padded.duration(), is(Duration.ofSeconds(14)));
    }

    @Test
    public void shiftSlides() {
        TimeWindow window = TimeWindow.of(EPOCH, EPOCH.plusSeconds(11));
        Duration shiftAmount = Duration.ofSeconds(3);

        TimeWindow shifted = window.shift(shiftAmount);

        assertThat(shifted.duration(), is(window.duration()));
        assertThat(shifted.start(), is(window.start().plus(shiftAmount)));
        assertThat(shifted.end(), is(window.end().plus(shiftAmount)));
    }

    @Test
    public void bulkSlide() {
        ArrayList<TimeWindow> list = newArrayList(
            TimeWindow.of(EPOCH, EPOCH.plusSeconds(11)),
            TimeWindow.of(EPOCH.plusSeconds(1), EPOCH.plusSeconds(12))
        );

        ArrayList<TimeWindow> shifted = TimeWindow.shiftAll(list, Duration.ofSeconds(1000));

        assertThat(shifted.get(0).start(), is(EPOCH.plusSeconds(1000)));
        assertThat(shifted.get(1).start(), is(EPOCH.plusSeconds(1001)));

        assertThat(shifted.get(0).end(), is(EPOCH.plusSeconds(1011)));
        assertThat(shifted.get(1).end(), is(EPOCH.plusSeconds(1012)));
    }

    @Test
    public void shiftSlides_inMillis() {
        TimeWindow window = TimeWindow.of(EPOCH, EPOCH.plusSeconds(11));
        long shiftAmount = Duration.ofSeconds(3).toMillis();

        TimeWindow shifted = window.shiftMillis(shiftAmount);

        assertThat(shifted.duration(), is(window.duration()));
        assertThat(shifted.start(), is(window.start().plusMillis(shiftAmount)));
        assertThat(shifted.end(), is(window.end().plusMillis(shiftAmount)));
    }

    @Test
    public void bulkSlide_inMillis() {
        ArrayList<TimeWindow> list = newArrayList(
            TimeWindow.of(EPOCH, EPOCH.plusSeconds(11)),
            TimeWindow.of(EPOCH.plusSeconds(1), EPOCH.plusSeconds(12))
        );

        ArrayList<TimeWindow> shifted = TimeWindow.shiftAll(list, 1000);

        assertThat(shifted.get(0).start(), is(EPOCH.plusSeconds(1)));
        assertThat(shifted.get(1).start(), is(EPOCH.plusSeconds(2)));

        assertThat(shifted.get(0).end(), is(EPOCH.plusSeconds(12)));
        assertThat(shifted.get(1).end(), is(EPOCH.plusSeconds(13)));
    }
}
