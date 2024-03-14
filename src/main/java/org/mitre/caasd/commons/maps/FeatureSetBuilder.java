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

import static com.google.common.collect.Lists.newArrayList;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.maps.MapFeatures.*;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.mitre.caasd.commons.LatLong;

/**
 * This Builder provides a fluent API to assemble a list of: {Circles, Lines, Polygons, and Text}
 * objects that should be added to a map.  One goal of the API is to remove all "map pixel location
 * math".
 */
public class FeatureSetBuilder {

    List<MapFeature> features = newArrayList();

    // once set, these fields can make it easier to add multiple features that have the same "brush traits"
    Color currentColor = null;
    Integer currentCircleDiameter = null;
    Float currentStrokeWidth = null;
    Font currentFont = new Font("Avenir", PLAIN, 32);

    public static FeatureSetBuilder newFeatureSetBuilder() {
        return new FeatureSetBuilder();
    }

    public FeatureSetBuilder setColor(Color c) {
        this.currentColor = c;
        return this;
    }

    public FeatureSetBuilder setCircleDiameter(int diameterInPixels) {
        this.currentCircleDiameter = diameterInPixels;
        return this;
    }

    public FeatureSetBuilder setStrokeWidth(float stroke) {
        this.currentStrokeWidth = stroke;
        return this;
    }

    public FeatureSetBuilder setFont(Font font) {
        requireNonNull(font);
        this.currentFont = font;
        return this;
    }

    public FeatureSetBuilder addFeature(MapFeature feature) {
        requireNonNull(feature);
        features.add(feature);
        return this;
    }

    public FeatureSetBuilder addCircle(LatLong location, Color color, int diameterInPixels, float strokeWidth) {
        requireNonNull(location);
        requireNonNull(color);
        features.add(circle(location, color, diameterInPixels, strokeWidth));
        return this;
    }

    public FeatureSetBuilder addCircle(LatLong location, Color color, int diameterInPixels) {
        requireNonNull(location);
        requireNonNull(color);
        requireNonNull(currentStrokeWidth);
        features.add(circle(location, color, diameterInPixels, currentStrokeWidth));
        return this;
    }

    public FeatureSetBuilder addCircle(LatLong location, Color color) {
        requireNonNull(currentCircleDiameter);
        requireNonNull(currentStrokeWidth);
        return addCircle(location, color, currentCircleDiameter, currentStrokeWidth);
    }

    public FeatureSetBuilder addCircle(LatLong location) {
        requireNonNull(currentColor);
        requireNonNull(currentCircleDiameter);
        requireNonNull(currentStrokeWidth);
        return addCircle(location, currentColor, currentCircleDiameter, currentStrokeWidth);
    }

    public FeatureSetBuilder addFilledCircle(LatLong location, Color color, int diameterInPixels) {
        requireNonNull(location);
        requireNonNull(color);
        features.add(filledCircle(location, color, diameterInPixels));
        return this;
    }

    public FeatureSetBuilder addFilledCircle(LatLong location, Color color) {
        requireNonNull(currentCircleDiameter);
        return addFilledCircle(location, color, currentCircleDiameter);
    }

    public FeatureSetBuilder addFilledCircle(LatLong location) {
        requireNonNull(currentColor);
        requireNonNull(currentCircleDiameter);
        return addFilledCircle(location, currentColor, currentCircleDiameter);
    }

    public FeatureSetBuilder addLine(LatLong from, LatLong to, Color color, float stroke) {
        requireNonNull(from);
        requireNonNull(to);
        requireNonNull(color);
        features.add(line(from, to, color, stroke));
        return this;
    }

    public FeatureSetBuilder addLine(LatLong from, LatLong to, Color color) {
        requireNonNull(currentStrokeWidth);
        return addLine(from, to, color, currentStrokeWidth);
    }

    public FeatureSetBuilder addLine(LatLong from, LatLong to) {
        requireNonNull(currentColor);
        requireNonNull(currentStrokeWidth);
        return addLine(from, to, currentColor, currentStrokeWidth);
    }

    public FeatureSetBuilder addShape(List<LatLong> pts, Color color) {
        requireNonNull(pts);
        requireNonNull(color);
        features.add(shape(pts, color));
        return this;
    }

    public FeatureSetBuilder addFilledShape(List<LatLong> pts, Color color) {
        requireNonNull(pts);
        requireNonNull(color);
        features.add(filledShape(pts, color));
        return this;
    }

    public FeatureSetBuilder addPath(List<LatLong> pts, Color color, float strokeWidth) {
        requireNonNull(pts);
        requireNonNull(color);
        features.add(path(pts, color, strokeWidth));
        return this;
    }

    public FeatureSetBuilder addText(String message, int xOffset, int yOffset, Color c, Font font) {
        requireNonNull(message);
        requireNonNull(c);
        requireNonNull(font);
        features.add(text(message, xOffset, yOffset, c, font));
        return this;
    }

    public FeatureSetBuilder addText(String message, int xOffset, int yOffset, Color c) {
        requireNonNull(currentFont);
        return addText(message, xOffset, yOffset, c, currentFont);
    }

    public FeatureSetBuilder addText(String message, int xOffset, int yOffset) {
        requireNonNull(currentFont);
        requireNonNull(currentFont);
        return addText(message, xOffset, yOffset, currentColor, currentFont);
    }

    public FeatureSetBuilder addText(String message, LatLong anchor, Color c, Font font) {
        requireNonNull(message);
        requireNonNull(anchor);
        requireNonNull(c);
        requireNonNull(font);
        features.add(text(message, anchor, c, font));
        return this;
    }

    public FeatureSetBuilder addText(String message, LatLong anchor, Color c) {
        requireNonNull(currentFont);
        return addText(message, anchor, c, currentFont);
    }

    public FeatureSetBuilder addText(String message, LatLong anchor) {
        requireNonNull(currentColor);
        requireNonNull(currentFont);
        return addText(message, anchor, currentColor, currentFont);
    }

    public FeatureSet build() {
        return new FeatureSet(this.features);
    }
}
