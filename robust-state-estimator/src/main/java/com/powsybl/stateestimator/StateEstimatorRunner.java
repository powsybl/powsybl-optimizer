/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.stateestimator.parameters.StateEstimatorAmplIOFiles;
import com.powsybl.stateestimator.parameters.input.knowledge.StateEstimatorKnowledge;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public final class StateEstimatorRunner {

    private StateEstimatorRunner() {
    }

    /**
     * Run the state estimation on the given network.
     * <p>
     * Note : network will only be modified in two ways. First, names for TwoWindingTransformers will be changed.
     * Second, all lines partially disconnected will be fully disconnected before running the state estimation.
     * </p>
     * @param variantId The network variant to use. It will set the variant on the network.
     * @param knowledge The knowledge on the network (measurements, set of suspected branches) used by the state estimation.
     * @param options The state estimation options (solving options) used.
     * @return All information about the run and possible modifications to apply.
     */
    public static StateEstimatorResults run(Network network, String variantId, StateEstimatorKnowledge knowledge, StateEstimatorOptions options) {
        return run(network, variantId, knowledge, options, new StateEstimatorConfig(false), LocalComputationManager.getDefault());
    }

    /**
     * Run the state estimation on the given network.
     * <p>
     * Note : network will only be modified in two ways. First, names for TwoWindingTransformers will be changed.
     * Second, all lines partially disconnected will be fully disconnected before running the state estimation.
     * </p>
     * @param variantId The network variant to use. It will set the variant on the network.
     * @param knowledge The knowledge on the network (measurements, set of suspected branches) used by the state estimation.
     * @param options The state estimation parameters (solving options) used.
     * @param config Allows debugging
     * @return All information about the run and possible modifications to apply.
     */
    public static StateEstimatorResults run(Network network, String variantId, StateEstimatorKnowledge knowledge, StateEstimatorOptions options, StateEstimatorConfig config, ComputationManager manager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(knowledge);
        Objects.requireNonNull(options);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        // Change name of every TwoWindingTransformer in the network
        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
            twt.setName("isTwoWindingTransformer");
        }
        // Make sure lines disconnected on one side are fully disconnected
        network.getLineStream()
                .filter(line -> line.getTerminal1().isConnected() ^ line.getTerminal2().isConnected())
                .forEach(Connectable::disconnect);
        // Build AMPL interface and run
        AmplModel stateEstimation = StateEstimatorModel.buildModel(); // Only AMPL files (.dat,.mod,.run) that never change should be given during this step
        StateEstimatorAmplIOFiles amplIoInterface = new StateEstimatorAmplIOFiles(knowledge, options, config.isDebug());
        AmplResults run = AmplModelRunner.run(network, variantId, stateEstimation, manager, amplIoInterface);
        return new StateEstimatorResults(run.isSuccess(), amplIoInterface, run.getIndicators());
    }
}
