/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Topological detection only. The numeric qualification of a bundle (shared-ratio interval,
 * tie / fix / release) is performed in the AMPL model and validated there.
 *
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
