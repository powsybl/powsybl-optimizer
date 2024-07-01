/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.stateestimator.parameters.input.knowledge.StateEstimatorKnowledge;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.io.IOException;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public final class StateEstimator {

    private StateEstimator() {
    }

    /**
     * @param network The network on which the state estimation is applied.
     * @param knowledge The knowledge on the network (measurements, set of suspected branches) used by the state estimation.
     * @return The results of the state estimation, without any measurements or suspected branches provided
     */
    public static StateEstimatorResults runStateEstimation(Network network, StateEstimatorKnowledge knowledge) throws IOException {
        return runStateEstimation(network, network.getVariantManager().getWorkingVariantId(), knowledge, new StateEstimatorOptions(),
                new StateEstimatorConfig(false), new LocalComputationManager());
    }

    /**
     * @param network The network on which the state estimation is applied.
     * @param knowledge The knowledge on the network (measurements, set of suspected branches) used by the state estimation.
     * @param options The state estimation parameters (solving options) used.
     * @return The results of the state estimation, with the given penalties and solving options.
     */
    public static StateEstimatorResults runStateEstimation(Network network, String variantId, StateEstimatorKnowledge knowledge, StateEstimatorOptions options, StateEstimatorConfig config, ComputationManager manager) {
        return StateEstimatorRunner.run(network, variantId, knowledge, options, config, manager);
    }

}
