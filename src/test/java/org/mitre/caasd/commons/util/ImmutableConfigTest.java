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

public class ImmutableConfigTest {

    /* All of these asProperties must exist for a TestConfigUsage to get created. */
    static List<String> REQUIRED_PROPS = newArrayList(
            "stringProperty",
            "byteProperty",
            "shortProperty",
            "intProperty",
            "longProperty",
            "floatProperty",
            "doubleProperty",
            "booleanProperty");

    /**
     * This test class shows how to use the ImmutableConfig class.
     * <p>
     * Notice how the asProperties retrieved are readable from the code itself. Notice, also that
     * there is no room to misspell or incorrectly capitalize the property names (in the user facing
     * api)
     * <p>
     * This class uses asProperties that are strictly required. Consequently, this class cannot be
     * created unless all required asProperties exist.
     */
    static class TestConfigUsage {

        ImmutableConfig confg;

        TestConfigUsage(String filePath) {
            this.confg = new ImmutableConfig(PropertyUtils.loadProperties(filePath), REQUIRED_PROPS);
        }

        TestConfigUsage(File textFile) {
            this.confg = new ImmutableConfig(PropertyUtils.loadProperties(textFile), REQUIRED_PROPS);
        }

        public String stringProperty() {
            return confg.getString("stringProperty");
        }

        public byte byteProperty() {
            return confg.getByte("byteProperty");
        }

        public short shortProperty() {
            return confg.getShort("shortProperty");
        }

        public int intProperty() {
            return confg.getInt("intProperty");
        }

        public long longProperty() {
            return confg.getLong("longProperty");
        }

        public float floatProperty() {
            return confg.getFloat("floatProperty");
        }

        public double doubleProperty() {
            return confg.getDouble("doubleProperty");
        }

        public boolean booleanProperty() {
            return confg.getBoolean("booleanProperty");
        }

        ImmutableConfig config() {
            return confg;
        }
    }

    /**
     * This test class shows how to use the ImmutableConfig class.
     * <p>
     * Notice how the asProperties retrieved are readable from the code itself. Notice, also that
     * there is no room to misspell or incorrectly capitalize the property names (in the user facing
     * api)
     * <p>
     * This class uses optional asProperties. Consequently, all "retrieval methods" return
     * Optionals.
     */
    static class TestConfigUsageWithOptional {

        ImmutableConfig confg;

        TestConfigUsageWithOptional(String filePath) {
            this.confg = new ImmutableConfig(PropertyUtils.loadProperties(filePath));
        }

        TestConfigUsageWithOptional(File textFile) {
            this.confg = new ImmutableConfig(PropertyUtils.loadProperties(textFile));
        }

        public Optional<String> stringProperty() {
            return confg.getOptionalString("stringProperty");
        }

        public Optional<Byte> byteProperty() {
            return confg.getOptionalByte("byteProperty");
        }

        public Optional<Short> shortProperty() {
            return confg.getOptionalShort("shortProperty");
        }

        public Optional<Integer> intProperty() {
            return confg.getOptionalInt("intProperty");
        }

        public Optional<Long> longProperty() {
            return confg.getOptionalLong("longProperty");
        }

        public Optional<Float> floatProperty() {
            return confg.getOptionalFloat("floatProperty");
        }

        public Optional<Double> doubleProperty() {
            return confg.getOptionalDouble("doubleProperty");
        }

        public Optional<Boolean> booleanProperty() {
            return confg.getOptionalBoolean("booleanProperty");
        }
    }

    private TestConfigUsage newGoodProperties() {
        Optional<File> resource = getResourceAsFile(ImmutableConfigTest.class, "goodProperties.properties");
        return new TestConfigUsage(resource.get());
    }

    private TestConfigUsage newEmptyProperties() {
        Optional<File> resource = getResourceAsFile(ImmutableConfigTest.class, "emptyProperties.properties");
        return new TestConfigUsage(resource.get());
    }

    private TestConfigUsageWithOptional newGoodOptionalProerties() {
        Optional<File> resource = getResourceAsFile(ImmutableConfigTest.class, "goodProperties.properties");
        return new TestConfigUsageWithOptional(resource.get());
    }

    private TestConfigUsageWithOptional newEmptyOptionalProerties() {
        Optional<File> resource = getResourceAsFile(ImmutableConfigTest.class, "emptyProperties.properties");
        return new TestConfigUsageWithOptional(resource.get());
    }

