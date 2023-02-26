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

package org.mitre.caasd.commons.maps;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.LatLong;

class LocallyCachingTileServerTest {

    @Disabled
    @Test
    public void cacheWorks() throws Exception {

        LocallyCachingTileServer tileServer = new LocallyCachingTileServer(
            new DebugTileServer()
        );

        tileServer.downloadMap(LatLong.of(32.8968, -97.0380), 10);
    }

    @Test
    public void cacheWithSmallLifetimeWorks() throws Exception {

        LocallyCachingTileServer tileServer = new LocallyCachingTileServer(
            new DebugTileServer(),
            Duration.ofSeconds(1L)
        );

        tileServer.downloadMap(LatLong.of(32.8968, -97.0380), 10);
    }

}