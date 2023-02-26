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
 * IterPair is short for "Iteration Neighbor Pair".  The goal of this class is to package two
 * consecutive elements from an iteration.
 */
public class IterPair<T> {
    private final T prior;
    private final T current;

    IterPair(T prior, T current) {
        this.prior = prior;
        this.current = current;
    }

    public T prior() {
        return prior;
    }

    public T current() {
        return current;
    }
}
