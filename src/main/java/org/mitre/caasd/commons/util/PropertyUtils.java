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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Byte.parseByte;
import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.Short.parseShort;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.mitre.caasd.commons.fileutil.FileUtils;

import com.google.common.collect.ImmutableMap;

/**
 * PropertyUtils provides convenient methods to load and access configuration data stored in flat
 * "Properties" files. These static methods reduce the development effort required use flat property
 * files (i.e. text files) and java.util.Properties objects. These methods addresses some of the
 * core flaws in java.util.Properties.
 * <p>
 * The first flaw solved is making Properties easy to load. "Loading" a java.util.Properties object
 * requires (1) creating an empty Properties object, (2) Finding the corresponding text file, (3)
 * creating a FileInputStream to read the text file, and (4) loading the properties. All 4 of these
 * steps must be handled individually and some can cause checked Exceptions that litter the code
 * with try/catch blocks. PropertyUtils removes the need to handle these annoying creation steps by
 * providing multiple convenience methods that do this work for you.
 * <p>
 * Another defect of java.util.Properties is that retrieving properties can be error prone. For
 * example, Strings returned by "getProperty(String key)" can contain trailing whitespace from the
 * source text file. Additionally, there is no default mechanism that throws Exceptions when a
 * required property is missing. Consequently, it is easy to accidentally create or use an
 * incomplete of otherwise malformed Properties object.
 * <p>
 * The methods in PropertyUtils addresses these shortcomings because retrieved properties are
 * automatically trimmed of whitespace and missing properties throw MissingPropertyExceptions as
 * opposed to merely returning null.
 * <p>
 * Together, these methods facilitate building a clean, facade API around a raw Properties
 * object. For example, the goal is to make writing methods like "shouldPublishResults()" and
 * "maxAttempts()" simple one-line methods rather than nests of String parsing and error trapping.
 */
public class PropertyUtils {

    /**
     * Load a Properties object from a text file. Note: This method ensures all properties defined
     * in the source text files are defined EXACTLY once.
     *
     * @param pathToTextFile The path to a File containing text data
     *
     * @return The Properties that were extracted from this file
     */
    public static Properties loadProperties(String pathToTextFile) {
        checkNotNull(pathToTextFile, "The path to a properties file cannot be null");
        return loadProperties(new File(pathToTextFile));
    }

    /**
     * Load a Properties object from a text file. Note: This method ensures all properties defined
     * in the source text files are defined EXACTLY once.
     *
     * @param propertiesFile A File containing text data
     *
     * @return The Properties that were extracted from this file
     */
    public static Properties loadProperties(File propertiesFile) {
        checkNotNull(propertiesFile, "The input propertiesFile cannot be null");
        checkArgument(propertiesFile.exists(), "The file: " + propertiesFile.getName() + " does not exist");
        try {
            return FileUtils.getProperties(propertiesFile);
        } catch (IOException ex) {
            throw new PropertyLoadingException(
                    "Problem loading Properties file: " + propertiesFile.getAbsolutePath() + "\n" + ex.getMessage(),
                    ex);
        }
    }

    public static ImmutableMap<String, String> asImmutableMap(Properties props) {
        checkNotNull(props, "The input properties cannot be null");

        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            builder.put(key.trim(), value.trim());
        }

