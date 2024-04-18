/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters;

import com.powsybl.stateestimator.parameters.input.knowledge.*;
import com.powsybl.stateestimator.parameters.input.options.StateEstimatorOptions;
import com.powsybl.stateestimator.parameters.input.options.SolvingOptions;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.stateestimator.parameters.output.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorAmplIOFiles implements AmplParameters {

    // Input files
    SolvingOptions solvingOptions;
    ActivePowerFlowMeasures activePowerFlowMeasures;
    ReactivePowerFlowMeasures reactivePowerFlowMeasures;
    ActivePowerInjectedMeasures activePowerInjectedMeasures;
    ReactivePowerInjectedMeasures reactivePowerInjectedMeasures;
    VoltageMagnitudeMeasures voltageMagnitudeMeasures;
    SuspectBranches suspectBranches;
    SlackBus slackBus;
    ZeroInjectionBuses zeroInjectionBuses;

    // Output files
    NetworkIndicatorsOutput networkIndicatorsOutput;
    StateVectorEstimateOutput stateVectorEstimateOutput;
    NetworkTopologyEstimateOutput networkTopologyEstimateOutput;
    MeasurementEstimatesAndResidualsOutput measurementEstimatesAndResidualsOutput;
    NetworkPowersEstimateOutput networkPowersEstimateOutput;


    boolean debug;

    public StateEstimatorAmplIOFiles(StateEstimatorKnowledge knowledge, StateEstimatorOptions options, boolean debug) {
        // Input files
        // Solver options
        this.solvingOptions = new SolvingOptions(options.getSolvingOptions());
        // Measurements
        this.activePowerFlowMeasures = new ActivePowerFlowMeasures(knowledge.getActivePowerFlowMeasures());
        this.reactivePowerFlowMeasures = new ReactivePowerFlowMeasures(knowledge.getReactivePowerFlowMeasures());
        this.activePowerInjectedMeasures = new ActivePowerInjectedMeasures(knowledge.getActivePowerInjectedMeasures());
        this.reactivePowerInjectedMeasures = new ReactivePowerInjectedMeasures(knowledge.getReactivePowerInjectedMeasures());
        this.voltageMagnitudeMeasures = new VoltageMagnitudeMeasures(knowledge.getVoltageMagnitudeMeasures());
        // Set of suspect branches
        this.suspectBranches = new SuspectBranches(knowledge.getSuspectBranches());
        // Reference angle bus (slack)
        this.slackBus = new SlackBus(knowledge.getSlackBus());
        // Set of zero-injection buses
        this.zeroInjectionBuses = new ZeroInjectionBuses(knowledge.getZeroInjectionBuses());

        // Output files
        this.networkIndicatorsOutput = new NetworkIndicatorsOutput();
        this.stateVectorEstimateOutput = new StateVectorEstimateOutput();
        this.networkTopologyEstimateOutput = new NetworkTopologyEstimateOutput();
        this.measurementEstimatesAndResidualsOutput = new MeasurementEstimatesAndResidualsOutput();
        this.networkPowersEstimateOutput = new NetworkPowersEstimateOutput();

        this.debug = debug;
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(solvingOptions, activePowerFlowMeasures, reactivePowerFlowMeasures,
                activePowerInjectedMeasures, reactivePowerInjectedMeasures, voltageMagnitudeMeasures,
                suspectBranches, slackBus, zeroInjectionBuses);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean hasConverged) {
        List<AmplOutputFile> list = new ArrayList<>();

        // TODO : add runIndicators
        // TODO : check if it is okay to save results even if hasConverged = false

        list.add(networkIndicatorsOutput);
        list.add(stateVectorEstimateOutput);
        list.add(networkTopologyEstimateOutput);
        list.add(measurementEstimatesAndResidualsOutput);
        list.add(networkPowersEstimateOutput);
        if (!hasConverged) {
            System.out.println("[WARNING] The state estimator has not converged. StateEstimatorResults will save last found estimates. [WARNING]");
        }
        return list;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    // Getters for output files
    public NetworkIndicatorsOutput getNetworkIndicatorsOutput() {
        return networkIndicatorsOutput;
    }

    public StateVectorEstimateOutput getStateVectorEstimateOutput() {
        return stateVectorEstimateOutput;
    }

    public NetworkTopologyEstimateOutput getNetworkTopologyEstimateOutput() {
        return networkTopologyEstimateOutput;
    }

    public MeasurementEstimatesAndResidualsOutput getMeasurementEstimatesAndResidualsOutput() {return measurementEstimatesAndResidualsOutput;}

    public NetworkPowersEstimateOutput getNetworkPowersEstimateOutput() {
        return networkPowersEstimateOutput;
    }

}
