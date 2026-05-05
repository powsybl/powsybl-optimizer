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
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelGroup;
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
    void parallelGroupsLargeOnly() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        ParallelTwoWindingsTransformersGroups input = new ParallelTwoWindingsTransformersGroups(groups);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            // Both transformers have ranges [0.95, 1.05], so the group intersection is also [0.95, 1.05]
            String ref = String.join(System.lineSeparator(),
                "#num_group num_branch group_rho_min group_rho_max",
                "1 " + t1 + " 0.950000 1.050000",
                "1 " + t2 + " 0.950000 1.050000") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void parallelGroupsExcludesPointAndEmpty() throws IOException {
        // POINT group -> should not appear in this file
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        ParallelTwoWindingsTransformersGroups input = new ParallelTwoWindingsTransformersGroups(groups);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // Only the header and the trailing blank line — no data row
            String ref = "#num_group num_branch group_rho_min group_rho_max" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void parallelGroupsTwoSeparateGroups() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateGroups();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        ParallelTwoWindingsTransformersGroups input = new ParallelTwoWindingsTransformersGroups(groups);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // 2 groups * 2 transfos = 4 data lines
            assertTrue(data.startsWith("#num_group num_branch group_rho_min group_rho_max"));
            long dataLines = data.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count();
            assertEquals(4, dataLines);
            // Each group index should appear on exactly two lines
            assertEquals(2, data.lines().filter(l -> l.startsWith("1 ")).count());
            assertEquals(2, data.lines().filter(l -> l.startsWith("2 ")).count());
        }
    }

    @Test
    void fixedRatioForPointGroup() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createPointIntersection();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(groups, network);
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
    void fixedRatioForEmptyGroup() throws IOException {
        // T1 in [0.95, 0.99], T2 in [1.01, 1.05]. Center of gap = 1.00
        // T1 (entirely below 1.00) snaps to rhoMax = 0.99
        // T2 (entirely above) snaps to rhoMin = 1.01
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersection();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(groups, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            assertTrue(data.contains(t1 + " 0.990000"), "T1 should be fixed at rhoMax = 0.99");
            assertTrue(data.contains(t2 + " 1.010000"), "T2 should be fixed at rhoMin = 1.01");
        }
    }

    @Test
    void fixedRatioForEmptyGroupWithStraddler() throws IOException {
        // Center of gap = 1.00. T1 (below) → 0.99. T2 (above) → 1.01
        // T3's domain [0.97, 1.03] contains 1.00 → fixed at 1.00, not at one of its bounds
        Network network = ParallelTransformersNetworkFactory.createEmptyIntersectionWithStraddler();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(groups, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            int t3 = mapper.getInt(AmplSubset.BRANCH, "T3");
            assertTrue(data.contains(t1 + " 0.990000"), "T1 should snap to rhoMax = 0.99");
            assertTrue(data.contains(t2 + " 1.010000"), "T2 should snap to rhoMin = 1.01");
            assertTrue(data.contains(t3 + " 1.000000"), "T3 should be fixed at the gap center 1.00");
        }
    }

    @Test
    void fixedRatioEmptyForLargeOnly() throws IOException {
        // Only LARGE groups present -> fixed ratio file should be empty (header only)
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<ParallelGroup> groups = ParallelTwoWindingsTransformersDetector.detectAndAnalyze(network);
        FixedRatioTwoWindingsTransformers input = new FixedRatioTwoWindingsTransformers(groups, network);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = "#num_branch fixed_rho" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }
}
