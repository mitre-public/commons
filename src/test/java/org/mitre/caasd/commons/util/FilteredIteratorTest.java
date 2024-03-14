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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.Functions.ALWAYS_TRUE;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class FilteredIteratorTest {

    @Test
    public void basicOperationWorks() {

        ArrayList<Integer> list = newArrayList(10, 200, 201, 5, -1, 8);

        FilteredIterator<Integer> iter = new FilteredIterator<>(list.iterator(), number -> number < 20);

        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(10));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(5));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(-1));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(8));
        assertThat(iter.hasNext(), is(false));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void nextFailsWhenHasNextIsFalse() {

        ArrayList<Integer> list = newArrayList();

        FilteredIterator<Integer> iter = new FilteredIterator(list.iterator(), ALWAYS_TRUE);

        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void removeIsNotSupported() {
        ArrayList<Integer> list = newArrayList();

        FilteredIterator<Integer> iter = new FilteredIterator(list.iterator(), ALWAYS_TRUE);

        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }
}
