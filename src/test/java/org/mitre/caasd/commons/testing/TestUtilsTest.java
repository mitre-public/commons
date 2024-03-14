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

package org.mitre.caasd.commons.testing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

public class TestUtilsTest {

    @Test
    public void testSerializeAndDeserializeOnGoodInput() {

        Integer original = 5;

        Integer result = (Integer) TestUtils.serializeAndDeserialize(original);

        assertThat("When we serialize 5 we can retrieve 5 from the serialized form", result == 5);
    }

    @Test
    public void testSerializeAndDeserializeOnBadInput() {
        /*
         * Here we confirm that bad input will not serialize properly.
         */
        class ClassThatCannotSerialize implements Serializable {

            final Thread unserializableField = new Thread();
        }

        try {
            ClassThatCannotSerialize original = new ClassThatCannotSerialize();
            Serializable copy = (Serializable) TestUtils.serializeAndDeserialize(original);
            fail("This should fail because a Thread cannot be serialized");
        } catch (Exception ex) {
            assertThat(ex.getMessage().contains("NotSerializableException"), is(true));
        }
    }

    @Test
    public void testConfirmSerializabilityOnGoodInput() {

        Serializable original = 5;
        File targetFile = new File("serializedNumber.ser");

        assertThat(targetFile.exists(), is(false));

        TestUtils.confirmSerializability(original, targetFile);

        assertThat("An output file exists if serialization is sucessful", targetFile.exists());
        targetFile.delete();
        assertThat("Clean up the test file", targetFile.exists(), is(false));
    }

    @Test
    public void testConfirmSerializabilityOnBadInput() {

        class ClassThatCannotSerialize implements Serializable {

            final Thread unserializableField = new Thread();
        }

        File targetFile = new File("shouldNotWork.ser");

        Serializable original = new ClassThatCannotSerialize();

        assertThrows(Exception.class, () -> TestUtils.confirmSerializability(original, targetFile));

        assertThat("The output file should be deleted in the event of a failure", targetFile.exists(), is(false));
    }
}
