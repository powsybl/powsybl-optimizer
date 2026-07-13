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
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTransformersNetworkFactory;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundles file carries the topological membership and the member orientation only
 * (num_bundle, num_branch, orientation, id), for every member of every orientable bundle.
 * All numeric qualification happens in the AMPL model, so there is nothing else to assert
 * on this file.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class ParallelTransformersInputFilesTest {

    @Test
    void membershipSimpleParallel() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        ParallelTwoWindingsTransformersDetector.DetectionResult detection = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(detection.bundles());
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            String ref = String.join(System.lineSeparator(),
                "#num_bundle num_branch orientation id",
                "1 " + t1 + " 1 \"T1\"",
                "1 " + t2 + " 1 \"T2\"") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void membershipTwoSeparateBundles() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateBundles();
        ParallelTwoWindingsTransformersDetector.DetectionResult detection = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(detection.bundles());
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // 2 bundles * 2 transformers = 4 data lines, every member written (no classification)
            assertTrue(data.startsWith("#num_bundle num_branch orientation id"));
            long dataLines = data.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count();
            assertEquals(4, dataLines);
            // Each bundle index should appear on exactly two lines
            assertEquals(2, data.lines().filter(l -> l.startsWith("1 ")).count());
            assertEquals(2, data.lines().filter(l -> l.startsWith("2 ")).count());
        }
    }

    @Test
    void membershipEmptyWhenGroupingIsOptedOut() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        OpenReacParameters parameters = new OpenReacParameters().setParallelTransformersGrouping(false);
        OpenReacAmplIOFiles io = new OpenReacAmplIOFiles(parameters, null, network, false, ReportNode.NO_OP);
        AmplInputFile input = io.getInputParameters().stream()
                .filter(f -> ParallelTwoWindingsTransformersBundles.PARAM_PARALLEL_TRANSFORMERS_FILE_NAME.equals(f.getFileName()))
                .findFirst().orElseThrow();
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // Detection is skipped entirely: the membership file is header-only, a no-op for AMPL,
            // even though the network does carry a detectable parallel bundle
            String ref = "#num_bundle num_branch orientation id" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void membershipEmptyWhenNoBundle() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createNoTransformer();
        ParallelTwoWindingsTransformersDetector.DetectionResult detection = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(detection.bundles());
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = "#num_bundle num_branch orientation id" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }
}
