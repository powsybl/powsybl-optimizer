/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.ampl.converter.AmplNetworkWriter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.IntersectionStatus;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelBundle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class ParallelTwoWindingsTransformersDetectorTest {

    @Test
    void noTransformer() {
        Network network = ParallelTransformersNetworkFactory.createNoTransformer();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertTrue(bundles.isEmpty());
    }

    @Test
    void simpleParallel() {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, bundles.size());
        assertEquals(Set.of("T1", "T2"), bundles.get(0));
    }

    @Test
    void threeParallel() {
        Network network = ParallelTransformersNetworkFactory.createThreeParallel();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, bundles.size());
        assertEquals(Set.of("T1", "T2", "T3"), bundles.get(0));
    }

    @Test
    void complexParallelOnTriangleCycle() {
        Network network = ParallelTransformersNetworkFactory.createTriangleCycle();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, bundles.size());
        assertEquals(Set.of("T1", "T2"), bundles.get(0));
    }

    @Test
    void complexParallelOnSquareCycle() {
        Network network = ParallelTransformersNetworkFactory.createSquareCycle();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, bundles.size());
        assertEquals(Set.of("T_AB", "T_BC", "T_CD", "T_DA"), bundles.get(0));
    }

    @Test
    void twoSeparateBundlesAreNotMerged() {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateBundles();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(2, bundles.size());
        // Order is not guaranteed; check by content
        assertTrue(bundles.contains(Set.of("T1", "T2")));
        assertTrue(bundles.contains(Set.of("T3", "T4")));
    }

    @Test
    void transformersWithoutRtcAreFiltered() {
        Network network = ParallelTransformersNetworkFactory.createOneRtcOneFixed();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        // Only T1 has an RTC, so the bundle falls below the size-2 threshold
        assertTrue(bundles.isEmpty());
    }

    @Test
    void analyzeLargeIntersection() {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.LARGE, g.status());
        assertEquals(0.95, g.low(), 1e-6);
        assertEquals(1.05, g.high(), 1e-6);
    }

    @Test
    void analyzePointIntersection() {
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.POINT, g.status());
        assertEquals(1.000, g.low(), 1e-6);
        assertEquals(1.000, g.high(), 1e-6);
    }

    @Test
    void analyzeEmptyIntersection() {
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.EMPTY, g.status());
        // For EMPTY: low = max(rhoMin_i) = 1.01, high = min(rhoMax_i) = 0.99
        assertEquals(1.01, g.low(), 1e-6);
        assertEquals(0.99, g.high(), 1e-6);
    }

    @Test
    void analyzeHybridPointWhenMemberFrozen() {
        // T1 optimisable, T2 not: T2 is pinned to its current tap (rho = 1.00, the mid step
        // of [0.95, 1.05]). That single point collapses to POINT what would be a LARGE bundle
        // if both were variable. Java-side equivalent of a member the optimiser cannot move
        // (AMPL znull / non-REGL_VAR member): a frozen member forces the shared ratio to its value.
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, Set.of("T1"));
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.POINT, g.status());
        assertEquals(1.000, g.low(), 1e-6);
        assertEquals(1.000, g.high(), 1e-6);
    }

    @Test
    void analyzeHybridEmptyWhenFrozenMemberOutOfReach() {
        // T1 optimisable in [0.95, 0.99]; T2 not optimisable, pinned at its current tap
        // (rho = 1.03, the mid step of [1.01, 1.05]). The frozen point 1.03 sits above T1's
        // whole domain -> disjoint -> EMPTY, with low = 1.03 (the pin) and high = 0.99.
        // Differs from the all-variable EMPTY (low = 1.01): the pin uses T2's current tap,
        // not its full range.
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, Set.of("T1"));
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.EMPTY, g.status());
        assertEquals(1.03, g.low(), 1e-6);
        assertEquals(0.99, g.high(), 1e-6);
    }

    @Test
    void mergeOverlappingSetsTransitive() {
        // Direct unit test of the merge utility
        List<Set<String>> input = List.of(
            Set.of("A", "B"),
            Set.of("B", "C"),
            Set.of("D", "E")
        );
        List<Set<String>> merged = ParallelTwoWindingsTransformersDetector.mergeOverlappingSets(input);
        assertEquals(2, merged.size());
        assertTrue(merged.contains(Set.of("A", "B", "C")));
        assertTrue(merged.contains(Set.of("D", "E")));
    }

    @Test
    void analyzeShiftedEffectiveIntersection() {
        // Identical raw tap domains [0.95, 1.05] but T2 carries cstRatio 225/222.75.
        // The intersection must be computed in effective space: [0.95 * 225/222.75, 1.05].
        // A raw-rho analysis would report [0.95, 1.05].
        Network network = ParallelTransformersNetworkFactory.createShiftedEffectiveIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.LARGE, g.status());
        assertEquals(0.95 * 225.0 / 222.75, g.low(), 1e-9);
        assertEquals(1.05, g.high(), 1e-9);
    }

    @Test
    void analyzeEmptyEffectiveIntersection() {
        // Identical raw tap domains, but cstRatios 1 vs 1.125 push the effective domains
        // apart: [0.95, 1.05] vs [1.06875, 1.18125] -> EMPTY. A raw-rho analysis would
        // classify this bundle LARGE and tie it, enforcing a constant effective-ratio
        // mismatch of 12.5% — exactly the circulating-flow situation to avoid.
        Network network = ParallelTransformersNetworkFactory.createEmptyEffectiveIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        assertEquals(1, bundles.size());
        ParallelBundle g = bundles.get(0);
        assertEquals(IntersectionStatus.EMPTY, g.status());
        assertEquals(0.95 * 1.125, g.low(), 1e-9);
        assertEquals(1.05, g.high(), 1e-9);
    }

    @Test
    void cstRatioMatchesAmplExporterContract() throws IOException {
        // cstRatio() duplicates the formula of the powsybl-core AMPL exporter
        // ("cst ratio (pu)" column of ampl_network_branches.txt). This test locks the
        // contract: if the exporter convention ever changes, this fails loudly instead
        // of letting the bundle analysis silently diverge from the AMPL model.
        Network network = ParallelTransformersNetworkFactory.createEmptyEffectiveIntersection();
        MemDataSource dataSource = new MemDataSource();
        new AmplNetworkWriter(network, dataSource,
            new AmplExportConfig(AmplExportConfig.ExportScope.ALL, false, AmplExportConfig.ExportActionType.CURATIVE))
            .write();
        String branches = new String(dataSource.getData("_network_branches", "txt"), StandardCharsets.UTF_8);
        for (String twtId : List.of("T1", "T2")) {
            // Columns: variant num bus1 bus2 3wt sub.1 sub.2 r x g1 g2 b1 b2 "cst ratio (pu)" ...
            String row = branches.lines()
                .filter(l -> !l.startsWith("#") && l.contains("\"" + twtId + "\""))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no exported row for " + twtId));
            double expected = Double.parseDouble(row.trim().split("\\s+")[13]);
            double actual = ParallelTwoWindingsTransformersDetector.cstRatio(network.getTwoWindingsTransformer(twtId));
            assertEquals(expected, actual, 1e-9, "cstRatio(" + twtId + ") diverges from the AMPL exporter");
        }
    }
}
