/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.io.IOException;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public final class DivergenceAnalyser {

    private DivergenceAnalyser() {
    }

    /**
     * @param network the network on which the divergence analysis is applied.
     * @return the results of the divergence analysis, without penalties.
     */
    public static DivergenceAnalyserResults runDivergenceAnalysis(Network network) throws IOException {
        return runDivergenceAnalysis(network, network.getVariantManager().getWorkingVariantId(), new DivergenceAnalyserParameters(),
                new DivergenceAnalyserConfig(false), new LocalComputationManager());
    }

    /**
     * @param network the network on which the divergence analysis is applied.
     * @param parameters the divergence analysis parameters (penalization, solving options) used.
     * @return the results of the divergence analysis, with the given penalties and solving options.
     */
    public static DivergenceAnalyserResults runDivergenceAnalysis(Network network, String variantId, DivergenceAnalyserParameters parameters, DivergenceAnalyserConfig config, ComputationManager manager) {
        return DivergenceAnalyserRunner.run(network, variantId, parameters, config, manager);
    }

}
