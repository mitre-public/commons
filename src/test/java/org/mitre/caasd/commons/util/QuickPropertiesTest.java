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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceAsFile;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import org.mitre.caasd.commons.util.PropertyUtils.MissingPropertyException;

import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.Test;

public class QuickPropertiesTest {

    /* All of these properties must exist for a TestRequiredProperties to get created. */
    static List<String> requiredProps = newArrayList(
            "stringProperty",
            "byteProperty",
            "shortProperty",
            "intProperty",
            "longProperty",
            "floatProperty",
            "doubleProperty",
            "booleanProperty");

    /**
     * This is a test class that reflects the "design idea" of the QuickProperties class. Notice how
     * the properties that can be retrieved are readable from the code itself. Notice also that
     * there is no room to misspell or incorrectly capitalize the property names (in the user facing
     * api)
     * <p>
     * This class uses properties that are strictly required. Consequently, this class cannot be
     * created unless all required properties exist.
     */
    static class TestRequiredProperties extends QuickProperties {

        TestRequiredProperties(String filePath) {
            super(filePath, requiredProps);
        }

        TestRequiredProperties(File textFile) {
            super(textFile, requiredProps);
        }

        public String stringProperty() {
            return getString("stringProperty");
        }

        public byte byteProperty() {
            return getByte("byteProperty");
        }

        public short shortProperty() {
            return getShort("shortProperty");
        }

        public int intProperty() {
            return getInt("intProperty");
        }

        public long longProperty() {
            return getLong("longProperty");
        }

        public float floatProperty() {
            return getFloat("floatProperty");
        }

        public double doubleProperty() {
            return getDouble("doubleProperty");
        }

        public boolean booleanProperty() {
            return getBoolean("booleanProperty");
        }
    }

    /**
     * This is a test class that reflects the "design idea" of the QuickProperties class. Notice how
     * the properties that can be retrieved are readable from the code itself. Notice also that
     * there is no room to misspell or incorrectly capitalize the property names.
     * <p>
     * This class provides optional properties. Consequently, all "retrieval methods" return
     * Optionals.
     */
    static class TestOptionalProperties extends QuickProperties {

        TestOptionalProperties(String filePath) {
            super(filePath, newArrayList());
        }

        TestOptionalProperties(File textFile) {
            super(textFile, newArrayList());
        }

        public Optional<String> stringProperty() {
            return getOptionalString("stringProperty");
        }

        public Optional<Byte> byteProperty() {
            return getOptionalByte("byteProperty");
        }

        public Optional<Short> shortProperty() {
            return getOptionalShort("shortProperty");
        }

        public Optional<Integer> intProperty() {
            return getOptionalInt("intProperty");
        }

        public Optional<Long> longProperty() {
            return getOptionalLong("longProperty");
        }

        public Optional<Float> floatProperty() {
            return getOptionalFloat("floatProperty");
        }

        public Optional<Double> doubleProperty() {
            return getOptionalDouble("doubleProperty");
        }

        public Optional<Boolean> booleanProperty() {
            return getOptionalBoolean("booleanProperty");
        }
    }

    private TestRequiredProperties newGoodProperties() {
        Optional<File> resource = getResourceAsFile(QuickPropertiesTest.class, "goodProperties.properties");
        return new TestRequiredProperties(resource.get());
    }

    private TestRequiredProperties newEmptyProperties() {
        Optional<File> resource = getResourceAsFile(QuickPropertiesTest.class, "emptyProperties.properties");
        return new TestRequiredProperties(resource.get());
    }

    private TestOptionalProperties newGoodOptionalProerties() {
        Optional<File> resource = getResourceAsFile(QuickPropertiesTest.class, "goodProperties.properties");
        return new TestOptionalProperties(resource.get());
    }

    private TestOptionalProperties newEmptyOptionalProerties() {
        Optional<File> resource = getResourceAsFile(QuickPropertiesTest.class, "emptyProperties.properties");
        return new TestOptionalProperties(resource.get());
    }

