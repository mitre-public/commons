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
 * This is a general purpose convenience class that should be used exceedingly sparingly. Always
 * consider replacing uses of Triple with well-named classes. For example, replacing a
 * {@code Triple<List<Person>,Instant, String>} with a "MeetingInvite" class will dramatically
 * improve the readability of the code.
 * <p>
 * Note: Serializing a {@literal Triple<A,B,C>} will fail if either A, B, or C do not implement
 * Serializable. This implementation does not require Am B, or C to be Serializable because it is
 * more important to support "Grouping" non-serializable objects than it is to ensure Serializing a
 * Triple never fails. It is a user mistake to attempt to Serialize a {@literal Triple<A,B,C>} when
 * either A, B, or C are non-serializable.
 * <p>
 * Similarly, it is also a mistake to serialize an {@literal ArrayList<T>} when T is
 * non-serializable. Despite this vulnerability it is useful for ArrayList, and Triple, to be
 * Serializable.
 * <p>
 * This class is Deprecated to encourage usage of Java Record classes. This class may or may not be
 * removed in the future.
 */
@Deprecated
public class Triple<A, B, C> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final A first;
    private final B second;
    private final C third;

    public Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <T, U, V> Triple<T, U, V> of(T first, U second, V third) {
        return new Triple<>(first, second, third);
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }

    public C third() {
        return third;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.first);
        hash = 23 * hash + Objects.hashCode(this.second);
        hash = 23 * hash + Objects.hashCode(this.third);
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
        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
        if (!Objects.equals(this.first, other.first)) {
            return false;
        }
        if (!Objects.equals(this.second, other.second)) {
            return false;
        }
        if (!Objects.equals(this.third, other.third)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{" + first.toString() + "," + second.toString() + "," + third.toString() + "}";
    }
}
