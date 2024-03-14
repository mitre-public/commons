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
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.reflect.Modifier.isPublic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.util.PropertyUtils.*;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class PropertyUtilsTest {

    @Test
    public void testParseProperties() {

        String allLines = new StringBuilder()
            .append("#shouldIgnoreThisComment = 5").append("\n")
            .append("variable1 : 12").append("\n")
            .append("what = valueForWhat").append("\n")
            .append("var : 18").append("\n")
            .toString();

        Properties props = parseProperties(allLines);

        assertEquals(props.getProperty("variable1"), "12");
        assertEquals(props.getProperty("what"), "valueForWhat");
        assertEquals(props.getProperty("var"), "18");
        assertThat("The comment should have been ignored", props.size(), is(3));
    }

    @Test
    public void canBuildMissingPropertyExceptions() {
        MissingPropertyException mpe = new MissingPropertyException("PROP_NAME");
        assertThat(mpe.getMessage(), equalTo("The property PROP_NAME is missing"));
    }

    @Test
    public void missingPropertyExceptionConstructorIsPublic() {
        /* Confirm a constructor is public so external packages can use this Exception type. */
        Constructor[] constructors = MissingPropertyException.class.getConstructors();

        Constructor<MissingPropertyException> constructorWithOneStringParam
            = (Constructor<MissingPropertyException>) Stream.of(constructors)
            .filter(constructor -> constructor.getParameterCount() == 1)
            .filter(constructor -> constructor.getParameterTypes()[0] == String.class)
            .findFirst()
            .get();

        assertTrue(isPublic(constructorWithOneStringParam.getModifiers()));
    }

    @Test
    public void getRequiredProperty_trimsWhiteSpace() {
        Properties props = new Properties();
        props.setProperty("key", " value ");

        assertThat(getString("key", props), is("value"));
    }

    @Test
    public void getRequiredProperty_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getString("missingKey", new Properties())
        );
    }

    @Test
    public void getOptionalProperty_trimsWhiteSpace() {

        Properties props = new Properties();
        props.setProperty("key", " value ");

        assertThat(getOptionalString("key", props).get(), is("value"));
    }

    @Test
    public void getOptionalProperty_returnsEmptyOptional() {
        Optional<String> result = getOptionalString("missingKey", new Properties());

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getOptionalProperty_defaultValueIsReturnedWhenKeyIsMissing() {
        String result = getOptionalString("missingKey", new Properties(), "defaultValue");
        assertThat(result, is("defaultValue"));
    }

    @Test
    public void getOptionalProperty_withDefault_trimsWhiteSpace() {

        Properties props = new Properties();
        props.setProperty("key", "   value   ");

        assertThat(getOptionalString("key", props, "someDefaultValue"), is("value"));
    }

    @Test
    public void getRequiredByte_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getByte("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredByte_parsesByte() {
        Properties props = new Properties();
        props.setProperty("key", "   22   ");
        assertThat(getByte("key", props), is((byte) 22));
    }

    @Test
    public void getRequiredShort_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getShort("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredShort_parsesShort() {
        Properties props = new Properties();
        props.setProperty("key", "   22   ");
        assertThat(getShort("key", props), is((short) 22));
    }

    @Test
    public void getRequiredInt_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getInt("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredInt_parsesInt() {
        Properties props = new Properties();
        props.setProperty("key", "   22   ");
        assertThat(getInt("key", props), is(22));
    }

    @Test
    public void getRequiredLong_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getLong("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredLong_parsesLong() {
        Properties props = new Properties();
        props.setProperty("key", "   22   ");
        assertThat(getLong("key", props), is((long) 22));
    }

    @Test
    public void getRequiredFloat_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getFloat("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredFloat_parsesFloat() {
        Properties props = new Properties();
        props.setProperty("key", "   22.123   ");
        assertThat(getFloat("key", props), is(22.123f));
    }

    @Test
    public void getRequiredDouble_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getDouble("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredDouble_parsesDouble() {
        Properties props = new Properties();
        props.setProperty("key", "   22.123   ");
        assertThat(getDouble("key", props), is(22.123));
    }

    @Test
    public void getRequiredBoolean_throwsMissingPropertyException() {
        assertThrows(MissingPropertyException.class,
            () -> getBoolean("missingKey", new Properties())
        );
    }

    @Test
    public void getRequiredBoolean_parsesBoolean() {
        Properties props = new Properties();
        props.setProperty("key", "   false   ");
        assertThat(getBoolean("key", props), is(false));
    }

    @Test
    public void getOptionalByte_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22   ");

        byte retrievedValue = getOptionalByte("key", props, (byte) 33);
        assertThat(retrievedValue, is((byte) 22));

        byte defaultValue = getOptionalByte("missingKey", props, (byte) 52);
        assertThat(defaultValue, is((byte) 52));
    }

    @Test
    public void getOptionalShort_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22   ");

        short retrievedValue = getOptionalShort("key", props, (short) 33);
        assertThat(retrievedValue, is((short) 22));

        short defaultValue = getOptionalShort("missingKey", props, (short) 52);
        assertThat(defaultValue, is((short) 52));
    }

    @Test
    public void getOptionalInt_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22   ");

        int retrievedValue = getOptionalInt("key", props, 33);
        assertThat(retrievedValue, is(22));

        int defaultValue = getOptionalInt("missingKey", props, 52);
        assertThat(defaultValue, is(52));
    }

    @Test
    public void getOptionalLong_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22   ");

        long retrievedValue = getOptionalLong("key", props, 33);
        assertThat(retrievedValue, is((long) 22));

        long defaultValue = getOptionalLong("missingKey", props, 52);
        assertThat(defaultValue, is((long) 52));
    }

    @Test
    public void getOptionalFloat_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22.123   ");

        float retrievedValue = getOptionalFloat("key", props, 33);
        assertThat(retrievedValue, is(22.123f));

        float defaultValue = getOptionalFloat("missingKey", props, 52);
        assertThat(defaultValue, is(52f));
    }

    @Test
    public void getOptionalDouble_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   22.123   ");

        double retrievedValue = getOptionalDouble("key", props, 33);
        assertThat(retrievedValue, is(22.123));

        double defaultValue = getOptionalDouble("missingKey", props, 52);
        assertThat(defaultValue, is(52.0));
    }

    @Test
    public void getOptionalBoolean_withDefault() {

        Properties props = new Properties();
        props.setProperty("key", "   false   ");

        boolean retrievedValue = getOptionalBoolean("key", props, true);
        assertThat(retrievedValue, is(false));

        boolean defaultValue = getOptionalBoolean("missingKey", props, true);
        assertThat(defaultValue, is(true));
    }

    @Test
    public void tokenizeAndValidate_acceptsEmptyString() {
        List<String> tokens = tokenizeAndValidate("", newArrayList("OPTION_1", "OPTION_2"));

        assertThat(tokens, empty());
    }

    @Test
    public void tokenizeAndValidate_rejectsNullString() {
        assertThrows(NullPointerException.class,
            () -> tokenizeAndValidate(null, newArrayList("OPTION_1", "OPTION_2"))
        );
    }

    @Test
    public void tokenizeAndValidate_rejectsTokenNotInList() {
        //CORE FUNCTIONALITY -- REJECTS UNKNOWN TOKENS
        assertThrows(IllegalArgumentException.class,
            () -> tokenizeAndValidate("OPTION_3", newArrayList("OPTION_1", "OPTION_2"))
        );
    }

    @Test
    public void tokenizeAndValidate_acceptsTokensFoundInList() {
        Set<String> validTokens = newHashSet("OPT_1", "OPT_2");
        List<String> tokens = tokenizeAndValidate("OPT_2, OPT_2, OPT_1", validTokens);

        assertThat(tokens, hasSize(3));

        assertThat(tokens.get(0), is("OPT_2"));
        assertThat(tokens.get(1), is("OPT_2"));
        assertThat(tokens.get(2), is("OPT_1"));
    }
}
