/*
 *    Copyright 2026 The MITRE Corporation
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

package org.mitre.caasd.commons;

import static java.lang.Double.doubleToRawLongBits;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Raw-bit reproducibility contract for the geospatial numerics.
 *
 * <p>These vectors assert {@link Double#doubleToRawLongBits(double)} of representative geodetic
 * calculations against frozen golden values. Unlike the tolerance-based tests (which communicate
 * mathematical intent) this test protects <em>bit-for-bit</em> reproducibility. It exists to catch
 * silent numerical drift from:
 *
 * <ul>
 *   <li>reintroducing a {@code java.lang.Math} transcendental where {@code FastMath} is expected
 *       (e.g. {@code Math.toRadians}/{@code Math.toDegrees}, which differ from their FastMath
 *       counterparts by ~1 ulp on a large fraction of inputs);
 *   <li>an Apache Commons Math version bump that changes a FastMath algorithm or table;
 *   <li>a JDK change to a StrictMath-backed exact op.
 * </ul>
 *
 * <p>The golden values below were recorded against Apache Commons Math <b>3.6.1</b> on a Java 17+
 * (strict floating point, JEP 306) toolchain. The reproducibility contract is defined for the
 * tuple <em>(commons version + Commons Math version + supported Java major version)</em>. A change
 * to any element of that tuple that moves these bits is a deliberate, reviewable event: regenerate
 * the baseline with {@code -Dgeodesy.record=true}, paste the emitted literals below, and call out
 * the shift so downstream baselines (TTFS, Boogie, phao) can be rebased in step.
 *
 * <p>To regenerate after an intentional change:
 * <pre>{@code ./gradlew test --tests '*GeodesyRawBitVectorsTest' -Dgeodesy.record=true}</pre>
 */
public class GeodesyRawBitVectorsTest {

    /** When true, print fresh golden literals instead of asserting. */
    private static final boolean RECORD = Boolean.getBoolean("geodesy.record");

    /** A named calculation producing one or more doubles whose raw bits are pinned. */
    private record Vector(String name, double... values) {}

    /**
     * The corpus. Coverage follows the determinism contract: distance/course/projection, coincident
     * and zero-distance inputs, poles and near-pole inputs, antimeridian crossings, very small and
     * nearly-antipodal separations, the curved projection TTFS relies on, rhumb-line calculations,
     * and normalization boundaries.
     */
    private static List<Vector> vectors() {
        List<Vector> v = new ArrayList<>();

        // --- great-circle distance (nm) ---
        v.add(new Vector("distance/coincident", Spherical.distanceInNM(33.9425, -118.408, 33.9425, -118.408)));
        v.add(new Vector("distance/JFK-LAX", Spherical.distanceInNM(40.6413, -73.7781, 33.9425, -118.408)));
        v.add(new Vector("distance/tiny", Spherical.distanceInNM(0.0, 0.0, 0.0, 0.001)));
        v.add(new Vector("distance/near-antipodal", Spherical.distanceInNM(0.0, 0.0, 0.5, 179.5)));
        v.add(new Vector("distance/pole-to-pole", Spherical.distanceInNM(90.0, 0.0, -90.0, 0.0)));

        // --- initial course (deg) ---
        v.add(new Vector("course/JFK-LAX", Spherical.courseInDegrees(40.6413, -73.7781, 33.9425, -118.408)));
        v.add(new Vector("course/due-north", Spherical.courseInDegrees(0.0, 0.0, 10.0, 0.0)));
        v.add(new Vector("course/across-antimeridian", Spherical.courseInDegrees(10.0, 179.5, 10.0, -179.5)));

        // --- great-circle projection (lat, lon) ---
        LatLong pEast = Spherical.projectOut(0.0, 0.0, 90.0, 100.0);
        v.add(new Vector("projection/east-from-origin", pEast.latitude(), pEast.longitude()));
        LatLong pPole = Spherical.projectOut(89.9, 0.0, 0.0, 100.0);
        v.add(new Vector("projection/over-pole", pPole.latitude(), pPole.longitude()));
        LatLong pAnti = Spherical.projectOut(0.0, 179.9, 90.0, 50.0);
        v.add(new Vector("projection/crosses-antimeridian", pAnti.latitude(), pAnti.longitude()));

        // --- curved projection (the TTFS-relevant path) ---
        LatLong pCurved = Spherical.projectOut(33.9, -118.4, 45.0, 80.0, 0.0005);
        v.add(new Vector("projection/curved-TTFS", pCurved.latitude(), pCurved.longitude()));

        // --- rhumb line ---
        LatLong r1 = LatLong.of(40.0, -70.0);
        LatLong r2 = LatLong.of(42.0, -71.0);
        v.add(new Vector("rhumb/distance", Rhumb.rhumbDistance(r1, r2)));
        v.add(new Vector("rhumb/azimuth", Rhumb.rhumbAzimuth(r1, r2)));
        LatLong rEnd = Rhumb.rhumbEndPosition(r1, 0.5, 0.01);
        v.add(new Vector("rhumb/end-position", rEnd.latitude(), rEnd.longitude()));

        // --- normalization / clamping boundaries ---
        v.add(new Vector("mod/-190_360", Spherical.mod(-190.0, 360.0)));
        v.add(new Vector("mod/370_360", Spherical.mod(370.0, 360.0)));
        v.add(new Vector("mod/-180_360", Spherical.mod(-180.0, 360.0)));
        v.add(new Vector("clamp/lat-95", LatLong.clampLatitude(95.0)));
        v.add(new Vector("clamp/lon-neg190", LatLong.clampLongitude(-190.0)));

        // --- LatLong instance convenience paths (Distance/Course wrappers) ---
        v.add(new Vector(
                "latlong/distanceTo-nm",
                LatLong.of(40.6413, -73.7781)
                        .distanceTo(LatLong.of(33.9425, -118.408))
                        .inNauticalMiles()));
        v.add(new Vector(
                "latlong/courseTo-deg",
                LatLong.of(40.6413, -73.7781)
                        .courseTo(LatLong.of(33.9425, -118.408))
                        .inDegrees()));

        return v;
    }

