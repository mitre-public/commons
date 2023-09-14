package org.mitre.caasd.commons.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.util.DemotedException;

public class UncheckTest {

    @Test
    public void testCheckedFunction() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels
                .stream()
                .map(CheckedFunction.demote(this::color))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(Color.RED, Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testCheckedPredicate() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        String color = labels
                .stream()
                .filter(CheckedPredicate.demote(s -> Color.RED.equals(color(s))))
                .findFirst()
                .orElse(null);

        assertEquals("red", color);
    }

    @Test
    public void testCheckedBiFunction() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels
                .stream()
                .reduce((List<Color>) new ArrayList<Color>(),
                        CheckedBiFunction.demote((coll, s) -> {
                            coll.add(color(s));
                            return coll;
                        }),
                        (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));

        assertEquals(Arrays.asList(Color.RED, Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testUnchecked() {
        List<String> labels = Arrays.asList("red", "green", "yellow");
        List<Color> colors = labels
                .stream()
                .filter(Uncheck.pred(s -> !Color.RED.equals(color(s))))
                .map(Uncheck.func(this::color))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(Color.GREEN, Color.YELLOW), colors);
    }

    @Test
    public void testCheckedPredicate_throwsDemoted() {
        List<String> labels = Arrays.asList("pink", "red", "green", "yellow");

        DemotedException ex = assertThrows(DemotedException.class, () -> {
            labels
                    .stream()
                    .filter(CheckedPredicate.demote(s -> Color.RED.equals(color(s))))
                    .findFirst()
                    .orElse(null);
        });
        assertInstanceOf(CheckedIllegalArgumentException.class, ex.getCause());
    }

    @Test
    public void testCheckedBiFunction_throwRuntime() {
        List<String> labels = Arrays.asList("red", "green", "yellow");

        assertThrows(UnsupportedOperationException.class, () -> {
            labels
                    .stream()
                    .reduce(Collections.emptyList(), // immutable empty list -> cannot add
                            CheckedBiFunction.demote((coll, s) -> {
                                coll.add(color(s));
                                return coll;
                            }),
                            (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));
        });
    }

    public enum Color {
        RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET
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
