/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.network.VoltageControlNetworkFactory;
import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class NoPenalizationTest {

    void assertNoPenalization(DivergenceAnalyserResults results) {
        // Verify run indicators
        assertEquals(results.getRunIndicators().get(1).getSecond(), "OK");
        assertEquals(results.getRunIndicators().get(3).getSecond(), "NO");

        // Verify penalization indicators
        for (Pair<String, String> indicator : results.getPenalizationIndicators()) {
            assertEquals(indicator.getSecond(), "0");
        }

        // Verify there is no penalization
        assertEquals(results.getBusPenalization().size(), 0);
        assertEquals(results.getBranchPenalization().size(), 0);
    }

    void noUselessPenalizationDivergenceAnalysis(Network network) throws IOException {
        // Verify load flow calculations converge
        LoadFlowResult loadFlowResult = LoadFlow.run(network, new LoadFlowParameters());
        assertTrue(loadFlowResult.isOk());

        // Verify DA results uses no penalization ...
        // ... with MINLP resolution and no penal activated
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        DivergenceAnalyserResults result = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        assertNoPenalization(result);
        result.printIndicators();

        // ... with MINLP resolution and all penal activated
        parameters.setAllPenalization(true);
        result = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        assertNoPenalization(result);

        // ... with NLP resolution and all penal activated
        parameters.setResolutionNlp();
        result = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        assertNoPenalization(result);

        // ... with NLP resolution and no penal activated
        parameters.setAllPenalization(false);
        result = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        assertNoPenalization(result);
    }

    /**
     * Test that divergence analysis does converge without penalty for consistent ieee14 matpower network.
     */
    @Test
    void testConsistentIEEE14() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        noUselessPenalizationDivergenceAnalysis(network);
    }

    /**
     * Test that divergence analysis does converge without penalty for consistent small network.
     */
    @Test
    void testSmallNetwork() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithShuntAndGeneratorVoltageControl();
        noUselessPenalizationDivergenceAnalysis(network);
    }

    /**
     * Test that divergence analysis does converge without penalty for consistent handmade network with 3wt.
     */
    @Test
    void testSmallNetworkWith3wt() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT3wt();
        noUselessPenalizationDivergenceAnalysis(network);
    }
}