    @Test
    public void testStringProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.stringProperty(), "value");
    }

    @Test
    public void testOptionalStringProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.stringProperty().get(), "value");
        assertFalse(emptyProperties.stringProperty().isPresent());
    }

    @Test
    public void testByteProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.byteProperty(), -12);
    }

    @Test
    public void testOptionalByteProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(-12, (byte) goodProperties.byteProperty().get());
        assertFalse(emptyProperties.byteProperty().isPresent());
    }

    @Test
    public void testShortProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.shortProperty(), 145);
    }

    @Test
    public void testOptionalShortProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertThat(goodProperties.shortProperty().get(), is((short) 145));
        assertFalse(emptyProperties.shortProperty().isPresent());
    }

    @Test
    public void testIntProperty() {

        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.intProperty(), 5);
    }

    @Test
    public void testOptionalIntProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertThat(goodProperties.intProperty().get(), is(5));
        assertFalse(emptyProperties.intProperty().isPresent());
    }

    @Test
    public void testLongProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.longProperty(), 9999999999L);
    }

    @Test
    public void testOptionalLongProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(9999999999L, (long) goodProperties.longProperty().get());
        assertFalse(emptyProperties.longProperty().isPresent());
    }

    @Test
    public void testFloatProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.floatProperty(), 0.22, 0.00001);
    }

    @Test
    public void testOptionalFloatProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.floatProperty().get(), 0.22, 0.00001);
        assertFalse(emptyProperties.floatProperty().isPresent());
    }

    @Test
    public void testDoubleProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertEquals(goodProperties.doubleProperty(), 0.45, 0.000);
    }

    @Test
    public void testOptionalDoubleProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.doubleProperty().get(), 0.45, 0.00001);
        assertFalse(emptyProperties.doubleProperty().isPresent());
    }

    @Test
    public void testBooleanProperty() {
        TestRequiredProperties goodProperties = newGoodProperties();
        assertTrue(goodProperties.booleanProperty());
    }

    @Test
    public void testOptionalBooleanProperty() {

        TestOptionalProperties goodProperties = newGoodOptionalProerties();
        TestOptionalProperties emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.booleanProperty().get(), true);
        assertFalse(emptyProperties.booleanProperty().isPresent());
    }

    @Test
    public void testFailingCreation() {
        // The command above should fail because that file doesn't exist
        assertThrows(IllegalArgumentException.class, () -> new TestRequiredProperties("missingFile.txt"));
    }

    @Test
    public void testFailingCreation2() {
        // The command above should fail because that file doesn't exist
        assertThrows(IllegalArgumentException.class, () -> new TestRequiredProperties(new File("missingFile.txt")));
    }

    @Test
    public void testAlternateConstructor() {
        Optional<File> resource = getResourceAsFile(QuickPropertiesTest.class, "goodProperties.properties");

        // QuickProperties created from a String (path) are properly loaded.
        assertDoesNotThrow(() -> new TestRequiredProperties(resource.get().getAbsolutePath()));
    }

    @Test
    public void testEntrySet() {
        ImmutableSortedSet<Entry<String, String>> entries = newGoodProperties().entrySet();

        assertEquals(entries.size(), 8);

        // notice, this list is in a different order than the original properties file
        List<Entry<String, String>> list = newArrayList(entries.iterator());
        assertEquals(list.get(0).getKey(), "booleanProperty");
        assertEquals(list.get(0).getValue(), "true");

        assertEquals(list.get(1).getKey(), "byteProperty");
        assertEquals(list.get(1).getValue(), "-12");

        assertEquals(list.get(2).getKey(), "doubleProperty");
        assertEquals(list.get(2).getValue(), "0.45");

        assertEquals(list.get(3).getKey(), "floatProperty");
        assertEquals(list.get(3).getValue(), "0.22");

        assertEquals(list.get(4).getKey(), "intProperty");
        assertEquals(list.get(4).getValue(), "5");

        assertEquals(list.get(5).getKey(), "longProperty");
        assertEquals(list.get(5).getValue(), "9999999999");

        assertEquals(list.get(6).getKey(), "shortProperty");
        assertEquals(list.get(6).getValue(), "145");

        assertEquals(list.get(7).getKey(), "stringProperty");
        assertEquals(list.get(7).getValue(), "value");
    }

    /*
     * Define a QuickProperties class that requires exactly 1 property: maxValue
     */
    static class SimpleDemo extends QuickProperties {

        SimpleDemo(Properties props) {
            super(props, newArrayList("maxValue"));
        }

        public int maxValue() {
            return getInt("maxValue");
        }

        public Optional<Integer> minValue() {
            return getOptionalInt("minValue");
        }
    }

    @Test
    public void testCreationSucceedsWhenRequirePropsExist() {
        Properties props = new Properties();
        props.setProperty("maxValue", "12");
        SimpleDemo demo = new SimpleDemo(props);

        assertEquals(demo.maxValue(), 12);
        assertThat("minValue wasn't provided", demo.minValue().isPresent(), is(false));
    }

    @Test
    public void testCreationFailsWhenRequirePropsAreMissing() {
        // fails because a new Properties object does not contain maxValue
        assertThrows(MissingPropertyException.class, () -> new SimpleDemo(new Properties()));
    }

    @Test
    public void testPropertiesCopying() {
        Properties in = new Properties();
        in.setProperty("prop1", "22");
        in.setProperty("prop2", " leadingAndTrailingSpacesAreDropped ");

        QuickProperties qp = new QuickProperties(in, newArrayList());

        Properties out = qp.properties();

        assertEquals(in.size(), out.size());
        assertEquals(out.getProperty("prop1"), "22");
        assertEquals(out.getProperty("prop2"), "leadingAndTrailingSpacesAreDropped");
    }

    static class DemoPair1 extends QuickProperties {

        DemoPair1(Properties props) {
            super(props, newArrayList("propRequiredBy1"));
        }

        public int requiredBy1() {
            return getInt("propRequiredBy1");
        }
    }

    static class DemoPair2 extends QuickProperties {

        DemoPair2(Properties props) {
            super(props, newArrayList("propRequiredBy2"));
        }

        public int requiredBy2() {
            return getInt("propRequiredBy2");
        }
    }

    @Test
    public void testCreatingMultiplePropsFromOneSource() {
        Properties combinedSource = new Properties();
        combinedSource.setProperty("propRequiredBy1", "1");
        combinedSource.setProperty("propRequiredBy2", "2");

        // direct creation works
        DemoPair1 pair1 = new DemoPair1(combinedSource);
        assertEquals(pair1.requiredBy1(), 1);

        // direct creation works
        DemoPair2 pair2 = new DemoPair2(combinedSource);
        assertEquals(pair2.requiredBy2(), 2);

        // indirect creation works too
        DemoPair1 indirect1 = new DemoPair1(pair2.properties());
        assertEquals(indirect1.requiredBy1(), 1);

        // indirect creation works too
        DemoPair2 indirect2 = new DemoPair2(pair1.properties());
        assertEquals(indirect2.requiredBy2(), 2);
    }

    @Test
    public void testToString() {
        /*
         * toString() should (A) list all value (both required and optional) and (b) order the
         * properties alphabetically
         */
        String fileContents = new StringBuilder()
                .append("prop3 : 22\n")
                .append("prop2 : hello\n")
                .append("prop1 : value")
                .toString();

        QuickProperties props = new QuickProperties(PropertyUtils.parseProperties(fileContents), newArrayList("prop1"));

        String expected = new StringBuilder()
                .append("prop1 : value\n")
                .append("prop2 : hello\n")
                .append("prop3 : 22\n")
                .toString();

        assertEquals(expected, props.toString());
    }

    @Test
    public void quickPropertiesRejectsFilesWithDuplicateKeys() {
        /*
         * This file should fail because it contains the same property listed twice. Listing a
         * single property twice is normally an error. Consequently, this behavior is specifically
         * prohibited.
         */
        File rejectFile = getResourceFile("org/mitre/caasd/commons/fileutil/rejectThisPropertiesFile.props");

        assertTrue(rejectFile.exists());

        assertThrows(IllegalArgumentException.class, () -> new QuickProperties(rejectFile, newArrayList()));
    }

    @Test
    public void quickPropertiesWorksWhenDuplicateKeysAreInComments() {
        /*
         * This file should NOT fail because it contains the same property listed twice, but those
         * extra times it appears in a comment. Listing a single property twice is normally an
         * error. However, people frequently comment out old/alternate property values so this
         * behavior is allowed.
         */

        File acceptFile = getResourceFile("org/mitre/caasd/commons/fileutil/acceptThisPropertiesFile.props");

        assertTrue(acceptFile.exists());

        QuickProperties props = new QuickProperties(acceptFile, newArrayList());

        assertThat(props.getString("key1"), is("goodValue"));
    }
}