    @Test
    public void rawBitVectorsAreStable() {
        if (RECORD) {
            StringBuilder sb = new StringBuilder("\n// ---- BEGIN recorded golden literals ----\n");
            for (Vector vec : vectors()) {
                sb.append("        GOLDEN.put(\"").append(vec.name()).append("\", new long[] {");
                double[] d = vec.values();
                for (int i = 0; i < d.length; i++) {
                    sb.append(i == 0 ? "" : ", ")
                            .append(doubleToRawLongBits(d[i]))
                            .append("L");
                }
                sb.append("});\n");
            }
            sb.append("        // ---- END recorded golden literals ----");
            System.out.println(sb);
            return;
        }

        for (Vector vec : vectors()) {
            long[] want = GOLDEN.get(vec.name());
            assertNotNull(want, () -> "No golden vector recorded for '" + vec.name() + "'");
            long[] got = new long[vec.values().length];
            for (int i = 0; i < got.length; i++) {
                got[i] = doubleToRawLongBits(vec.values()[i]);
            }
            assertArrayEquals(
                    want,
                    got,
                    () -> "Raw-bit drift in '" + vec.name()
                            + "'. If this change is intentional, regenerate with -Dgeodesy.record=true"
                            + " and notify downstream baseline owners (TTFS/Boogie/phao).");
        }
    }

    /**
     * Golden raw-long-bits, recorded against Commons Math 3.6.1 / Java 17+. Regenerate deliberately
     * with {@code -Dgeodesy.record=true} (see class javadoc).
     */
    private static final Map<String, long[]> GOLDEN = new LinkedHashMap<>();

    static {
        // ---- BEGIN recorded golden literals (Commons Math 3.6.1 / Java 17+) ----
        GOLDEN.put("distance/coincident", new long[] {0L});
        GOLDEN.put("distance/JFK-LAX", new long[] {4656934714103711784L});
        GOLDEN.put("distance/tiny", new long[] {4588808722267318817L});
        GOLDEN.put("distance/near-antipodal", new long[] {4667140329955577958L});
        GOLDEN.put("distance/pole-to-pole", new long[] {4667163656640794996L});
        GOLDEN.put("course/JFK-LAX", new long[] {4643525114549701575L});
        GOLDEN.put("course/due-north", new long[] {4645040803167600640L});
        GOLDEN.put("course/across-antimeridian", new long[] {4636027494060342865L});
        GOLDEN.put("projection/east-from-origin", new long[] {4367761920622715025L, 4610183959599437568L});
        GOLDEN.put("projection/over-pole", new long[] {4635923372968108810L, 4640537199629208566L});
        GOLDEN.put("projection/crosses-antimeridian", new long[] {4363259196380799776L, -4582860631832128932L});
        GOLDEN.put("projection/curved-TTFS", new long[] {4630097343014175659L, -4585422176628648767L});
        GOLDEN.put("rhumb/distance", new long[] {4585537556525948652L});
        GOLDEN.put("rhumb/azimuth", new long[] {-4623200976460421943L});
        GOLDEN.put("rhumb/end-position", new long[] {4630897082153721358L, -4588771134559635375L});
        GOLDEN.put("mod/-190_360", new long[] {4640185359819341824L});
        GOLDEN.put("mod/370_360", new long[] {4621819117588971520L});
        GOLDEN.put("mod/-180_360", new long[] {4640537203540230144L});
        GOLDEN.put("clamp/lat-95", new long[] {4636033603912859648L});
        GOLDEN.put("clamp/lon-neg190", new long[] {-4582834833314545664L});
        GOLDEN.put("latlong/distanceTo-nm", new long[] {4656934714103711784L});
        GOLDEN.put("latlong/courseTo-deg", new long[] {4643525114549701575L});
        // ---- END recorded golden literals ----
    }
}
