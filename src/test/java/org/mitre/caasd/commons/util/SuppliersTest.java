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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.util.Suppliers.environmentVarSupplier;
import static org.mitre.caasd.commons.util.Suppliers.systemPropertySupplier;

import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;

import org.mitre.caasd.commons.fileutil.FileUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuppliersTest {

    @TempDir
    public File tempDir;

    @Test
    public void fileBasedSupplier_canReachFile() throws Exception {

        File tempFile = new File(tempDir, "putMyPropsInHere.cfg");
        String propertyValue = "iAmAValue";
        FileUtils.appendToFile(tempFile, "thisIsAPropertyKey=   " + propertyValue + "  ");

        Supplier<String> fbs = Suppliers.fileBasedSupplier(tempFile, "thisIsAPropertyKey");

        assertThat(fbs.get(), is(propertyValue));
    }

    @Test
    public void fileBasedSupplier_butNoFile() {

        String randomFileName = UUID.randomUUID() + ".txt";

        Supplier<String> fbs = Suppliers.fileBasedSupplier(new File(randomFileName), "thisIsAPropertyKey");

        // this IllegalArgumentException is thrown because the "File parser" notices the File does not exist
        assertThrows(IllegalArgumentException.class, () -> fbs.get());
    }

    @Test
    public void fileBasedSupplier_canBeBuildWithNullFile() {
        // we don't want this to fail AT CONSTRUCTION...it can fail during use though
        assertDoesNotThrow(() -> Suppliers.fileBasedSupplier(null, "propertyKey"));
    }

    @Test
    public void fileBasedSupplier_builtWithNullFileFails() {

        Supplier<String> fbs = Suppliers.fileBasedSupplier(null, "propertyKey");

        assertThrows(NullPointerException.class, () -> fbs.get());
    }

    @Test
    public void canPullFromSystemProps_happyPath() {

        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        System.setProperty(key, value);

        Supplier<String> sysPropSup = systemPropertySupplier(key);

        assertThat(sysPropSup.get(), is(value));
    }

    @Test
    public void canPullFromSystemProps_noValueComesBackNull() {

        // make a random key -- it obviously won't be in the system properties...so it should come back null
        String key = UUID.randomUUID().toString();
        Supplier<String> sysPropSup = systemPropertySupplier(key);
        assertThat(sysPropSup.get(), nullValue());
    }

    @Test
    public void canPullFromEnvironmentVars_noValueComesBackNull() {

        // make a random key -- it obviously won't be in the system properties...so it should come back null
        String key = UUID.randomUUID().toString();
        Supplier<String> environmentVarSup = environmentVarSupplier(key);
        assertThat(environmentVarSup.get(), nullValue());
    }
}
