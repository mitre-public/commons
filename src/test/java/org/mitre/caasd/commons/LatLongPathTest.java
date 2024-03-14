package org.mitre.caasd.commons;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import org.mitre.caasd.commons.util.IterPair;

import org.junit.jupiter.api.Test;

class LatLongPathTest {

    @Test
    public void basicConstructor() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1)
        );

        assertThat(path.get(0), is(LatLong.of(0.0, 0.1)));
        assertThat(path.get(1), is(LatLong.of(1.0, 1.1)));
    }

    @Test
    public void toAndFromBytes() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        LatLongPath path2 = LatLongPath.fromBytes(path.toBytes());

        assertThat(path.size(), is(path2.size()));
        for (int i = 0; i < path.size(); i++) {
            assertThat(path.get(i), is(path2.get(i)));
        }

        assertThat(path.equals(path2), is(true));
    }

    @Test
    public void toAndFromBase64() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        LatLongPath path2 = LatLongPath.fromBase64Str(path.toBase64());

        assertThat(path.size(), is(path2.size()));
        for (int i = 0; i < path.size(); i++) {
            assertThat(path.get(i), is(path2.get(i)));
        }

        assertThat(path.equals(path2), is(true));
    }

    @Test
    public void toArray() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        LatLong[] array = path.toArray();

        assertThat(array.length, is(3));
        assertThat(array[0], is(LatLong.of(0.0, 0.1)));
        assertThat(array[1], is(LatLong.of(1.0, 1.1)));
        assertThat(array[2], is(LatLong.of(2.0, 2.1)));
    }

    @Test
    public void toMatrix() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        double[][] matrix = path.toMatrix();

        assertThat(matrix[0].length, is(3));
        assertThat(matrix[1].length, is(3));
        assertThat(matrix[0], is(new double[]{0.0, 1.0, 2.0}));
        assertThat(matrix[1], is(new double[]{0.1, 1.1, 2.1}));

        //leading 0 = latitude
        assertThat(matrix[0][0], is(0.0));
        assertThat(matrix[0][1], is(1.0));
        assertThat(matrix[0][2], is(2.0));

        //leading 1 = longitude
        assertThat(matrix[1][0], is(0.1));
        assertThat(matrix[1][1], is(1.1));
        assertThat(matrix[1][2], is(2.1));
    }

    @Test
    public void pathDist() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1),
            LatLong.of(3.0, 3.1)
        );

        Distance dist = path.pathDistance();

        Distance manual = Distance.between(LatLong.of(0.0, 0.1), LatLong.of(1.0, 1.1))
            .plus(Distance.between(LatLong.of(1.0, 1.1), LatLong.of(2.0, 2.1)))
            .plus(Distance.between(LatLong.of(2.0, 2.1), LatLong.of(3.0, 3.1)));

        assertThat(dist, is(manual));
    }

    @Test
    public void legIteratorHasNothingWhenSmallPath() {

        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1)
        );

        Iterator<IterPair<LatLong>> legs = path.legIterator();
        assertThat(legs.hasNext(), is(false));
    }

    @Test
    public void avgLatLongIsCorrect() {

        //These points are 846.45952 Nautical Miles apart!
        //The "naive average location" will be WRONG
        LatLong one = LatLong.of(0.0, 10.0);
        LatLong two = LatLong.of(10.0, 20.0);

        LatLongPath path = LatLongPath.from(one, two);

        LatLong average = path.avgLatLong();

        Distance oneToAvg = one.distanceTo(average);
        Distance avgToTwo = average.distanceTo(two);
        Distance totalDist = one.distanceTo(two);

        //Both "half journeys" are the same length
        assertEquals(oneToAvg.inNauticalMiles(), avgToTwo.inNauticalMiles(), 0.00001);
        //The "whole distance" equals both halves added together
        assertEquals(totalDist.minus(oneToAvg).minus(avgToTwo).inNauticalMiles(), 0, 0.00001);
    }

    @Test
    public void quickAvgLatLong_simple() {

        //These points are 846.45952 Nautical Miles apart!
        //The "naive average location" will be WRONG
        LatLong one = LatLong.of(0.0, 10.0);
        LatLong two = LatLong.of(10.0, 20.0);

        LatLongPath path = LatLongPath.from(one, two);

        LatLong average = path.avgLatLong(); //accurately computed avg LatLong
        LatLong naiveAverage = LatLong.of(5.0, 15.0); //naive arithmetic average of LatLong

        assertThat(path.quickAvgLatLong(), is(naiveAverage));

        //the naive answer is off by over 2.5 NM!
        Distance realToNaive = average.distanceTo(naiveAverage);
        assertThat(realToNaive.isGreaterThan(Distance.ofNauticalMiles(2.5)), is(true));
    }


    @Test
    public void testAvgLatLong_acrossDateLine() {

        LatLong east = LatLong.of(0.0, -179.5); //just east of line
        LatLong west = LatLong.of(0.0, 179.0); //just west of line

        LatLongPath path = LatLongPath.from(east, west);

        LatLong naiveAverage = LatLong.of(0.0, -0.25); //naive arithmetic average of LatLong
        LatLong correctAverage = path.quickAvgLatLong();  //(0.0,179.75)

        System.out.println(correctAverage.toString());

        Distance distBtwPoints = east.distanceTo(west);

        //the distance between the east and west points is small (about 90.01 NM)
        assertThat(distBtwPoints.isLessThan(Distance.ofNauticalMiles(91.0)), is(true));
        assertThat(distBtwPoints.isGreaterThan(Distance.ofNauticalMiles(89.0)), is(true));

        //the distance between the average point and the east point is about 45.5 NM
        assertThat(correctAverage.distanceTo(east).isLessThan(Distance.ofNauticalMiles(45.5)),
            is(true));
        assertThat(correctAverage.distanceTo(east).isGreaterThan(Distance.ofNauticalMiles(44.5)),
            is(true));

        //the distance between the average point and the west point is about 45.5 NM
        assertThat(correctAverage.distanceTo(west).isLessThan(Distance.ofNauticalMiles(45.5)),
            is(true));
        assertThat(correctAverage.distanceTo(west).isGreaterThan(Distance.ofNauticalMiles(44.5)),
            is(true));

        //the distance from average to east = distance from average to west
        assertEquals(
            correctAverage.distanceTo(east).inNauticalMiles(),
            correctAverage.distanceTo(west).inNauticalMiles(),
            0.001
        );

        //the naive answer is literally on the other side of the planet
        Distance error = correctAverage.distanceTo(naiveAverage);
        assertThat(error.isGreaterThan(Distance.ofNauticalMiles(10_801)), is(true));
    }


    @Test
    public void testAvgLatLong_acrossDateLine_2() {

        LatLong east = LatLong.of(0.0, -179.5); //just east of line  (aka "180 - .5")
        LatLong west_1 = LatLong.of(0.0, 179.0); //just west of line (aka "180 + 1")
        LatLong west_2 = LatLong.of(0.0, 179.0); //just west of line (aka "180 + 1")

        LatLongPath path_1 = LatLongPath.from(east, west_1);
        LatLongPath path_2 = LatLongPath.from(east, west_1, west_2);

        LatLong midPoint = path_1.quickAvgLatLong();  //should be midpoint
        LatLong twoThirdPoint = path_2.quickAvgLatLong();  //should be 2/3rds towards west

        assertThat(midPoint, is(LatLong.of(0.0, 179.75)));
        assertThat(twoThirdPoint, is(LatLong.of(0.0, 179.5)));

        Distance distBtwPoints = east.distanceTo(west_1);

        //the distance between the east and west points is small (about 90.01 NM)
        assertThat(distBtwPoints.isLessThan(Distance.ofNauticalMiles(90.1)), is(true));
        assertThat(distBtwPoints.isGreaterThan(Distance.ofNauticalMiles(90.0)), is(true));

        //the distance between the average point and the east point is about 45.5 NM
        assertEquals(midPoint.distanceTo(east).inNauticalMiles(), 45.005, 0.001);

        //the distance between the average point and the west point is about 45.005 NM
        assertEquals(midPoint.distanceTo(west_1).inNauticalMiles(), 45.005, 0.001);

        //the distance from average to east = distance from average to west
        assertEquals(
            midPoint.distanceTo(east).inNauticalMiles(),
            midPoint.distanceTo(west_1).inNauticalMiles(),
            0.001
        );

        //the distance between the 2/3rd point and the east point is about 60.006 NM
        assertEquals(twoThirdPoint.distanceTo(east).inNauticalMiles(), 60.006, 0.001);

        //the distance between the 2/3rd point and the west point is about 30.003 NM
        assertEquals(twoThirdPoint.distanceTo(west_1).inNauticalMiles(), 30.003, 0.001);

        //the distance from 2/3rd point to east is twice the distance from 2/3rd point to west
        assertEquals(
            twoThirdPoint.distanceTo(east).inNauticalMiles(),
            twoThirdPoint.distanceTo(west_1).inNauticalMiles() * 2,
            0.001
        );
    }

    @Test
    public void testAvgLatLong_similarResults_differentMethods() {

        //total path length = 254.52942NM
        LatLongPath path = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1),
            LatLong.of(3.0, 3.1)
        );

        //compute the solution two ways..
        LatLong quickAverage = path.quickAvgLatLong();
        LatLong accurateAverage = path.avgLatLong();

        //solutions a NOT the same...but they are very similar given the length of the path involved
        assertNotEquals(quickAverage, accurateAverage);

        Distance pathDist = path.pathDistance();  //254.52942NM
        Distance delta = quickAverage.distanceTo(accurateAverage);  //0.03832NM

        assertThat(delta.inNauticalMiles() / pathDist.inNauticalMiles(), lessThan(0.001));
    }


    @Test
    public void supportEmptyPaths() {

        LatLongPath path = LatLongPath.from();

        assertThat(path.size(), is(0));
        assertThat(path.isEmpty(), is(true));
        assertThat(path.pathDistance(), is(Distance.ZERO));
        assertThat(path.toArray(), is(new LatLongPath[]{}));
        assertThat(path.toList(), hasSize(0));
        assertThat(path.toMatrix(), is(new double[2][0]));
        assertThat(path.latitudes(), is(new double[]{}));
        assertThat(path.longitudes(), is(new double[]{}));

        byte[] bytes = path.toBytes();
        LatLongPath path2 = LatLongPath.fromBytes(bytes);

        assertThat(path, is(path2));
    }


    @Test
    public void testJoiningMultiplePaths_varArgs() {

        LatLongPath path1 = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        LatLongPath path2 = LatLongPath.from(
            LatLong.of(3.0, 3.1),
            LatLong.of(4.0, 4.1)
        );

        LatLongPath path3 = LatLongPath.from(
            LatLong.of(5.0, 5.1)
        );

        LatLongPath fullPath = LatLongPath.join(path1, path2, path3);

        LatLongPath manualFull = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1),
            LatLong.of(3.0, 3.1),
            LatLong.of(4.0, 4.1),
            LatLong.of(5.0, 5.1)
        );

        assertThat(fullPath, is(manualFull));
    }


    @Test
    public void testJoiningMultiplePaths_collection() {

        LatLongPath path1 = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1)
        );

        LatLongPath path2 = LatLongPath.from(
            LatLong.of(3.0, 3.1),
            LatLong.of(4.0, 4.1)
        );

        LatLongPath path3 = LatLongPath.from(
            LatLong.of(5.0, 5.1)
        );

        List<LatLongPath> pathList = newArrayList(path1, path2, path3);
        LatLongPath fullPath = LatLongPath.join(pathList);

        LatLongPath manualFull = LatLongPath.from(
            LatLong.of(0.0, 0.1),
            LatLong.of(1.0, 1.1),
            LatLong.of(2.0, 2.1),
            LatLong.of(3.0, 3.1),
            LatLong.of(4.0, 4.1),
            LatLong.of(5.0, 5.1)
        );

        assertThat(fullPath, is(manualFull));
    }

    @Test
    public void testSubpath() {

        LatLong a = LatLong.of(0.0, 0.1);
        LatLong b = LatLong.of(1.0, 1.1);
        LatLong c = LatLong.of(2.0, 2.1);

        LatLongPath fullPath = LatLongPath.from(a, b, c);

        //full "copy subset" gives unique object with same data
        LatLongPath abc = fullPath.subpath(0, 3);
        assertThat(fullPath.equals(abc), is(true));
        assertThat(fullPath == abc, is(false));

        assertThat(fullPath.subpath(0, 0), is(LatLongPath.from()));
        assertThat(fullPath.subpath(0, 1), is(LatLongPath.from(a)));
        assertThat(fullPath.subpath(0, 2), is(LatLongPath.from(a, b)));
        assertThat(fullPath.subpath(0, 3), is(LatLongPath.from(a, b, c)));

        assertThat(fullPath.subpath(1, 1), is(LatLongPath.from()));
        assertThat(fullPath.subpath(1, 2), is(LatLongPath.from(b)));
        assertThat(fullPath.subpath(1, 3), is(LatLongPath.from(b, c)));

        assertThat(fullPath.subpath(2, 3), is(LatLongPath.from(c)));

        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(3, 1));
        assertThrows(IllegalArgumentException.class, () -> fullPath.subpath(0, 4));
    }
}