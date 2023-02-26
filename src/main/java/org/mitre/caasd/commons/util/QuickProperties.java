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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.util.PropertyUtils.asImmutableMap;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * A QuickProperties provides convenient methods to load and access property data stored in flat
 * files.
 * <p>
 * The purpose of this class is to reduce the development effort require to make a flat properties
 * file (i.e. a text file) both programmatically useful and unmistakably clear. To meet this
 * objective the QuickProperties class addresses some flaws in java.util.Properties.
 * <p>
 * The first flaw QuickProperties solves is making properties simple to load. Creating a "loaded"
 * java.util.Properties object requires (1) creating an empty Properties object, (2) Finding the
 * corresponding text file, (3) creating a FileInputStream to read the text file, and (4) loading
 * the properties. All 4 of these steps must be handled individually and some can cause checked
 * Exceptions that litter the code with try/catch blocks. Consequently, creating a usable Properties
 * object requires lots of busy work development. QuickProperties removes the need to handle these
 * annoying creation steps by providing multiple constructors that do this work for you.
 * <p>
 * Another defect a standard java.util.Properties object has is that retrieving properties can be
 * error prone. For example, Strings returned by "getProperty(String key)" can contain trailing
 * whitespace from the source text file. Additionally, there is no default mechanism that throws
 * Exceptions when a required property is missing. Consequently, it is easy to accidentally create
 * or use an incomplete of otherwise malformed Properties object.
 * <p>
 * QuickProperties addresses both of these shortcomings (retrieved properties are automatically
 * trimmed of whitespace and missing properties throw MissingPropertyExceptions as opposed to merely
 * returning null).
 * <p>
 * It is important to notice that QuickProperties does not have methods that change property values.
 * Consequently, QuickProperties objects can be safely shared without worrying about concurrent
 * state changes.
 * <p>
 * <B> This class is designed to be extended.  </B> New subclasses (i.e. extensions of
 * QuickProperties) should add well named methods like:
 * <pre>public String propertyA() {...}</pre>
 * <pre>public boolean propertyB(){...}</pre>
 * <pre>public int propertyC(){...}</pre> Where the properties A, B, and C have useful names like
 * "username()", "removeDuplicates()", or "maxAllowableCount()". This convention encourages readable
 * code like:
 * <pre> if(properties.shouldPublishResults()) {
 *   publishResults();
 * }
 * </pre>
 * <p>
 * QuickProperties require both a source of property data AND a collection of required properties.
 * If the property data does not contain the required properties then the QuickProperties object
 * cannot be created (because an Exception will be thrown).
 * <p>
 *
 * @deprecated Prefer ImmutableConfig over this class. Encouraging users to extend QuickProperties
 *     is poor practice.
 */
