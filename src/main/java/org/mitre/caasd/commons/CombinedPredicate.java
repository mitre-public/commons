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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.caasd.commons.util.Preconditions.checkNoNullElement;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class CombinedPredicate<T> implements Predicate<T> {

    private final List<Predicate<T>> components;

    private final Predicate<T> combination;

    @SafeVarargs
    public CombinedPredicate(Predicate<T>... requirements) {
        checkNoNullElement(requirements);
        this.components = newArrayList(requirements);
        this.combination = combine(requirements);
    }

    @Override
    public boolean test(T t) {
        return combination.test(t);
    }

    public List<Predicate<T>> components() {
        return Collections.unmodifiableList(components);
    }

    @SafeVarargs
    public static <T> Predicate<T> combine(Predicate<T>... predicates) {
        checkNotNull(predicates);
        checkArgument(predicates.length > 0);

        Predicate<T> workingPred = predicates[0];

        for (int i = 1; i < predicates.length; i++) {
            workingPred = workingPred.and(predicates[i]);
        }

        return workingPred;
    }
}