    @Test
    public void testStringProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.stringProperty(), "value");
    }

    @Test
    public void testOptionalStringProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.stringProperty().get(), "value");
        assertFalse(emptyProperties.stringProperty().isPresent());
    }

    @Test
    public void testByteProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.byteProperty(), -12);
    }

    @Test
    public void testOptionalByteProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertTrue(goodProperties.byteProperty().get() == -12);
        assertFalse(emptyProperties.byteProperty().isPresent());
    }

    @Test
    public void testShortProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.shortProperty(), 145);
    }

    @Test
    public void testOptionalShortProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertTrue(goodProperties.shortProperty().get() == 145);
        assertFalse(emptyProperties.shortProperty().isPresent());
    }

    @Test
    public void testIntProperty() {

        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.intProperty(), 5);
    }

    @Test
    public void testOptionalIntProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertTrue(goodProperties.intProperty().get() == 5);
        assertFalse(emptyProperties.intProperty().isPresent());
    }

    @Test
    public void testLongProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.longProperty(), 9999999999L);
    }

    @Test
    public void testOptionalLongProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertTrue(goodProperties.longProperty().get() == 9999999999L);
        assertFalse(emptyProperties.longProperty().isPresent());
    }

    @Test
    public void testFloatProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.floatProperty(), 0.22, 0.00001);
    }

    @Test
    public void testOptionalFloatProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.floatProperty().get(), 0.22, 0.00001);
        assertFalse(emptyProperties.floatProperty().isPresent());
    }

    @Test
    public void testDoubleProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertEquals(goodProperties.doubleProperty(), 0.45, 0.000);
    }

    @Test
    public void testOptionalDoubleProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.doubleProperty().get(), 0.45, 0.00001);
        assertFalse(emptyProperties.doubleProperty().isPresent());
    }

    @Test
    public void testBooleanProperty() {
        TestConfigUsage goodProperties = newGoodProperties();
        assertTrue(goodProperties.booleanProperty());
    }

    @Test
    public void testOptionalBooleanProperty() {

        TestConfigUsageWithOptional goodProperties = newGoodOptionalProerties();
        TestConfigUsageWithOptional emptyProperties = newEmptyOptionalProerties();

        assertEquals(goodProperties.booleanProperty().get(), true);
        assertFalse(emptyProperties.booleanProperty().isPresent());
    }

    @Test
    public void testFailingCreation() {
        // The command above should fail because that file doesn't exist
        assertThrows(IllegalArgumentException.class, () -> new TestConfigUsage("missingFile.txt"));
    }

    @Test
    public void testFailingCreation2() {
        // The command above should fail because that file doesn't exist
        assertThrows(IllegalArgumentException.class, () -> new TestConfigUsage(new File("missingFile.txt")));
    }

    @Test
    public void testAlternateConstructor() {
        Optional<File> resource = getResourceAsFile(ImmutableConfigTest.class, "goodProperties.properties");

        // ImmutableConfig created from a String (path) are properly loaded
        assertDoesNotThrow(() -> new TestConfigUsage(resource.get().getAbsolutePath()));
    }

    @Test
    public void testEntrySet() {
        ImmutableSortedSet<Entry<String, String>> entries =
                newGoodProperties().config().entrySet();

        assertEquals(entries.size(), 8);

        // notice, this list is in a different order than the original asProperties file
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

    /* Define a ImmutableConfig class that requires exactly 1 property: maxValue  */
    static class SimpleDemo {

        ImmutableConfig config;

        SimpleDemo(Properties props) {
            config = new ImmutableConfig(props, newArrayList("maxValue"));
        }

        public int maxValue() {
            return config.getInt("maxValue");
        }

        public Optional<Integer> minValue() {
            return config.getOptionalInt("minValue");
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

    static class DemoPair1 {

        ImmutableConfig cfg;

        DemoPair1(Properties props) {
            this.cfg = new ImmutableConfig(props, newArrayList("propRequiredBy1"));
        }

        public int requiredBy1() {
            return cfg.getInt("propRequiredBy1");
        }
    }

    static class DemoPair2 {

        ImmutableConfig cfg;

        DemoPair2(Properties props) {
            this.cfg = new ImmutableConfig(props, newArrayList("propRequiredBy2"));
        }

        public int requiredBy2() {
            return cfg.getInt("propRequiredBy2");
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
        DemoPair1 indirect1 = new DemoPair1(pair2.cfg.asProperties());
        assertEquals(indirect1.requiredBy1(), 1);

        // indirect creation works too
        DemoPair2 indirect2 = new DemoPair2(pair1.cfg.asProperties());
        assertEquals(indirect2.requiredBy2(), 2);
    }

    @Test
    public void testToString() {
        /*
         * toString() should (A) list all value (both required and optional) and (b) order the
         * asProperties alphabetically
         */
        String fileContents = new StringBuilder()
                .append("prop3 : 22\n")
                .append("prop2 : hello\n")
                .append("prop1 : value")
                .toString();

        ImmutableConfig props = new ImmutableConfig(PropertyUtils.parseProperties(fileContents), newArrayList("prop1"));

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

        assertThrows(IllegalArgumentException.class, () -> new ImmutableConfig(rejectFile, newArrayList()));
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

        ImmutableConfig props = new ImmutableConfig(acceptFile, newArrayList());

        assertThat(props.getString("key1"), is("goodValue"));
    }
}
