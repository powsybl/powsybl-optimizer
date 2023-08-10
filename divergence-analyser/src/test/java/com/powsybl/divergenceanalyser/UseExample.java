/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class UseExample {

    void useExample() throws IOException {
        // Load your favorite network
        Network network = Network.read("your favorite network");

        // Lf parameters
        LoadFlowParameters parametersLf = new LoadFlowParameters();
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setAlwaysUpdateNetwork(true);

        // Verify LF diverges (otherwise, divergence analysis is useless)
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertFalse(loadFlowResult.isOk());

        // Defines the parameters of divergence analysis
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setYPenal(true)
                .setXiPenal(true)
                .setResolutionNlp()
                .setTargetVUnitsPenal(true)
                .setMaxTimeSolving(30);

        // print the results of divergence analysis
        DivergenceAnalyserResults results = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        results.printIndicators();
        results.printPenalizationPu();
    }
}
