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

import java.lang.reflect.Field;
import java.util.Map;

/**
 * This is a dangerous utility that allows you to forcibly change Immutable environment variables
 * using reflection magic.
 * <p>
 * Use this class with care, also be sure to reset the environment variables when finished testing
 */
public class EnvironmentVariableForcer {
    // see:
    // https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/test/java/utils/EnvironmentVariableHelper.java

    private final Map<String, String> originalEnvironmentVariables;
    private final Map<String, String> modifiableMap;
    private volatile boolean mutated = false;

    @SuppressWarnings("unchecked")
    public EnvironmentVariableForcer() {
        try {
            originalEnvironmentVariables = System.getenv();
            Field f = originalEnvironmentVariables.getClass().getDeclaredField("m");
            f.setAccessible(true);
            modifiableMap = (Map<String, String>) f.get(originalEnvironmentVariables);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String key, String value) {
        mutated = true;
        modifiableMap.put(key, value);
    }

    public void remove(String key) {
        mutated = true;
        modifiableMap.remove(key);
    }

    public void reset() {
        if (mutated) {
            synchronized (this) {
                if (mutated) {
                    modifiableMap.clear();
                    modifiableMap.putAll(originalEnvironmentVariables);
                    mutated = false;
                }
            }
        }
    }

    @Override
    protected void finalize() {
        reset();
    }
}
