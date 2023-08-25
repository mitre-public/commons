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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class CompositeConsumer<T> implements Consumer<T> {

    final List<Consumer<T>> consumers;

    public CompositeConsumer(Consumer<T> oneConsumer) {
        this.consumers = new LinkedList<>();
        this.consumers.add(oneConsumer);
    }

    @SafeVarargs
    public CompositeConsumer(Consumer<T>... arrayOfConsumers) {
        this.consumers = new LinkedList<>();
        this.consumers.addAll(Arrays.asList(arrayOfConsumers));
    }

    @Override
    public void accept(T t) {
        for (Consumer<T> consumer : consumers) {
            consumer.accept(t);
        }
    }
}
