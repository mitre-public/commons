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
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.List;

import org.mitre.caasd.commons.LatLong;

/**
 * MapFeature classes are "drawn on" a Graphics2D object.  The API allows you to define MapFeature
 * with regular LatLong and color information.
 */
public class MapFeatures {

    //@todo -- Add Multiline text option e.g. (String[] {"line 1", "line 2", "line 3"})
    //@todo -- Add Gradient option
    //@todo -- Consider putting all "stroke/color/font" features behind a "brush" interface (which will use the Composite pattern and frequently contain UnsupportedOperations)


    /** This class helps us draw a circle on a map. */
    private static class Circle implements MapFeature {
        final LatLong loc;
        final Color color;
        final int pixelWidth;
        final boolean isFilled;
        final float strokeWidth;

        public Circle(LatLong loc, Color color, int widthInPixels, boolean isFilled, float strokeWidth) {
            this.loc = loc;
            this.color = color;
            this.pixelWidth = widthInPixels;
            this.isFilled = isFilled;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            PixelLatLong center = new PixelLatLong(loc, zeroPixel.zoom(), zeroPixel.tileSize());

            g.setColor(color);
            g.setStroke(new BasicStroke(strokeWidth, CAP_ROUND, JOIN_ROUND));

            //The draw operations start on the top-left, if we want the circle centered around the location we must shift the pixels
            int x = center.x(zeroPixel) - pixelWidth / 2;
            int y = center.y(zeroPixel) - pixelWidth / 2;

            if (isFilled) {
                g.fillOval(x, y, pixelWidth, pixelWidth);
            } else {
                g.drawOval(x, y, pixelWidth, pixelWidth);
            }
        }
    }

    /** This class helps us draw a line on a map. */
    private static class Line implements MapFeature {
        final LatLong from;
        final LatLong to;
        final Color color;
        final float strokeWidth;

        Line(LatLong from, LatLong to, Color color, float strokeWidth) {
            this.from = from;
            this.to = to;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            PixelLatLong start = new PixelLatLong(from, zeroPixel.zoom(), zeroPixel.tileSize());
            PixelLatLong end = new PixelLatLong(to, zeroPixel.zoom(), zeroPixel.tileSize());

            g.setColor(color);
            g.setStroke(new BasicStroke(strokeWidth, CAP_ROUND, JOIN_ROUND));

            g.drawLine(start.x(zeroPixel), start.y(zeroPixel), end.x(zeroPixel), end.y(zeroPixel));
        }
    }

    /** This class helps us draw a line on a map. */
    private static class Rect implements MapFeature {
        final LatLong topLeft;
        final LatLong bottomRight;
        final Color color;
        final float strokeWidth;

        Rect(LatLong topLeft, LatLong bottomRight, Color color, float strokeWidth) {
            this.topLeft = topLeft;
            this.bottomRight = bottomRight;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            PixelLatLong topLeftPixel =  new PixelLatLong(topLeft, zeroPixel.zoom(), zeroPixel.tileSize());
            PixelLatLong bottomRightPixel =  new PixelLatLong(bottomRight, zeroPixel.zoom(), zeroPixel.tileSize());

            int width = (int) (bottomRightPixel.x() - topLeftPixel.x());
            int height = (int) (bottomRightPixel.y() - topLeftPixel.y());

            g.setColor(color);
            g.setStroke(new BasicStroke(strokeWidth, CAP_ROUND, JOIN_ROUND));

            g.drawRect(topLeftPixel.x(zeroPixel), topLeftPixel.y(zeroPixel), width, height);
        }
    }


    private static class Polygon implements MapFeature {

        List<LatLong> vertices;
        Color color;
        boolean isFilled;

        Polygon(List<LatLong> vertices, Color color, boolean isFilled) {
            requireNonNull(vertices);
            requireNonNull(color);
            this.vertices = vertices;
            this.color = color;
            this.isFilled = isFilled;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            g.setColor(color);

            int n = vertices.size();
            int[] xArray = new int[n];
            int[] yArray = new int[n];
            for (int i = 0; i < n; i++) {
                PixelLatLong pixel = toPixel(vertices.get(i), zeroPixel);
                xArray[i] = (int) pixel.x(zeroPixel);
                yArray[i] = (int) pixel.y(zeroPixel);
            }

            java.awt.Polygon shape = new java.awt.Polygon(xArray, yArray, xArray.length);

            if(isFilled) {
                g.fill(shape);
            } else {
                g.draw(shape);
            }
        }
    }


    private static class Path implements MapFeature {

        final List<LatLong> vertices;
        final Color color;
        final Float strokeWidth;

