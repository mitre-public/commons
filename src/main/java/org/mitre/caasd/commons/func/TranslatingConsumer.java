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

package org.mitre.caasd.commons.func;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A TranslatingConsumer converts a {@code Consumer<AFTER>} into a {@code Consumer<BEFORE>}.
 *
 * <p>This is achieved by decorating a {@code Consumer<AFTER>} with a preceding "translation
 * function" {@code Function<BEFORE, AFTER>} that intercepts inputs of type BEFORE and converts them
 * to instances of type AFTER that are then fed into the inner decorated consumer.
 *
 * @param <BEFORE> The type before the translation step (i.e. the upstream type)
 * @param <AFTER> The type after the translation step (i.e. the downstream type)
 */
public class TranslatingConsumer<BEFORE, AFTER> implements Consumer<BEFORE> {

    private final Function<BEFORE, AFTER> translator;

    private final Consumer<AFTER> downStream;

    /**
     * Wrap a downStream consumer with a preceding "type translation step".  This allows the
     * downStream Consumer<AFTER> to masquerade as a Consumer<BEFORE>
     *
     * @param translator Converts inputs from the "BEFORE" type to the "AFTER" type (and sends them
     *                   to the downStream consumer)
     * @param downStream Receive the "post translation" type
     */
    public TranslatingConsumer(Function<BEFORE, AFTER> translator, Consumer<AFTER> downStream) {
        this.translator = requireNonNull(translator);
        this.downStream = requireNonNull(downStream);
    }

    public static <IN, OUT> TranslatingConsumer<IN, OUT> of(Function<IN, OUT> translator, Consumer<OUT> downStream) {
        return new TranslatingConsumer<>(translator, downStream);
    }

    @Override
    public void accept(BEFORE input) {
        AFTER item = translator.apply(input);
        downStream.accept(item);
    }
}
