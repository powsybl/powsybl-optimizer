/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.stateestimator.parameters.StateEstimatorAmplIOFiles;
import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.ampl.executor.AmplModel;
import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public final class StateEstimatorRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateEstimatorRunner.class);

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
    public static StateEstimatorResults run(Network network, String variantId, StateEstimatorKnowledge knowledge, StateEstimatorOptions options, String estimatorType) {
        return run(network, variantId, knowledge, options, new StateEstimatorConfig(false), LocalComputationManager.getDefault(), estimatorType);
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
     * @param estimatorType The type of the estimator used: Weighted Least Squares (WLS) or Weighted Least Absolute Values (WLAV)
     * @param config Allows debugging
     * @return All information about the run and possible modifications to apply.
     */
    public static StateEstimatorResults run(Network network, String variantId, StateEstimatorKnowledge knowledge, StateEstimatorOptions options, StateEstimatorConfig config, ComputationManager manager, String estimatorType) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(variantId);
        Objects.requireNonNull(knowledge);
        Objects.requireNonNull(options);
        Objects.requireNonNull(config);
        Objects.requireNonNull(manager);
        Objects.requireNonNull(estimatorType);
        // Change name of every TwoWindingTransformer in the network
        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
            twt.setName("isTwoWindingTransformer");
        }
        // Make sure lines disconnected on one side are fully disconnected
        network.getLineStream()
                .filter(line -> line.getTerminal1().isConnected() ^ line.getTerminal2().isConnected())
                .forEach(Connectable::disconnect);

        AmplExportConfig amplExportConfig = new AmplExportConfig(AmplExportConfig.ExportScope.ALL, true, AmplExportConfig.ExportActionType.CURATIVE);

        // Build AMPL interface and run
        AmplModel stateEstimation = StateEstimatorModel.buildModel(estimatorType); // Only AMPL files (.dat,.mod,.run) that never change should be given during this step
        StateEstimatorAmplIOFiles amplIoInterface = new StateEstimatorAmplIOFiles(knowledge, options, config.isDebug(), amplExportConfig);
        AmplResults run = AmplModelRunner.run(network, variantId, stateEstimation, manager, amplIoInterface);
        StateEstimatorResults stateEstimatorResults = new StateEstimatorResults(run.isSuccess(), amplIoInterface, run.getIndicators());
        // Complete attributes of StateEstimatorResults (measurements extended with estimates and residuals returned by the state estimation)
        stateEstimatorResults.setActivePowerFlowMeasuresExtended(new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures(), stateEstimatorResults.measurementEstimatesAndResiduals));
        stateEstimatorResults.setReactivePowerFlowMeasuresExtended(new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures(), stateEstimatorResults.measurementEstimatesAndResiduals));
        stateEstimatorResults.setActivePowerInjectedMeasuresExtended(new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures(), stateEstimatorResults.measurementEstimatesAndResiduals));
        stateEstimatorResults.setReactivePowerInjectedMeasuresExtended(new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures(), stateEstimatorResults.measurementEstimatesAndResiduals));
        stateEstimatorResults.setVoltageMagnitudeMeasuresExtended(new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures(), stateEstimatorResults.measurementEstimatesAndResiduals));

        // If FILTER mode is used, warn the user about abnormalities detected by the filter
        if (estimatorType.equals("FILTER")) {
            // Warn the user about any gross measurement error
            for (var measure : stateEstimatorResults.getActivePowerFlowMeasuresExtended().getMeasuresWithEstimatesAndResiduals().entrySet()) {
                if (Double.parseDouble(measure.getValue().get(7)) == 999999999) {
                    String warning = "Measurement n°" + measure.getKey() + " is considered abnormal by the filter. It should be removed.";
                    LOGGER.warn(warning);
                }
            }
            for (var measure : stateEstimatorResults.getReactivePowerFlowMeasuresExtended().getMeasuresWithEstimatesAndResiduals().entrySet()) {
                if (Double.parseDouble(measure.getValue().get(7)) == 999999999) {
                    String warning = "Measurement n°" + measure.getKey() + " is considered abnormal by the filter. It should be removed.";
                    LOGGER.warn(warning);
                }
            }
            for (var measure : stateEstimatorResults.getActivePowerInjectedMeasuresExtended().getMeasuresWithEstimatesAndResiduals().entrySet()) {
                if (Double.parseDouble(measure.getValue().get(5)) == 999999999) {
                    String warning = "Measurement n°" + measure.getKey() + " is considered abnormal by the filter. It should be removed.";
                    LOGGER.warn(warning);
                }
            }
            for (var measure : stateEstimatorResults.getReactivePowerInjectedMeasuresExtended().getMeasuresWithEstimatesAndResiduals().entrySet()) {
                if (Double.parseDouble(measure.getValue().get(5)) == 999999999) {
                    String warning = "Measurement n°" + measure.getKey() + " is considered abnormal by the filter. It should be removed.";
                    LOGGER.warn(warning);
                }
            }
            for (var measure : stateEstimatorResults.getVoltageMagnitudeMeasuresExtended().getMeasuresWithEstimatesAndResiduals().entrySet()) {
                if (Double.parseDouble(measure.getValue().get(5)) == 999999999) {
                    String warning = "Measurement n°" + measure.getKey() + " is considered abnormal by the filter. It should be removed.";
                    LOGGER.warn(warning);
                }
            }
            // Warn the user about any topology error
            for (var branch : stateEstimatorResults.getNetworkTopologyEstimate()) {
                if (!branch.getPresumedStatus().equals("PRESUMED " + branch.getEstimatedStatus())) {
                    String warning = "Status of branch " + branch.getBranchId() + " is considered abnormal by the filter. It should be switched to: "
                            + branch.getEstimatedStatus();
                    LOGGER.warn(warning);
                }
            }
        }
        return stateEstimatorResults;
    }
}
