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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.Serializable;

import org.mitre.caasd.commons.fileutil.FileUtils;

public class TestUtils {

    /**
     * Verify that a Serializable object is indeed Serializable. Do this by (1) trying to serialize
     * the object, (2) deserializing the object, and (3) confirming the original object is equal to
     * the deserialized object
     *
     * @param serializableObj
     */
    public static synchronized void verifySerializability(Serializable serializableObj) {
        checkNotNull(serializableObj);

        String fileName = "serializationTest.ser";
        File targetFile = new File(fileName);

        assertThat("The targetFile should not exists yet", targetFile.exists(), is(false));

        FileUtils.serialize(serializableObj, fileName);

        assertThat("The targetFile should now exist", targetFile.exists(), is(true));

        Object obj = FileUtils.deserialize(targetFile);

        new File(fileName).delete();

        assertEquals(serializableObj, obj);
    }
}
