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

package org.mitre.caasd.commons.util;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.util.Preconditions.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

public class PreconditionsTest {

    @Test
    public void checkNoNullElement_accepts_iterable() {
        ArrayList<Integer> list = newArrayList(1, 2, 3);
        checkNoNullElement(list);
    }

    @Test
    public void checkNoNullElement_accepts_array() {
        Integer[] array = new Integer[] {1, 2, 3};
        checkNoNullElement(array);
    }

    @Test
    public void checkNoNullElement_throwsNpeFromIterable() {
        ArrayList<Integer> list = newArrayList(1, null, 3);

        assertThrows(NullPointerException.class, () -> checkNoNullElement(list));
    }

    @Test
    public void checkNoNullElement_throwsNpeFromArray() {
        Integer[] array = new Integer[] {1, null, 3};

        assertThrows(NullPointerException.class, () -> checkNoNullElement(array));
    }

    @Test
    public void checkAllTrue_accepts_iterable() {

        ArrayList<Integer> list = newArrayList(1, 2, 3);

        assertDoesNotThrow(() -> checkAllTrue(list, i -> i > -1));
    }

    @Test
    public void checkAllTrue_rejects_iterable() {

        ArrayList<Integer> list = newArrayList(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> checkAllTrue(list, i -> i < 2));
    }

    @Test
    public void checkAllFalse_accepts_iterable() {
        ArrayList<Integer> list = newArrayList(1, 2, 3);

        assertDoesNotThrow(() -> checkAllFalse(list, i -> i < -1));
    }

    @Test
    public void checkAllFalse_rejects_iterable() {

        ArrayList<Integer> list = newArrayList(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> checkAllFalse(list, i -> i < 2));
    }

    @Test
    public void checkAllMatch_accept_iterable() {

        ArrayList<String> list = newArrayList("A", "A", "A");

        assertDoesNotThrow(() -> checkAllMatch(list, str -> str));
    }

    @Test
    public void checkAllMatch_mappedTypeOverrideEquals() {

        List<OverriddenEquals> trickyItems = newArrayList(
                new OverriddenEquals("A", Color.RED),
                new OverriddenEquals("A", Color.BLUE),
                new OverriddenEquals("A", Color.YELLOW));

        // Notice, these items are all "equal"
        assertThat(trickyItems.get(0), is(trickyItems.get(1)));
        assertThat(trickyItems.get(1), is(trickyItems.get(2)));

        // checkAllMatch should reflect the mapped class's equals method
        assertDoesNotThrow(() -> checkAllMatch(trickyItems, tricky -> tricky));
    }

    static class OverriddenEquals {
        String str; // matters to overridden equals
        Color color; // DOES NOT matter to overridden equals

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.str);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OverriddenEquals other = (OverriddenEquals) obj;
            if (!Objects.equals(this.str, other.str)) {
                return false;
            }
            return true;
        }

        OverriddenEquals(String str, Color clr) {
            this.str = str;
            this.color = clr;
        }
    }
}