        Path(List<LatLong> vertices, Color color, float strokeWidth) {
            requireNonNull(vertices);
            requireNonNull(color);
            this.vertices = vertices;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            g.setColor(color);
            g.setStroke(new BasicStroke(strokeWidth, CAP_ROUND, JOIN_ROUND));

            int n = vertices.size();
            int[] xArray = new int[n];
            int[] yArray = new int[n];
            for (int i = 0; i < n; i++) {
                PixelLatLong pixel = toPixel(vertices.get(i), zeroPixel);
                xArray[i] = (int) pixel.x(zeroPixel);
                yArray[i] = (int) pixel.y(zeroPixel);
            }

            //See also: https://docs.oracle.com/javase/tutorial/2d/geometry/examples/ShapesDemo2D.java

            GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, n);
            polyline.moveTo(xArray[0], yArray[0]);
            for (int i = 1; i < n; i++) {
                polyline.lineTo(xArray[i], yArray[i]);
            }

            g.draw(polyline);
        }
    }


    /** This class helps us draw a text on a map. */
    private static class TextLabel implements MapFeature {
        final String text;
        final LatLong anchor;
        final Color color;
        final Font font;

        TextLabel(String text, LatLong anchor, Color color, Font font) {
            this.text = text;
            this.anchor = anchor;
            this.color = color;
            this.font = font;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            PixelLatLong pixelAnchor = new PixelLatLong(anchor, zeroPixel.zoom(), zeroPixel.tileSize());

            g.setColor(color);
            g.setFont(font);
            g.drawString(text, pixelAnchor.x(zeroPixel), pixelAnchor.y(zeroPixel));
        }
    }

    private static class InfoText implements MapFeature {

        final String text;
        final int xOffset;
        final int yOffset;
        final Color color;
        final Font font;

        InfoText(String text, int xOffset, int yOffset, Color color, Font font) {
            this.text = text;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.color = color;
            this.font = font;
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {

            g.setColor(color);
            g.setFont(font);
            g.drawString(text, xOffset, yOffset);
        }
    }


    private static class CompositeFeature implements MapFeature {

        List<MapFeature> features;

        CompositeFeature(Collection<MapFeature> componentFeatures) {
            requireNonNull(componentFeatures);
            this.features = newArrayList(componentFeatures);
        }

        @Override
        public void drawOn(Graphics2D g, PixelLatLong zeroPixel) {
            features.forEach(feature -> feature.drawOn(g, zeroPixel));
        }
    }

    public static MapFeature filledCircle(LatLong loc, Color c, int diameterInPixels) {
        return new Circle(loc, c, diameterInPixels, true, 1.0f); //stroke width does not matter for filled circles
    }

    public static MapFeature circle(LatLong loc, Color c, int diameterInPixels, float strokeLineWidth) {
        return new Circle(loc, c, diameterInPixels, false, strokeLineWidth);
    }

    public static MapFeature line(LatLong from, LatLong to, Color c, float lineWidth) {
        return new Line(from, to, c, lineWidth);
    }

    public static MapFeature rect(LatLong topLeft, LatLong bottomRight, Color c, float lineWidth) {
        return new Rect(topLeft, bottomRight, c, lineWidth);
    }

    public static MapFeature text(String text, LatLong anchor, Color c) {
        return new TextLabel(text, anchor, c, new Font("Avenir", PLAIN, 32));
    }

    public static MapFeature text(String text, LatLong anchor, Color c, Font font) {
        return new TextLabel(text, anchor, c, font);
    }

    public static MapFeature text(String text, int xOffset, int yOffset, Color c) {
        return new InfoText(text, xOffset, yOffset, c, new Font("Avenir", PLAIN, 32));
    }

    public static MapFeature text(String text, int xOffset, int yOffset, Color c, Font font) {
        return new InfoText(text, xOffset, yOffset, c, font);
    }

    public static MapFeature shape(List<LatLong> pts, Color c) {
        return new Polygon(pts, c, false);
    }

    public static MapFeature filledShape(List<LatLong> pts, Color c) {
        return new Polygon(pts, c, true);
    }

    /** Create a filled shape and a shape (i.e. outline) where each may have a different color. */
    public static List<MapFeature> shapeWithOutline(List<LatLong> points, Color fillColor, Color borderColor) {
        return newArrayList(
            filledShape(points, fillColor),
            shape(points, borderColor)
        );
    }

    public static MapFeature path(List<LatLong> pts, Color c, float strokeWidth) {
        return new Path(pts, c, strokeWidth);
    }

    /** Combines multiple MapFeatures into a single MapFeature. */
    public static MapFeature compose(Collection<MapFeature> features) {
        return new CompositeFeature(features);
    }

    private static PixelLatLong toPixel(LatLong loc, PixelLatLong zero) {
        return new PixelLatLong(loc, zero.zoom(), zero.tileSize());
    }
}
