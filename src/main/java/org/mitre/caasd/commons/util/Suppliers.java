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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;
import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * The Suppliers in this class can be used by themselves but they are designed to be part of a
 * SupplierChain that "injects assets" without hardcoding exactly where that asset must come from.
 */
public class Suppliers {

    /**
     * @param propertyKey The key that is searched for
     * @param flatFile    An optional File that may contain the propertyKey (can be null if you
     *                    don't want to support File Based look-up)
     * @return a SupplierChain that sequentially searches: Environment Variables, System Properties,
     * and then a flat text file (java Property formatting) until is finds the requested
     * propertyKey
     */
    @SuppressWarnings("unchecked")
    public static SupplierChain<String> stringSupplierChain(String propertyKey, File flatFile) {
        return SupplierChain.of(
            environmentVarSupplier(propertyKey),
            systemPropertySupplier(propertyKey),
            fileBasedSupplier(flatFile, propertyKey)
        );
    }

    /** @return A Supplier that searches the environment variables for a specific key. */
    public static Supplier<String> environmentVarSupplier(String key) {
        return new EnvironmentVarSupplier(key);
    }

    /** @return A Supplier that searches Java System Properties for a specific key. */
    public static Supplier<String> systemPropertySupplier(String key) {
        return new SystemPropertiesSupplier(key);
    }

    /** @return A Supplier that parses a file, searches for a specific key, and returns the value. */
    public static Supplier<String> fileBasedSupplier(File f, String key) {
        return new FileBasedSupplier(f, key);
    }

    /** Pulls a Key-Value pair from Environment Vars (e.g. System.getenv(key)). */
    public static class EnvironmentVarSupplier implements Supplier<String> {

        // See https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/EnvironmentVariableCredentialsProvider.java
        // That is a similar class that only provides AWSCredentials.
        // This is a more general solution

        private final String propertyKey;
        private boolean queryWasRun;
        private String propertyValue;

        public EnvironmentVarSupplier(String key) {
            requireNonNull(key, "The System property search key cannot be null");
            this.propertyKey = key;
            this.queryWasRun = false;
            this.propertyValue = null;
        }

        @Override
        public String get() {
            if (queryWasRun) {
                return propertyValue;
            }

            String val = System.getenv(propertyKey);

            this.queryWasRun = true;
            this.propertyValue = nonNull(val)
                ? val.trim()
                : null;

            return propertyValue;
        }
    }

    /** Pulls a Key-Value pair from Java System Properties (e.g. System.getProperty(key)). */
    public static class SystemPropertiesSupplier implements Supplier<String> {

        // See https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/SystemPropertiesCredentialsProvider.java
        // That is a similar class that only provides AWSCredentials,
        // This is a more general solution

        private final String propertyKey;
        private boolean queryWasRun;
        private String propertyValue;

        public SystemPropertiesSupplier(String key) {
            requireNonNull(key, "The System property search key cannot be null");
            this.propertyKey = key;
            this.queryWasRun = false;
            this.propertyValue = null;
        }

        @Override
        public String get() {
            if (queryWasRun) {
                return propertyValue;
            }

            //Do not throw an NPE if "value" is null.  We may rely on this behavior if we want to then
            String val = System.getProperty(propertyKey);

            this.queryWasRun = true;
            this.propertyValue = nonNull(val)
                ? val.trim()
                : null;

            return propertyValue;
        }
    }


    /** Pulls a Key-Value pair from a flat property File (see java.util.Properties). */
    public static class FileBasedSupplier implements Supplier<String> {

        private final File sourceFile;
        private final String propertyKey;
        private boolean queryWasRun;
        private String propertyValue;

        public FileBasedSupplier(File sourceFile, String key) {
            /*
             * Implementation Note: Do not eagerly parse this file because that will be wasted work
             * if an earlier supplier in a SupplierChain has the asset we are looking for
             */
            requireNonNull(key);
            this.sourceFile =
                sourceFile;  //CAN BE NULL!  This allows saying "No" to File-Based support when building a SupplierChain
            this.propertyKey = key;
            this.queryWasRun = false;
            this.propertyValue = null;
        }

        @Override
        public String get() {
            if (queryWasRun) {
                return propertyValue;
            }

            Properties props = loadProperties(sourceFile);
            Optional<String> optValue = PropertyUtils.getOptionalString(propertyKey, props);

            this.queryWasRun = true;
            this.propertyValue = optValue.isPresent()
                ? optValue.get().trim()
                : null;

            return propertyValue;
        }
    }
}
