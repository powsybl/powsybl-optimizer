package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.io.IOException;

public final class DivergenceAnalyser {

    private DivergenceAnalyser() {
    }

    public static DivergenceAnalyserResults getDivergenceAnalysisResults(Network network) throws IOException {
        return getDivergenceAnalysisResults(network, new DivergenceAnalyserParameters());
    }

    public static DivergenceAnalyserResults getDivergenceAnalysisResults(Network network, DivergenceAnalyserParameters parameters) throws IOException {
        DivergenceAnalyserResults divAnalysisResult;

        try (ComputationManager computationManager = new LocalComputationManager()) {
            divAnalysisResult = DivergenceAnalyserRunner.run(network,
                    network.getVariantManager().getWorkingVariantId(), parameters, new DivergenceAnalyserConfig(true),
                    computationManager);
        }

        return divAnalysisResult;
    }

}
