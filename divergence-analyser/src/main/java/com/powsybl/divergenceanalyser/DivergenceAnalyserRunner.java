/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.DivergenceAnalyserAmplIOFiles;
import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public final class DivergenceAnalyserRunner {

    private DivergenceAnalyserRunner() {
    }

    /**
     * Run the divergence analysis on the given network. It will NOT modify the network.
     * @param variantId the network variant to use. It will set the variant on the network.
     * @param parameters Parameters to customize the divergence analysis run.
     * @return All information about the run and possible modifications to apply.
     */
    public static DivergenceAnalyserResults run(Network network, String variantId, DivergenceAnalyserParameters parameters) {
        return run(network, variantId, parameters, new DivergenceAnalyserConfig(false), LocalComputationManager.getDefault());
    }

    /**
     * Run the divergence analysis on the given network. It will NOT modify the network.
     * @param variantId the network variant to use. It will set the variant on the network.
     * @param parameters Parameters to customize the Divergence Analysis run.
     * @param config allows debugging
     * @return All information about the run and possible modifications to apply.
     */
    public static DivergenceAnalyserResults run(Network network, String variantId, DivergenceAnalyserParameters parameters, DivergenceAnalyserConfig config, ComputationManager manager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        AmplModel divergenceAnalysis = DivergenceAnalyserModel.buildModel();
        DivergenceAnalyserAmplIOFiles amplIoInterface = new DivergenceAnalyserAmplIOFiles(parameters, config.isDebug());
        AmplResults run = AmplModelRunner.run(network, variantId, divergenceAnalysis, manager, amplIoInterface);
        return new DivergenceAnalyserResults(run.isSuccess(), amplIoInterface, run.getIndicators()); // TODO : Add the check of status // TODO : Define status of DA run
    }
}