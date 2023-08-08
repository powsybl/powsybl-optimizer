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
import com.powsybl.openloadflow.ac.nr.NewtonRaphsonStoppingCriteriaType;
import com.powsybl.openloadflow.ac.nr.StateVectorScalingMode;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class UseCaseTest {

    private LoadFlowParameters parametersLf;

    @BeforeEach
    void setup() {
        parametersLf = new LoadFlowParameters();
        parametersLf.setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES) // DC_VALUES for usual run
                .setDistributedSlack(false)
                .setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN)
                .setUseReactiveLimits(false)
                .setReadSlackBus(true)
                .setTransformerVoltageControlOn(false)
                .setShuntCompensatorVoltageControlOn(false)
                .setPhaseShifterRegulationOn(false);

        // Parameters for OLF
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parametersLf);
        parametersExt.setNewtonRaphsonStoppingCriteriaType(NewtonRaphsonStoppingCriteriaType.PER_EQUATION_TYPE_CRITERIA)
                .setStateVectorScalingMode(StateVectorScalingMode.LINE_SEARCH)
                .setVoltageRemoteControl(false)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
                .setVoltageInitModeOverride(OpenLoadFlowParameters.VoltageInitModeOverride.NONE)
                .setTransformerVoltageControlMode(OpenLoadFlowParameters.TransformerVoltageControlMode.INCREMENTAL_VOLTAGE_CONTROL)
                .setShuntVoltageControlMode(OpenLoadFlowParameters.ShuntVoltageControlMode.INCREMENTAL_VOLTAGE_CONTROL)
                .setPhaseShifterControlMode(OpenLoadFlowParameters.PhaseShifterControlMode.INCREMENTAL)
                .setLowImpedanceBranchMode(OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_ZERO_IMPEDANCE_LINE)
                .setAlwaysUpdateNetwork(true)
                .setMaxNewtonRaphsonIterations(30);
    }

    void useCase() throws IOException {
        // Load your favorite network
        Network network = Network.read("your favorite network");

        // Verify LF diverges before DA modifications (otherwise, DA is useless)
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertFalse(loadFlowResult.isOk());

        // Defines the parameters of DA
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        parameters.setG1Penal(true)
                .setG2Penal(true)
                .setB2Penal(true)
                .setB1Penal(true)
                .setResolutionNlp();

        DivergenceAnalyserResults result = DivergenceAnalyser.runDivergenceAnalysis(network, parameters);
        result.printIndicators();
        result.printPenalization();
    }
}
