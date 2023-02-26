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

package org.mitre.caasd.commons.math;

import java.util.ArrayList;
import java.util.Collection;

public class XyPoint {

    final double x;
    final double y;

    public XyPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static XyPoint of(double x, double y) {
        return new XyPoint(x, y);
    }

    public static XyDataset asDataset(Collection<XyPoint> xyData) {
        //re-package results as a Dataset and return
        ArrayList<Double> xData = new ArrayList<>(xyData.size());
        ArrayList<Double> yData = new ArrayList<>(xyData.size());
        for (XyPoint xyPoint : xyData) {
            xData.add(xyPoint.x());
            yData.add(xyPoint.y());
        }
        return new XyDataset(xData, yData);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + " , " + y + ")";
    }

}
