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
import java.util.Objects;

/**
 * This is a general purpose convenience class that should be used sparingly. Always consider
 * replacing uses of Pair with well-named classes. For example, systematically using LatLong in
 * place of {@code Pair<Double,Double>} will dramatically improve the readability of a code-base.
 * <p>
 * Note: Serializing a {@literal Pair<L,R>} will fail if either L or R do not implement
 * Serializable. This implementation does not require L and R to be Serializable because it is more
 * important to support "Pairing" non-serializable objects than it is to ensure Serializing a Pair
 * never fails. It is a user mistake to attempt to Serialize a {@literal Pair<L,R>} when either L or
 * R are non-serializable.
 * <p>
 * Similarly, it is also a mistake to serialize an {@literal ArrayList<T>} when T is
 * non-serializable. Despite this vulnerability it is useful for ArrayList, and Pair, to be
 * Serializable.
 * <p>
 * This class is Deprecated to encourage usage of Java Record classes. This class may or may not be
 * removed in the future.
 */
@Deprecated
public class Pair<L, R> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final L first;
    private final R second;

    public Pair(L first, R second) {
        this.first = first;
        this.second = second;
    }

    public static <T, U> Pair<T, U> of(T first, U second) {
        return new Pair<>(first, second);
    }

    public L first() {
        return first;
    }

    public R second() {
        return second;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.first);
        hash = 23 * hash + Objects.hashCode(this.second);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (!Objects.equals(this.first, other.first)) {
            return false;
        }
        if (!Objects.equals(this.second, other.second)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{" + first.toString() + "," + second.toString() + "}";
    }
}
