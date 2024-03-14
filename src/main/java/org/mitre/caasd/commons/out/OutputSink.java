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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * An OutputSink is a "place" where output data can be sent (frequently for permanently storage)
 * <p>
 * This interface has two goals.
 * <p>
 * The first goal is to allow client code to easily "plug-in" the output publication rules of their
 * choice. For example, client code may want to publish records to a Database, a Kafka Cluster, an
 * Avro archive, or merely print them to a text file or System.out. In any case, the code that
 * creates records should be cleanly separated from the code that publishes/archives those records.
 * <p>
 * The second goal is to make client code more readable. The term OutputSink is easier to understand
 * than a {@code Consumer<T>}
 */
public interface OutputSink<T> extends Consumer<T>, Closeable, Flushable {

    /**
     * Push this data record to the output sink (ie send the record to a file, database, etc).
     *
     * @param record An record that needs to be handled.
     */
    @Override
    void accept(T record);

    /**
     * Flushes any records temporarily stored by this OutputSink (some implementations may choose
     * to aggregate multiple records before interacting with a persistent data-storage system.
     */
    @Override
    default void flush() throws IOException {
        // by default do nothing.
    }

    /**
     * Do nothing by default. This method is only necessary for OutputSinks that need to leave a
     * system-resource like a network socket, database connection, or FileStream open for the sake
     * of performance.
     *
     * @throws IOException
     */
    @Override
    default void close() throws IOException {
        // by default do nothing.
    }

    default OutputSink<T> andThen(OutputSink<T> after) {
        return new CompositeSink<>(this, after);
    }

    @SafeVarargs
    static <K> OutputSink<K> combine(OutputSink<K>... destinations) {
        return new CompositeSink<>(destinations);
    }

    static <K> OutputSink<K> combine(List<OutputSink<K>> allSinks) {
        return new CompositeSink<>(allSinks);
    }

    /* Combines multiple OutputSinks into a single chain */
    class CompositeSink<K> implements OutputSink<K> {

        private final ArrayList<OutputSink<K>> destinations;

        @SafeVarargs
        private CompositeSink(OutputSink<K>... destinations) {
            checkNotNull(destinations);
            this.destinations = newArrayList(destinations);
        }

        private CompositeSink(List<OutputSink<K>> sinks) {
            checkNotNull(sinks);
            this.destinations = newArrayList(sinks);
        }

        @Override
        public void accept(K record) {
            for (OutputSink<K> destination : destinations) {
                destination.accept(record);
            }
        }

        @Override
        public void close() throws IOException {
            for (OutputSink<K> destination : destinations) {
                destination.close();
            }
        }
    }
}
