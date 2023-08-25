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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import org.mitre.caasd.commons.util.PropertyUtils.MissingPropertyException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

/**
 * An ImmutableConfig provides methods to load and access config data stored in flat files. The goal
 * of this class is to improve upon java.util.Properties by making interacting with property files
 * (aka text files) programmatically useful, unmistakably clear, and less error-prone.
 * <p>
 * The first flaw ImmutableConfig solves is making Properties simple to load. Creating a "loaded"
 * java.util.Properties object requires (1) creating an empty Properties object, (2) Finding the
 * corresponding text file, (3) creating a FileInputStream to read the text file, and (4) loading
 * the asProperties. All 4 of these steps must be handled individually and some can cause checked
 * Exceptions that litter the code with try/catch blocks. Consequently, creating a usable Properties
 * object requires lots of busy work development. ImmutableConfig removes the need to handle these
 * annoying creation steps by providing multiple constructors that do this work for you.
 * <p>
 * Another defect java.util.Properties has is that retrieving asProperties can be error prone. For
 * example, Strings returned by "getProperty(String key)" can contain trailing whitespace from the
 * source text file. Additionally, there is no default mechanism that throws Exceptions when a
 * required property is missing. Consequently, it is easy to accidentally create or use an
 * incomplete of otherwise malformed Properties object.
 * <p>
 * ImmutableConfig addresses both of these shortcomings. Retrieved asProperties are automatically
 * trimmed of whitespace and querying missing asProperties produces MissingPropertyExceptions as
 * opposed to merely returning null.
 * <p>
 * ImmutableConfig does not have methods that change property values. Consequently, ImmutableConfig
 * objects can be safely shared without worrying about concurrent state changes.
 * <p>
 * <B> This class cannot be extended.
 * <p>
 * </B> New config classes should store data in an ImmutableConfig and then add well named methods
 * like:
 * <pre>public String propertyA() {...}</pre>
 * <pre>public boolean propertyB(){...}</pre>
 * <pre>public int propertyC(){...}</pre> Where the asProperties A, B, and C have useful names like
 * "username()", "removeDuplicates()", or "maxAllowableCount()". This convention encourages readable
 * code like:
 * <pre> if(config.shouldPublishResults()) {
 *   publishResults();
 * }
 * </pre>
 * ImmutableConfig (which is designed for encapsulation) is preferable to QuickProperties (which is
 * designed for extension).
 */