        return builder.build();
    }

    /**
     * Parse a Properties object out of a String
     *
     * @param allProperties A String containing multiple properties. For example the String
     *                      "property1 = 17\nproperty2 : value\nproperty3 = anotherValue\n" defines
     *                      a Property object with 3 properties (property1, property2, property3)
     *
     * @return The Properties found in the String
     */
    public static Properties parseProperties(String allProperties) {
        checkNotNull(allProperties, "Cannot parse a null input String");

        Properties props = new Properties();
        try {
            props.load(new StringReader(allProperties));
        } catch (IOException ioe) {
            throw new PropertyLoadingException("Problem parsing properties from: " + allProperties, ioe);
        }

        return props;
    }

    /**
     * Throw a MissingPropertyException if the key is missing.
     *
     * @param props               A Properties object
     * @param requiredPropertyKey All of these keys must be set in the provided Properties object
     *
     * @throws MissingPropertyException if the required property is missing
     */
    public static void verifyPropertyIsSet(Properties props, String requiredPropertyKey) {
        getString(requiredPropertyKey, props);
    }

    /**
     * Verify that every one of these properties is set in the provided Properties object. Throw a
     * MissingPropertyException if a key is missing.
     *
     * @param props                A Properties object
     * @param requiredPropertyKeys All of these keys must be set in the provided Properties object
     *
     * @throws MissingPropertyException if the required property is missing
     */
    public static void verifyPropertiesAreSet(Properties props, String... requiredPropertyKeys) {
        for (String key : requiredPropertyKeys) {
            getString(key, props);
        }
    }

    /**
     * Verify that every one of these properties is set in the provided Properties object. Throw a
     * MissingPropertyException if a key is missing.
     *
     * @param props                A Properties object
     * @param requiredPropertyKeys All of these keys must be set in the provided Properties object
     *
     * @throws MissingPropertyException if the required property is missing
     */
    public static void verifyPropertiesAreSet(Properties props, Collection<String> requiredPropertyKeys) {
        for (String key : requiredPropertyKeys) {
            getString(key, props);
        }
    }

    /**
     * Retrieve a required property from a Properties object. This method throws a
     * MissingPropertyException if the required property is not in the provided properties file.
     * <p>
     * The purpose of this method is to simplify property extraction by (1) providing better
     * warning/exceptions, (2) automatically removing whitespace, and (3) make code intent clear.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object
     *
     * @return The requested property that has been trimmed of any whitespace.
     * @throws MissingPropertyException if the required property is missing
     */
    public static String getString(String propertyKey, Properties properties) {
        checkNotNull(propertyKey);
        checkNotNull(properties);
        String value = properties.getProperty(propertyKey);
        if (value != null) {
            return value.trim();
        } else {
            throw new MissingPropertyException(propertyKey);
        }
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static byte getByte(String propertyKey, Properties properties) {
        return parseByte(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static short getShort(String propertyKey, Properties properties) {
        return parseShort(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static int getInt(String propertyKey, Properties properties) {
        return parseInt(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static long getLong(String propertyKey, Properties properties) {
        return parseLong(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static float getFloat(String propertyKey, Properties properties) {
        return parseFloat(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static double getDouble(String propertyKey, Properties properties) {
        return parseDouble(getString(propertyKey, properties));
    }

    /**
     * Retrieve a value from a Properties object and parse it to the appropriate type. This method
     * throws a MissingPropertyException if the required propertyKey is not found in the Properties
     * object.
     * <p>
     * This method simplify property extraction by (1) providing better warning/exceptions, (2)
     * making code intent clear, (3) automatically casting String values to the desired types, and
     * (4) automatically removing whitespace.
     *
     * @param propertyKey A key for a property that MUST be in the properties object
     * @param properties  A Properties object that stores String-String Key-Value pairs
     *
     * @return The requested property, trimmed, parsed, and cast appropriately.
     * @throws MissingPropertyException if the required property is missing
     */
    public static boolean getBoolean(String propertyKey, Properties properties) {
        checkNotNull(propertyKey);
        checkNotNull(properties);
        return parseBoolean(getString(propertyKey, properties));
    }

    /**
     * Retrieve an optional property from a Properties object. Trim all whitespace from the returned
     * result. Wrap the retrieved result in an Optional.
     * <p>
     * The purpose of this method is to simplify property extraction by (1) providing better
     * warning/exceptions, (2) automatically removing whitespace, and (3) make code intent clear.
     *
     * @param propertyKey A key for a property that may or may not be in the properties object
     * @param properties  A Properties object
     *
     * @return An Optional containing the corresponding property trimmed of any whitespace (if it
     *     exists)
     */
    public static Optional<String> getOptionalString(String propertyKey, Properties properties) {
        String value = properties.getProperty(propertyKey);
        if (value != null) {
            return Optional.of(value.trim());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieve an optional property from a Properties object. Trim all whitespace from the returned
     * result. If the property does not exist return the default value.
     * <p>
     * The purpose of this method is to simplify property extraction by (1) providing better
     * warning/exceptions, (2) automatically removing whitespace, and (3) make code intent clear.
     *
     * @param propertyKey  A key for a property that may or may not be in the properties object
     * @param properties   A Properties object
     * @param defaultValue The value returned if the propertyKey is missing.
     *
     * @return The request property trimmed of any whitespace or the default value if the requested
     *     property is missing.
     */
    public static String getOptionalString(String propertyKey, Properties properties, String defaultValue) {
        String value = properties.getProperty(propertyKey);
        if (value != null) {
            return value.trim();
        } else {
            return defaultValue;
        }
    }

    public static byte getOptionalByte(String propertyKey, Properties properties, byte defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Byte.parseByte(prop.get()) : defaultValue;
    }

    public static short getOptionalShort(String propertyKey, Properties properties, short defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Short.parseShort(prop.get()) : defaultValue;
    }

    public static int getOptionalInt(String propertyKey, Properties properties, int defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Integer.parseInt(prop.get()) : defaultValue;
    }

    public static long getOptionalLong(String propertyKey, Properties properties, long defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Long.parseLong(prop.get()) : defaultValue;
    }

    public static float getOptionalFloat(String propertyKey, Properties properties, float defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Float.parseFloat(prop.get()) : defaultValue;
    }

    public static double getOptionalDouble(String propertyKey, Properties properties, double defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Double.parseDouble(prop.get()) : defaultValue;
    }

    public static boolean getOptionalBoolean(String propertyKey, Properties properties, boolean defaultValue) {

        Optional<String> prop = getOptionalString(propertyKey, properties);

        return prop.isPresent() ? Boolean.parseBoolean(prop.get()) : defaultValue;
    }

    /**
     * Tokenize a String and verify that every token in the input String appears in the collection
     * of "valid tokens". Then returns a list of the tokens found.
     *
     * @param csvLine     A line of comma separated tokens
     * @param validTokens The set of tokens which are permitted to appear in this line
     *
     * @return An list of the tokens found while parsing (list elements are ordered according to
     *     appearance in input String)
     */
    public static List<String> tokenizeAndValidate(String csvLine, Collection<String> validTokens) {
        checkNotNull(validTokens);

        csvLine = csvLine.trim();

        if (csvLine.isEmpty()) {
            return newArrayList();
        }

        // Tokenize, trim, and then verify the comma delimited tokens are valid
        String[] splits = csvLine.split(",");
        for (int i = 0; i < splits.length; i++) {
            splits[i] = splits[i].trim();
            checkArgument(validTokens.contains(splits[i]), "Invalid token: " + splits[i] + " found in: " + csvLine);
        }

        return newArrayList(splits);
    }

    /**
     * This Exception class permits loading properties without try/catch blocks by converting
     * checked IOException to RuntimeExceptions
     */
    public static class PropertyLoadingException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        PropertyLoadingException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static class MissingPropertyException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public MissingPropertyException(String nameOfMissingProperty) {
            super("The property " + nameOfMissingProperty + " is missing");
        }
    }
}
