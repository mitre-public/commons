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

package org.mitre.caasd.commons.out;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.out.Sinks.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class SinksTest {

    @Test
    public void noOpSinkWorks() {

        OutputSink<Integer> sink = noOpSink();

        sink.accept(5);
        sink.accept(15);
    }

    @Test
    public void collectionSinkWorks() {

        CollectionSink<Integer> sink = collectionSink(newArrayList());

        sink.accept(5);
        sink.accept(15);

        List<Integer> col = (List<Integer>) sink.collection();

        assertThat(col.get(0), is(5));
        assertThat(col.get(1), is(15));
    }
}
