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

import static org.mitre.caasd.commons.util.Suppliers.stringSupplierChain;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

public class MapBoxApi implements TileServer {

    public enum Style {
        STREETS("streets-v11"),
        OUTDOORS("outdoors-v11"),
        LIGHT("light-v10"),
        DARK("dark-v10"),
        SATELLITE("satellite-v9"),
        SATELLITE_STREETS("satellite-streets-v11");

        private final String url;

        Style(String url) {
            this.url = url;
        }

        public String urlComponent() {
            return url;
        }
    }

    public static final String MAPBOX_ACCESS_TOKEN_KEY = "MAPBOX_ACCESS_TOKEN";

    /**
     * Sequentially searches: Environment Variables, System Properties, and then a flat text file
     * (java Property formatting) until is finds the requested propertyKey
     */
    private static final Supplier<String> TOKEN_SUPPLIER = stringSupplierChain(
        MAPBOX_ACCESS_TOKEN_KEY, new File(System.getProperty("user.dir"), "mapbox.token")
    );

    private final Style mappingStyle;

    /**
     * Using the MapBoxApi requires passing a valid "MAPBOX_ACCESS_TOKEN" to the JVM one of these 3
     * ways: Setting an Environment Variable, Setting a System Property, or putting a
     * "java.util.Properties" formatted file named "mapbox.token" in the working directory.
     *
     * <p>No matter which method you use you will need to label a valid MapBox API access token with
     * the key: MAPBOX_ACCESS_TOKEN
     *
     * <p>For example: "MAPBOX_ACCESS_TOKEN=this.IsNotAValidKey.EvenThoughILookLikeOne"
     *
     * @param mappingStyle The type of MapBox image to request
     */
    public MapBoxApi(Style mappingStyle) {
        this.mappingStyle = mappingStyle;
    }

    @Override
    public int maxZoomLevel() {
        return 18; //this is what's in serpent level
    }

    @Override
    public int maxTileSize() {
        return 512;
    }

    @Override
    public URL getUrlFor(TileAddress ta) {
        String url = "https://api.mapbox.com/styles/v1/mapbox/" + mappingStyle.urlComponent()
            + "/tiles/512/"
            + ta.tileUrlComponent()
            + "/" + "?access_token=" + TOKEN_SUPPLIER.get();

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}