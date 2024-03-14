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

package org.mitre.caasd.commons.out;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * Interface for converting a record to a json object that can easily be written out
 */
public interface JsonWritable {

    // THIS METHOD MIGHT BE REMOVED -- WE NEED TO SEE IF GETTING ACCESS TO THE JsonElement IS BETTER
    default JsonElement getJsonElement() {
        Gson gson = new GsonBuilder().create();
        return gson.toJsonTree(this, this.getClass());
    }

    default String asJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);

        return checkNotNull(json, "Gson produced null when attempting to convert an object to JSON");
    }
}