@Deprecated
public class QuickProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableMap<String, String> properties;

    private final ImmutableSet<String> requiredProperties;

    /**
     * Create a new QuickProperties that automatically loads the properties found in a text file
     *
     * @param pathToTextFile The path of a text file
     * @param requiredProps  The properties that must exist for this QuickProperties object to be
     *                       properly defined (i.e. it is an error to miss one of these properties)
     */
    public QuickProperties(String pathToTextFile, Collection<String> requiredProps) {
        this(loadProperties(pathToTextFile), requiredProps);
    }

    /**
     * Create a new QuickProperties that automatically loads the properties found in a File
     *
     * @param propertiesFile A text file
     * @param requiredProps  The properties that must exist for this QuickProperties object to be
     *                       properly defined (i.e. it is an error to miss one of these properties)
     */
    public QuickProperties(File propertiesFile, Collection<String> requiredProps) {
        this(loadProperties(propertiesFile), requiredProps);
    }

    /**
     * Create a new QuickProperties that archives these Properties. The primary benefit of this
     * constructor is getting access to the automatic error checking and requiring the use of new,
     * hopefully well-named, access methods.
     *
     * @param properties    A set of properties
     * @param requiredProps The properties that must exist for this QuickProperties object to be
     *                      properly defined (i.e. it is an error to miss one of these properties)
     */
    public QuickProperties(Properties properties, Collection<String> requiredProps) {
        this(
            asImmutableMap(properties),
            checkNotNull(requiredProps, "The requiredProps cannot be null")
        );
    }

    /**
     * Create a new QuickProperties that contains this set of Properties
     *
     * @param mapOfProperties These properties are copied and stored in an immutable collection.
     * @param requiredProps   The properties that must exist for this QuickProperties object to be
     *                        properly defined (i.e. it is an error to miss one of these
     *                        properties)
     */
    public QuickProperties(Map<String, String> mapOfProperties, Collection<String> requiredProps) {
        checkNotNull(mapOfProperties, "The input map cannot be null");
        this.properties = ImmutableMap.copyOf(mapOfProperties);
        this.requiredProperties = ImmutableSet.copyOf(requiredProps);
        verifyRequiredPropertiesAreSet();
    }

    private void verifyRequiredPropertiesAreSet() {
        for (String requiredProperty : requiredProperties) {
            getRequired(requiredProperty);
        }
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
    protected String getRequired(String propertyName) {
        String value = properties.get(propertyName);
        if (value != null) {
            return value.trim();
        } else {
            throw new MissingPropertyException(propertyName);
        }
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
        String value = properties.get(propertyName);
        if (value != null) {
            return Optional.of(value.trim());
        } else {
            return Optional.empty();
        }
    }

    protected String getString(String propertyName) {
        return getRequired(propertyName);
    }

    protected byte getByte(String propertyName) {
        return Byte.parseByte(getRequired(propertyName));
    }

    protected short getShort(String propertyName) {
        return Short.parseShort(getRequired(propertyName));
    }

    protected int getInt(String propertyName) {
        return Integer.parseInt(getRequired(propertyName));
    }

    protected long getLong(String propertyName) {
        return Long.parseLong(getRequired(propertyName));
    }

    protected float getFloat(String propertyName) {
        return Float.parseFloat(getRequired(propertyName));
    }

    protected double getDouble(String propertyName) {
        return Double.parseDouble(getRequired(propertyName));
    }

    protected boolean getBoolean(String propertyName) {
        return Boolean.parseBoolean(getRequired(propertyName));
    }

    protected Optional<String> getOptionalString(String propertyName) {
        return getOptionalProperty(propertyName);
    }

    protected Optional<Byte> getOptionalByte(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Byte.parseByte(prop.get()))
            : Optional.empty();
    }

    protected Optional<Short> getOptionalShort(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Short.parseShort(prop.get()))
            : Optional.empty();
    }

    protected Optional<Integer> getOptionalInt(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Integer.parseInt(prop.get()))
            : Optional.empty();
    }

    protected Optional<Long> getOptionalLong(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Long.parseLong(prop.get()))
            : Optional.empty();
    }

    protected Optional<Float> getOptionalFloat(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Float.parseFloat(prop.get()))
            : Optional.empty();
    }

    protected Optional<Double> getOptionalDouble(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Double.parseDouble(prop.get()))
            : Optional.empty();
    }

    protected Optional<Boolean> getOptionalBoolean(String propertyName) {
        Optional<String> prop = getOptionalProperty(propertyName);

        return prop.isPresent()
            ? Optional.of(Boolean.parseBoolean(prop.get()))
            : Optional.empty();
    }

    /**
     * @return A ImmutableSortedSet that contains all the properties. A ImmutableSortedSet is
     *     returned so that (1) entries are sorted alphabetically by the property name (i.e. the
     *     key), and (2) the returned set does introduce mutability.
     */
    public ImmutableSortedSet<Entry<String, String>> entrySet() {

        Comparator<Entry<String, String>> comparator
            = (o1, o2) -> o1.getKey().compareTo(o2.getKey());

        return ImmutableSortedSet
            .orderedBy(comparator)
            .addAll(properties.entrySet())
            .build();
    }

    /** @return The Raw mapping of Keys to Values. */
    public ImmutableMap<String, String> rawMapping() {
        return this.properties;
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
    public Properties properties() {
        Properties props = new Properties();
        for (Entry<String, String> entry : properties.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        return props;
    }
}
