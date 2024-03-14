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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 * This class provides a convenient way to fit a polynomial function to an x-y data set.
 *
 * <p>This class is powered by "org.apache.commons:commons-math3".  This dependency solves
 * Least-Squares fitting problems using "standard Java code" (i.e. no native code).  Consequently,
 * larger problems should NOT be solved using this code. Problems with "large N" generate large
 * matrix problems that should be solved with native or GPU-based code.
 *
 * <p>Note:  This class exposes its dependency on "org.apache.commons:commons-math3" via its public
 * API.
 */
public class CurveFitters {

    /**
     * @param polynomialDegree The degree of the fit polynomial (aka 2 = quadratic)
     * @param xData            The list of X values
     * @param yData            The list of Y values
     * @param predictHere      The x value for which you will predict a y value.
     *
     * @return A predicted y value
     */
    public static double fitAndPredict(
            int polynomialDegree, List<Double> xData, List<Double> yData, double predictHere) {
        checkArgument(xData.size() == yData.size());

        int n = xData.size();

        List<Double> equalWeights = IntStream.range(0, n).mapToObj(i -> 1.0).collect(toList());

        return fitAndPredict(polynomialDegree, equalWeights, xData, yData, predictHere);
    }

    /**
     * @param polynomialDegree The degree of the fit polynomial (aka 2 = quadratic)
     * @param weights          The weights for each data point
     * @param xData            The list of X values
     * @param yData            The list of Y values
     * @param predictHere      The x value for which you will predict a y value.
     *
     * @return A predicted y value
     */
    public static double fitAndPredict(
            int polynomialDegree, List<Double> weights, List<Double> xData, List<Double> yData, double predictHere) {

        PolynomialFunction func = weightedFit(polynomialDegree, weights, xData, yData);
        return func.value(predictHere);
    }

    /**
     * @param polynomialDegree The degree of the fit polynomial (aka 2 = quadratic)
     * @param xData            The list of X values
     * @param yData            The list of Y values
     *
     * @return A fitted PolynomialFunction (from org.apache.commons:commons-math3)
     */
    public static PolynomialFunction fit(int polynomialDegree, List<Double> xData, List<Double> yData) {
        checkArgument(xData.size() == yData.size());

        int n = xData.size();

        List<Double> equalWeights = IntStream.range(0, n).mapToObj(i -> 1.0).collect(toList());

        return weightedFit(polynomialDegree, equalWeights, xData, yData);
    }

    /**
     * @param polynomialDegree The degree of the fit polynomial (aka 2 = quadratic)
     * @param weights          The weights for each data point
     * @param xData            The list of X values
     * @param yData            The list of Y values
     *
     * @return A fitted PolynomialFunction (from org.apache.commons:commons-math3)
     */
    public static PolynomialFunction weightedFit(
            int polynomialDegree, List<Double> weights, List<Double> xData, List<Double> yData) {
        checkArgument(weights.size() == xData.size());
        checkArgument(xData.size() == yData.size());

        int n = xData.size();

        List<WeightedObservedPoint> pts = IntStream.range(0, n)
                .mapToObj(i -> new WeightedObservedPoint(weights.get(i), xData.get(i), yData.get(i)))
                .collect(toList());

        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(polynomialDegree);

        double[] polynomialCoef = pcf.fit(pts);

        return new PolynomialFunction(polynomialCoef);
    }
}
