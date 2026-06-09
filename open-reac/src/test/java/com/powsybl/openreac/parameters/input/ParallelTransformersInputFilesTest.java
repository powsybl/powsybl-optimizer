/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTransformersNetworkFactory;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelBundle;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class ParallelTransformersInputFilesTest {

    @Test
    void parallelBundlesLargeOnly() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            // Both transformers have ranges [0.95, 1.05], so the bundle intersection is also [0.95, 1.05]
            String ref = String.join(System.lineSeparator(),
                "#num_bundle num_branch bundle_rho_min bundle_rho_max id",
                "1 " + t1 + " 0.950000 1.050000 \"T1\"",
                "1 " + t2 + " 0.950000 1.050000 \"T2\"") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void parallelBundlesExcludesPointAndEmpty() throws IOException {
        // POINT bundle -> should not appear in this file
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // Only the header and the trailing blank line — no data row
            String ref = "#num_bundle num_branch bundle_rho_min bundle_rho_max id" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void parallelBundlesTwoSeparateBundles() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateBundles();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // 2 bundles * 2 transfos = 4 data lines
            assertTrue(data.startsWith("#num_bundle num_branch bundle_rho_min bundle_rho_max id"));
            long dataLines = data.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count();
            assertEquals(4, dataLines);
            // Each bundle index should appear on exactly two lines
            assertEquals(2, data.lines().filter(l -> l.startsWith("1 ")).count());
            assertEquals(2, data.lines().filter(l -> l.startsWith("2 ")).count());
        }
    }

    @Test
    void fixedRatioForPointBundle() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(bundles, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // Both transformers fixed at 1.000000
            assertTrue(data.contains("1.000000"));
            long dataLines = data.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count();
            assertEquals(2, dataLines);
        }
    }

    @Test
    void fixedRatioForEmptyBundle() throws IOException {
        // T1 in [0.95, 0.99], T2 in [1.01, 1.05]. Center of gap = 1.00
        // T1 (entirely below 1.00) snaps to rhoMax = 0.99
        // T2 (entirely above) snaps to rhoMin = 1.01
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersection();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(bundles, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            assertTrue(data.contains(t1 + " 0.990000 \"T1\""), "T1 should be fixed at rhoMax = 0.99");
            assertTrue(data.contains(t2 + " 1.010000 \"T2\""), "T2 should be fixed at rhoMin = 1.01");
        }
    }

    @Test
    void fixedRatioForEmptyBundleWithStraddler() throws IOException {
        // Center of gap = 1.00. T1 (below) -> 0.99. T2 (above) -> 1.01
        // T3's domain [0.97, 1.03] contains 1.00 -> fixed at 1.00, not at one of its bounds
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersectionWithStraddler();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(bundles, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            int t3 = mapper.getInt(AmplSubset.BRANCH, "T3");
            assertTrue(data.contains(t1 + " 0.990000 \"T1\""), "T1 should snap to rhoMax = 0.99");
            assertTrue(data.contains(t2 + " 1.010000 \"T2\""), "T2 should snap to rhoMin = 1.01");
            assertTrue(data.contains(t3 + " 1.000000 \"T3\""), "T3 should be fixed at the gap center 1.00");
        }
    }

    @Test
    void fixedRatioEmptyForLargeOnly() throws IOException {
        // Only LARGE bundles present -> fixed ratio file should be empty (header only)
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelBundle> bundles = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network, null);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(bundles, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = "#num_branch fixed_rho id" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }
}