public final class ImmutableConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableMap<String, String> propertyMap;

    /**
     * Create an ImmutableConfig by loading the asProperties found in a File
     *
     * @param propertiesFile A text file
     * @param requiredProps  The asProperties that must exist for this ImmutableConfig object to be
     *                       properly defined (i.e. it is an error to miss one of these
     *                       asProperties)
     */
    public ImmutableConfig(File propertiesFile, Collection<String> requiredProps) {
        this(
            loadProperties(propertiesFile),
            requireNonNull(requiredProps, "The requiredProps cannot be null")
        );
    }

    /**
     * Create an ImmutableConfig by loading the asProperties found in a File
     *
     * @param propertiesFile A text file
     */
    public ImmutableConfig(File propertiesFile) {
        this(loadProperties(propertiesFile));
    }

    public ImmutableConfig(Properties props) {
        this(props, emptyList());
    }

    public ImmutableConfig(Properties props, Collection<String> requiredPropertyKeys) {
        this(asImmutableMap(props), requiredPropertyKeys);
    }

    public ImmutableConfig(Map<String, String> properties) {
        this(properties, emptyList());
    }

    /**
     * Create an ImmutableConfig directly from a KV collection of properties
     *
     * @param properties           A String-String map representing a collection of KV properties
     * @param requiredPropertyKeys The map keys which must exist for this ImmutableConfig object to
     *                             be properly defined.
     */
    public ImmutableConfig(Map<String, String> properties, Collection<String> requiredPropertyKeys) {
        requireNonNull(properties);
        requireNonNull(requiredPropertyKeys);
        this.propertyMap = ImmutableMap.copyOf(properties);
        verifyKeysExist(requiredPropertyKeys);
    }


    /**
     * Eagerly validate this config by confirming the existence of these required property keys.
     *
     * @param requiredPropertyKeys keys this ImmutableConfig must contain
     */
    public void verifyKeysExist(Collection<String> requiredPropertyKeys) {
        requiredPropertyKeys.forEach(this::getRequired);
    }

    /**
     * Retrieve a required property from the immutable properties map. Throw an Exception if the
     * property cannot be found. Also trim all whitespace from the returned result.
     *
     * @param propertyName The key to use when accessing the property map
     *
     * @return The corresponding property
     * @throws MissingPropertyException if this property is missing from the PROPERTIES object.
     */
    private String getRequired(String propertyName) {
        String value = propertyMap.get(propertyName);
        if (value != null) {
            return value.trim();
        } else {
            throw new MissingPropertyException(propertyName);
        }
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public String getString(String propertyName) {
        return getRequired(propertyName);
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public byte getByte(String propertyName) {
        return Byte.parseByte(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public short getShort(String propertyName) {
        return Short.parseShort(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public int getInt(String propertyName) {
        return Integer.parseInt(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public long getLong(String propertyName) {
        return Long.parseLong(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public float getFloat(String propertyName) {
        return Float.parseFloat(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public double getDouble(String propertyName) {
        return Double.parseDouble(getRequired(propertyName));
    }

    /** Retrieve a required property, Throw a MissingPropertyException if the property is not found. */
    public boolean getBoolean(String propertyName) {
        return Boolean.parseBoolean(getRequired(propertyName));
    }

    /**
     * Retrieve a optional property from the immutable properties map. Trim all whitespace from the
     * returned result. Wrap the retrieved result in an Optional
     *
     * @param propertyName The key to use when accessing the property map
     *
     * @return An Optional containing the corresponding property if it existed
     */
    private Optional<String> getOptionalProperty(String propertyName) {
        String value = propertyMap.get(propertyName);
        if (value != null) {
            return Optional.of(value.trim());
        } else {
            return Optional.empty();
        }
    }

    /** Retrieves an optional property. */
    public Optional<String> getOptionalString(String propertyName) {
        return getOptionalProperty(propertyName);
    }

    /** Retrieves an optional property. */
    public Optional<Byte> getOptionalByte(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Byte::parseByte);
    }

    /** Retrieves an optional property. */
    public Optional<Short> getOptionalShort(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Short::parseShort);
    }

    /** Retrieves an optional property. */
    public Optional<Integer> getOptionalInt(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Integer::parseInt);
    }

    /** Retrieves an optional property. */
    public Optional<Long> getOptionalLong(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Long::parseLong);
    }

    /** Retrieves an optional property. */
    public Optional<Float> getOptionalFloat(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Float::parseFloat);
    }

    /** Retrieves an optional property. */
    public Optional<Double> getOptionalDouble(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Double::parseDouble);
    }

    /** Retrieves an optional property. */
    public Optional<Boolean> getOptionalBoolean(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);
        return prop.map(Boolean::parseBoolean);
    }

    public static ImmutableMap<String, String> asImmutableMap(Properties props) {
        requireNonNull(props, "The input properties cannot be null");

        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            builder.put(key.trim(), value.trim());
        }

        return builder.build();
    }

    /**
     * @return A ImmutableSortedSet that contains all the asProperties. A ImmutableSortedSet is
     *     returned so that (1) entries are sorted alphabetically by the property name (i.e. the
     *     key), and (2) the returned set does introduce mutability.
     */
    public ImmutableSortedSet<Entry<String, String>> entrySet() {

        Comparator<Entry<String, String>> comparator
            = Entry.comparingByKey();

        return ImmutableSortedSet
            .orderedBy(comparator)
            .addAll(propertyMap.entrySet())
            .build();
    }

    /** @return The Raw mapping of Keys to Values. */
    public ImmutableMap<String, String> rawMapping() {
        return this.propertyMap;
    }

    /**
     * @return Collapse this String-String property mapping to a single String. The output String is
     *     designed to look like the contents of a flat text file. This means that each {@literal
     *     Entry<String, String>} is written to a separate line and the key and value are delimited
     *     by " : " (the colon with a leading and trailing space char)
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        String SPACING = " : ";

        for (Map.Entry<String, String> entry : entrySet()) {
            String propertyName = entry.getKey();
            String value = entry.getValue();

            sb.append(propertyName).append(SPACING).append(value).append("\n");
        }

        return sb.toString();
    }

    /**
     * @return A copy of the Properties stored in this object. The returned Properties object can be
     *     manipulated.
     */
    public Properties asProperties() {
        Properties props = new Properties();
        for (Entry<String, String> entry : propertyMap.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        return props;
    }
}