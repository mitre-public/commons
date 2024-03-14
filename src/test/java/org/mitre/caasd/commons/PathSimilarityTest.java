package org.mitre.caasd.commons;

import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mitre.caasd.commons.PathSimilarity.*;

import org.junit.jupiter.api.Test;

class PathSimilarityTest {

    @Test
    public void testLongitudeDist() {

        assertThat(longitudeDelta(0.0, 1.0), is(1.0));
        assertThat(longitudeDelta(1.0, 0.0), is(1.0));

        assertThat(longitudeDelta(179.5, -179.5), is(1.0));
        assertThat(longitudeDelta(-179.5, 179.5), is(1.0));

        assertThat(longitudeDelta(0, -179.5), is(179.5));
        assertThat(longitudeDelta(-179.5, 0), is(179.5));

        assertThat(longitudeDelta(0, 22.5), is(22.5));
        assertThat(longitudeDelta(22.5, 0), is(22.5));
    }

    @Test
    public void testLatitudeDist() {

        assertThat(latitudeDelta(0.0, 1.0), is(1.0));
        assertThat(latitudeDelta(1.0, 0.0), is(1.0));

        assertThat(latitudeDelta(89.5, -89.5), is(179.0));
        assertThat(latitudeDelta(-89.5, 89.5), is(179.0));

        assertThat(latitudeDelta(0, -89.5), is(89.5));
        assertThat(latitudeDelta(-89.5, 0), is(89.5));

        assertThat(latitudeDelta(0, 22.5), is(22.5));
        assertThat(latitudeDelta(22.5, 0), is(22.5));
    }

    @Test
    public void testAccurateSimilarity() {

        LatLong a = LatLong.of(032.82810, -097.36291);
        LatLong b = LatLong.of(032.82960, -097.36299);
        LatLong c = LatLong.of(032.83143, -097.36338);
        LatLong d = LatLong.of(032.83332, -097.36362);

        LatLongPath path1 = new LatLongPath(a, b, c, d);

        LatLong e = LatLong.of(032.83482, -097.36409);
        LatLong f = LatLong.of(032.83645, -097.36448);
        LatLong g = LatLong.of(032.83801, -097.36487);
        LatLong h = LatLong.of(032.83951, -097.36511);

        LatLongPath path2 = new LatLongPath(e, f, g, h);

        assertThat(slowSimilarity(path1, path1), is(0.0));
        assertThat(slowSimilarity(path2, path2), is(0.0));

        double expectedDist = a.distanceInNM(e) + b.distanceInNM(f) + c.distanceInNM(g) + d.distanceInNM(h);

        assertThat(slowSimilarity(path1, path2), is(-expectedDist));
        assertThat(slowSimilarity(path1, path2), is(-expectedDist));
        assertThat(slowSimilarity(path1, path2), is(slowSimilarity(path2, path1)));
    }

    @Test
    public void testLatDist() {

        LatLong a = LatLong.of(032.82810, -097.36291);
        LatLong b = LatLong.of(032.82960, -097.36299);
        LatLong c = LatLong.of(032.83143, -097.36338);
        LatLong d = LatLong.of(032.83332, -097.36362);

        LatLongPath path1 = new LatLongPath(a, b, c, d);

        LatLong e = LatLong.of(032.83482, -097.36409);
        LatLong f = LatLong.of(032.83645, -097.36448);
        LatLong g = LatLong.of(032.83801, -097.36487);
        LatLong h = LatLong.of(032.83951, -097.36511);

        LatLongPath path2 = new LatLongPath(e, f, g, h);

        double actualLatDist = latitudeDist(path1.latitudes(), path2.latitudes());

        double expectLatDist = abs(a.latitude() - e.latitude())
                + abs(b.latitude() - f.latitude())
                + abs(c.latitude() - g.latitude())
                + abs(d.latitude() - h.latitude());

        assertEquals(actualLatDist, expectLatDist, 0.000000001);
    }

    @Test
    public void testLongDist() {

        LatLong a = LatLong.of(032.82810, -097.36291);
        LatLong b = LatLong.of(032.82960, -097.36299);
        LatLong c = LatLong.of(032.83143, -097.36338);
        LatLong d = LatLong.of(032.83332, -097.36362);

        LatLongPath path1 = new LatLongPath(a, b, c, d);

        LatLong e = LatLong.of(032.83482, -097.36409);
        LatLong f = LatLong.of(032.83645, -097.36448);
        LatLong g = LatLong.of(032.83801, -097.36487);
        LatLong h = LatLong.of(032.83951, -097.36511);

        LatLongPath path2 = new LatLongPath(e, f, g, h);

        double actualLongDist = longitudeDist(path1.longitudes(), path2.longitudes());

        double expectLongDist = abs(a.longitude() - e.longitude())
                + abs(b.longitude() - f.longitude())
                + abs(c.longitude() - g.longitude())
                + abs(d.longitude() - h.longitude());

        assertEquals(actualLongDist, expectLongDist, 0.000000001);
    }

    @Test
    public void testSimpleSimilarity() {

        LatLong a = LatLong.of(032.82810, -097.36291);
        LatLong b = LatLong.of(032.82960, -097.36299);
        LatLong c = LatLong.of(032.83143, -097.36338);
        LatLong d = LatLong.of(032.83332, -097.36362);

        LatLongPath path1 = new LatLongPath(a, b, c, d);

        LatLong e = LatLong.of(032.83482, -097.36409);
        LatLong f = LatLong.of(032.83645, -097.36448);
        LatLong g = LatLong.of(032.83801, -097.36487);
        LatLong h = LatLong.of(032.83951, -097.36511);

        LatLongPath path2 = new LatLongPath(e, f, g, h);

        // self similarity is always zero
        assertEquals(similarity(path1, path1), 0.0, 0.00000001);
        assertEquals(similarity(path2, path2), 0.0, 0.00000001);

        double expectedSim = -latitudeDist(path1.latitudes(), path2.latitudes())
                - longitudeDist(path1.longitudes(), path2.longitudes());

        assertThat(similarity(path1, path2), is(expectedSim));
        assertThat(similarity(path2, path1), is(expectedSim));
        assertThat(similarity(path1, path2), is(similarity(path2, path1)));
    }
}
