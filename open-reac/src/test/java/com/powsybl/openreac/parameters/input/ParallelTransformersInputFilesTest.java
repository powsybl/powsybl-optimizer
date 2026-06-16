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
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundles file now carries the topological membership only (num_bundle, num_branch, id),
 * for every member of every detected bundle. All numeric qualification happens in the AMPL
 * model, so there is nothing else to assert on this file.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class ParallelTransformersInputFilesTest {

    @Test
    void membershipSimpleParallel() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createSimpleParallel();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            int t1 = mapper.getInt(AmplSubset.BRANCH, "T1");
            int t2 = mapper.getInt(AmplSubset.BRANCH, "T2");
            String ref = String.join(System.lineSeparator(),
                "#num_bundle num_branch id",
                "1 " + t1 + " \"T1\"",
                "1 " + t2 + " \"T2\"") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void membershipTwoSeparateBundles() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createTwoSeparateBundles();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            // 2 bundles * 2 transformers = 4 data lines, every member written (no classification)
            assertTrue(data.startsWith("#num_bundle num_branch id"));
            long dataLines = data.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count();
            assertEquals(4, dataLines);
            // Each bundle index should appear on exactly two lines
            assertEquals(2, data.lines().filter(l -> l.startsWith("1 ")).count());
            assertEquals(2, data.lines().filter(l -> l.startsWith("2 ")).count());
        }
    }

    @Test
    void membershipEmptyWhenNoBundle() throws IOException {
        Network network = ParallelTransformersNetworkFactory.createNoTransformer();
        List<Set<String>> bundles = ParallelTwoWindingsTransformersDetector.detect(network);
        ParallelTwoWindingsTransformersBundles input = new ParallelTwoWindingsTransformersBundles(bundles);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = "#num_bundle num_branch id" + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }
}
