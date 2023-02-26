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

/**
 * A CompositeTranslator combines two translators into a single translator.
 *
 * @param <A> The starting type
 * @param <B> The intermediate type (this type is only relevant when calling the constructor)
 * @param <C> The ending type
 */
public class CompositeTranslator<A, B, C> implements Translator<A, C> {

    private final Translator<A, B> step1;
    private final Translator<B, C> step2;

    public CompositeTranslator(Translator<A, B> step1, Translator<B, C> step2) {
        this.step1 = step1;
        this.step2 = step2;
    }

    @Override
    public C to(A item) {
        B middle = step1.to(item);
        C result = step2.to(middle);
        return result;
    }

    @Override
    public A from(C item) {
        B middle = step2.from(item);
        A result = step1.from(middle);
        return result;
    }
}
