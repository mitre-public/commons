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

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A CountingConsumer decorates the provided Consumer with a counting mechanic.
 */
public class CountingConsumer<T> implements Consumer<T>, Serializable {

    private static final long serialVersionUID = 7876428916387834743L;

    private int numCallsToAccept = 0;

    private final Consumer<T> wrappedConsumer;

    public CountingConsumer(Consumer<T> consumer) {
        this.wrappedConsumer = consumer;
    }

    @Override
    public void accept(T t) {
        numCallsToAccept++;
        wrappedConsumer.accept(t);
    }

    public int numCallsToAccept() {
        return numCallsToAccept;
    }

    public Consumer<T> innerConsumer() {
        return wrappedConsumer;
    }
}
