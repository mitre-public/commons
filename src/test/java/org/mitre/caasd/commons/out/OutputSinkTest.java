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

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mitre.caasd.commons.out.OutputSink.combine;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class OutputSinkTest {

    static Map<String, String> MAP = newHashMap();

    // leave "bread crumbs" in the MAP when these methods are called
    static class EvntDest_1 implements OutputSink<JsonWritable> {

        @Override
        public void accept(JsonWritable event) {
            MAP.put("ED1", "accept");
        }

        @Override
        public void close() {
            MAP.put("ED1", "close");
        }
    }

    // leave "bread crumbs" in the MAP when these methods are called
    static class EvntDest_2 implements OutputSink<JsonWritable> {

        @Override
        public void accept(JsonWritable event) {
            MAP.put("ED2", "accept");
        }

        @Override
        public void close() {
            MAP.put("ED2", "close");
        }
    }

    @Test
    public void andThenForwardsCallsToBothComponents() throws IOException {

        OutputSink<JsonWritable> one = new EvntDest_1();
        OutputSink<JsonWritable> two = new EvntDest_2();

        OutputSink<JsonWritable> combination = one.andThen(two);

        MAP.clear();

        combination.accept(null);
        assertThat(MAP, hasEntry("ED1", "accept"));
        assertThat(MAP, hasEntry("ED2", "accept"));

        combination.close();
        assertThat(MAP, hasEntry("ED1", "close"));
        assertThat(MAP, hasEntry("ED2", "close"));
    }

    @Test
    public void combine_forwardsCallsToBothComponents() throws IOException {

        OutputSink<JsonWritable> combination = combine(new EvntDest_1(), new EvntDest_2());

        MAP.clear();

        combination.accept(null);
        assertThat(MAP, hasEntry("ED1", "accept"));
        assertThat(MAP, hasEntry("ED2", "accept"));

        combination.close();
        assertThat(MAP, hasEntry("ED1", "close"));
        assertThat(MAP, hasEntry("ED2", "close"));
    }
}
