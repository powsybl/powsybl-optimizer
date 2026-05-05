/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.IntersectionStatus;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelGroup;
import org.junit.jupiter.api.Test;

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
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        assertTrue(groups.isEmpty());
    }

    @Test
    void simpleParallel() {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, groups.size());
        assertEquals(Set.of("T1", "T2"), groups.get(0));
    }

    @Test
    void threeParallel() {
        Network network = ParallelTransformersNetworkFactory.createThreeParallel();
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, groups.size());
        assertEquals(Set.of("T1", "T2", "T3"), groups.get(0));
    }

    @Test
    void complexParallelOnSquareCycle() {
        Network network = ParallelTransformersNetworkFactory.createSquareCycle();
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, groups.size());
        assertEquals(Set.of("T_AB", "T_BC", "T_CD", "T_DA"), groups.get(0));
    }

    @Test
    void twoSeparateGroupsAreNotMerged() {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateGroups();
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(2, groups.size());
        // Order is not guaranteed; check by content
        assertTrue(groups.contains(Set.of("T1", "T2")));
        assertTrue(groups.contains(Set.of("T3", "T4")));
    }

    @Test
    void transformersWithoutRtcAreFiltered() {
        Network network = ParallelTransformersNetworkFactory.createOneRtcOneFixed();
        List<Set<String>> groups = ParallelTwoWindingsTransformersDetector.detect(network);
        // Only T1 has an RTC, so the group falls below the size-2 threshold
        assertTrue(groups.isEmpty());
    }

    @Test
    void analyzeLargeIntersection() {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        assertEquals(1, groups.size());
        ParallelGroup g = groups.get(0);
        assertEquals(IntersectionStatus.LARGE, g.status());
        assertEquals(0.95, g.low(), 1e-6);
        assertEquals(1.05, g.high(), 1e-6);
    }

    @Test
    void analyzePointIntersection() {
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        assertEquals(1, groups.size());
        ParallelGroup g = groups.get(0);
        assertEquals(IntersectionStatus.POINT, g.status());
        assertEquals(1.000, g.low(), 1e-6);
        assertEquals(1.000, g.high(), 1e-6);
    }

    @Test
    void analyzeEmptyIntersection() {
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersection();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        assertEquals(1, groups.size());
        ParallelGroup g = groups.get(0);
        assertEquals(IntersectionStatus.EMPTY, g.status());
        // For EMPTY: low = max(rhoMin_i) = 1.01, high = min(rhoMax_i) = 0.99
        assertEquals(1.01, g.low(), 1e-6);
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
}
