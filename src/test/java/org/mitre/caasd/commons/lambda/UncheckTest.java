package org.mitre.caasd.commons.lambda;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mitre.caasd.commons.util.DemotedException;

import org.junit.jupiter.api.Test;

public class UncheckTest {

    @Test
    public void testCheckedFunction() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels.stream().map(Uncheck.func(this::color)).collect(Collectors.toList());

        assertEquals(Arrays.asList(Color.RED, Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testCheckedPredicate() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        String color = labels.stream()
                .filter(Uncheck.pred(s -> Color.RED.equals(color(s))))
                .findFirst()
                .orElse(null);

        assertEquals("red", color);
    }

    @Test
    public void testCheckedConsumer() {

        // just a "Consumer" that throws an Exception...
        CheckedConsumer<String> brokenConsumer = str -> {
            throw new ParseException("I always fail", 0);
        };

        List<String> labels = Arrays.asList("red", "green", "yellow");

        DemotedException ex =
                assertThrows(DemotedException.class, () -> labels.forEach(Uncheck.consumer(brokenConsumer)));

        assertInstanceOf(ParseException.class, ex.getCause());
    }

    @Test
    public void testCheckedConsumer_noExceptionThrown() {

        // just a "Consumer" that might throw an Exception...
        CheckedConsumer<String> stringConsumer = str -> {
            if (str.startsWith("e")) {
                throw new IllegalArgumentException("Cannot start with e");
            }
        };

        List<String> aList = Arrays.asList("red", "green", "yellow");

        // aList.forEach(stringConsumer); //does not compile!, Hence the class
        aList.forEach(Uncheck.consumer(stringConsumer));
    }

    @Test
    public void testCheckedBiFunction() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels.stream()
                .reduce(
                        (List<Color>) new ArrayList<Color>(),
                        Uncheck.biFunc((coll, s) -> {
                            coll.add(color(s));
                            return coll;
                        }),
                        (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));

        assertEquals(Arrays.asList(Color.RED, Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testUnchecked() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels.stream()
                .filter(Uncheck.pred(s -> !Color.RED.equals(color(s))))
                .map(Uncheck.func(this::color))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testCheckedPredicate_throwsDemoted() {
        List<String> labels = Arrays.asList("pink", "red", "green", "yellow");

        DemotedException ex = assertThrows(DemotedException.class, () -> {
            labels.stream()
                    .filter(Uncheck.pred(s -> Color.RED.equals(color(s))))
                    .findFirst()
                    .orElse(null);
        });
        assertInstanceOf(CheckedIllegalArgumentException.class, ex.getCause());
    }

    @Test
    public void testCheckedBiFunction_throwRuntime() {
        List<String> labels = Arrays.asList("red", "green", "yellow");

        assertThrows(UnsupportedOperationException.class, () -> {
            labels.stream()
                    .reduce(
                            Collections.emptyList(), // immutable empty list -> cannot add
                            Uncheck.biFunc((coll, s) -> {
                                coll.add(color(s));
                                return coll;
                            }),
                            (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));
        });
    }

    @Test
    public void testCheckedBinaryOp_failing() {

        List<String> labels = Arrays.asList("red", "green", "yellow");

        CheckedBinaryOperator<String> brokenJoiner = (str1, str2) -> {
            throw new ParseException("I always fail", 0);
        };

        DemotedException ex = assertThrows(DemotedException.class, () -> {
            labels.stream().reduce(Uncheck.biOp(brokenJoiner)).get();
        });
        assertThat(ex.getCause(), instanceOf(ParseException.class));
    }

    @Test
    public void testCheckedBinaryOp_working() {

        List<String> labels = Arrays.asList("red", "green", "yellow");

        // never actually throws and exception
        CheckedBinaryOperator<String> workingJoiner = (str1, str2) -> str1.concat(str2);

        // DOES NOT COMPILE!, workingJoiner can't take the place of a BinaryOperator
        // String result = labels.stream().reduce(workingJoiner).get();

        String result = labels.stream().reduce(Uncheck.biOp(workingJoiner)).get();

        assertThat(result, is("redgreenyellow"));
    }

    public enum Color {
        RED,
        ORANGE,
        YELLOW,
        GREEN,
        BLUE,
        VIOLET
    }

    private static final class CheckedIllegalArgumentException extends Exception {

        public CheckedIllegalArgumentException(IllegalArgumentException e) {
            super(e.getMessage());
        }
    }

    private Color color(String s) throws CheckedIllegalArgumentException {
        try {
            return Color.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            // rethrow as checked exception
            throw new CheckedIllegalArgumentException(e);
        }
    }
}
