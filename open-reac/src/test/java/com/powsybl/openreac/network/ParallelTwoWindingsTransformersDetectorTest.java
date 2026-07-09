/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.Bundle;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.DetectionResult;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.Member;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Topological detection and orientation only. The numeric qualification of a bundle
 * (shared-ratio interval, tie / fix / release) is performed in the AMPL model and
 * validated there.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class ParallelTwoWindingsTransformersDetectorTest {

    private static Set<String> ids(Bundle bundle) {
        return bundle.members().stream().map(Member::transformerId).collect(Collectors.toSet());
    }

    private static Map<String, Integer> orientations(Bundle bundle) {
        return bundle.members().stream().collect(Collectors.toMap(Member::transformerId, Member::orientation));
    }

    @Test
    void noTransformer() {
        Network network = ParallelTransformersNetworkFactory.createNoTransformer();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertTrue(result.bundles().isEmpty());
        assertTrue(result.undecidedBundles().isEmpty());
    }

    @Test
    void simpleParallel() {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        // Homogeneous declaration -> every member aligned with the reference (+1)
        assertEquals(Map.of("T1", 1, "T2", 1), orientations(result.bundles().get(0)));
    }

    @Test
    void threeParallel() {
        Network network = ParallelTransformersNetworkFactory.createThreeParallel();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        assertEquals(Map.of("T1", 1, "T2", 1, "T3", 1), orientations(result.bundles().get(0)));
    }

    @Test
    void complexParallelOnTriangleCycle() {
        Network network = ParallelTransformersNetworkFactory.createTriangleCycle();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        assertEquals(Map.of("T1", 1, "T2", 1), orientations(result.bundles().get(0)));
    }

    @Test
    void complexParallelOnSquareCycle() {
        Network network = ParallelTransformersNetworkFactory.createSquareCycle();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        // The factory alternates the declaration direction around the cycle: T_AB and T_CD
        // are declared HV->LV, T_BC and T_DA LV->HV. Reference (first id) is T_AB.
        assertEquals(Map.of("T_AB", 1, "T_BC", -1, "T_CD", 1, "T_DA", -1),
                orientations(result.bundles().get(0)));
    }

    @Test
    void twoSeparateBundlesAreNotMerged() {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateBundles();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(2, result.bundles().size());
        // Order is not guaranteed; check by content
        List<Set<String>> idSets = result.bundles().stream().map(ParallelTwoWindingsTransformersDetectorTest::ids).toList();
        assertTrue(idSets.contains(Set.of("T1", "T2")));
        assertTrue(idSets.contains(Set.of("T3", "T4")));
    }

    @Test
    void transformersWithoutRtcAreFiltered() {
        Network network = ParallelTransformersNetworkFactory.createOneRtcOneFixed();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        // Only T1 has an RTC, so the bundle falls below the size-2 threshold
        assertTrue(result.bundles().isEmpty());
        assertTrue(result.undecidedBundles().isEmpty());
    }

    @Test
    void antiParallelDeclarationIsOriented() {
        Network network = ParallelTransformersNetworkFactory.createAntiParallel();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        // T2 is declared with swapped terminals relative to T1 (the reference)
        assertEquals(Map.of("T1", 1, "T2", -1), orientations(result.bundles().get(0)));
        assertTrue(result.undecidedBundles().isEmpty());
    }

    @Test
    void antiParallelTriangleIsOrientedAndKept() {
        Network network = ParallelTransformersNetworkFactory.createAntiParallelTriangle();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        // Real-data pattern: an HV->LV member looped with two LV->HV twins through another
        // HV bus. Reference (first id) is T1; T2 and T3 are reversed relative to it.
        assertEquals(Map.of("T1", 1, "T2", -1, "T3", -1), orientations(result.bundles().get(0)));
        assertTrue(result.undecidedBundles().isEmpty());
    }

    @Test
    void equalVoltageSimpleParallelIsOrientedByBusPair() {
        Network network = ParallelTransformersNetworkFactory.createEqualVoltageAntiParallel();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        assertEquals(1, result.bundles().size());
        // Degenerate nominal-voltage pair: orientation falls back to the terminal-1 bus,
        // still able to spot that T2 is declared reversed
        assertEquals(Map.of("T1", 1, "T2", -1), orientations(result.bundles().get(0)));
    }

    @Test
    void equalVoltageCycleIsUndecided() {
        Network network = ParallelTransformersNetworkFactory.createEqualVoltageSquareCycle();
        DetectionResult result = ParallelTwoWindingsTransformersDetector.detect(network);
        // Coupling transformers in a cycle: no nominal-voltage side and no shared bus
        // pair to orient the members -> released, reported as undecided
        assertTrue(result.bundles().isEmpty());
        assertEquals(1, result.undecidedBundles().size());
        assertEquals(Set.of("T_AB", "T_BC", "T_CD", "T_DA"), result.undecidedBundles().get(0));
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
