package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.output.DivergenceAnalyserResults;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.ac.nr.NewtonRaphsonStoppingCriteriaType;
import com.powsybl.openloadflow.ac.nr.StateVectorScalingMode;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DivergenceAnalyserTest {

    @Test
    void divAnalyserTest() throws IOException {
        // Load your favorite network
        Network network = Network.read("your favorite network iidm");

        LoadFlowParameters parametersLf = new LoadFlowParameters();
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
                .setAlwaysUpdateNetwork(true);

        // Verify LF diverges before DA modifications (otherwise, DA is useless)
        LoadFlowResult loadFlowResult = LoadFlow.run(network, parametersLf);
        assertFalse(loadFlowResult.isOk());

        // Apply divergence analysis on it and apply modifications
        DivergenceAnalyserResults result = DivergenceAnalyser.getDivergenceAnalysisResults(network);
        result.applyDivAnalysisResults(network);

        // Verify LF converges after DA modifications
        loadFlowResult = LoadFlow.run(network, parametersLf);
        assertTrue(loadFlowResult.isOk());

        // Print indicators of DA run
        System.out.println(result.getIndicators());

    }
}
