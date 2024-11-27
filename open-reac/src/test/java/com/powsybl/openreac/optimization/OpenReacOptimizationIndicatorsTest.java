/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
public class OpenReacOptimizationIndicatorsTest extends AbstractOpenReacRunnerTest {

    // TODO : find some parameters that are working. Take things that seem good
    @Test
    void testInputIndicators() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        OpenReacParameters parameters = new OpenReacParameters()
                .setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE)
                .setObjectiveDistance(69)
                .setLogLevelAmpl(OpenReacAmplLogLevel.WARNING)
                .setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS)
                .setMinPlausibleLowVoltageLimit(0.7888)
                .setMaxPlausibleHighVoltageLimit(1.3455)
                .setReactiveSlackBusesMode(ReactiveSlackBusesMode.NO_GENERATION)
                .setActivePowerVariationRate(0.88)
                .setMinPlausibleActivePowerThreshold(0.45)
                .setLowImpedanceThreshold(1e-5)
                .setMinNominalVoltageIgnoredBus(2.)
                .setMinNominalVoltageIgnoredVoltageBounds(0.75)
                .setPQMax(3987.76)
                .setLowActivePowerDefaultLimit(12.32)
                .setHighActivePowerDefaultLimit(1452.66)
                .setDefaultQmaxPmaxRatio(0.24)
                .setDefaultMinimalQPRange(2.)
                .setDefaultVariableScalingFactor(1.1222)
                .setDefaultConstraintScalingFactor(0.7889)
                .setReactiveSlackVariableScalingFactor(0.2)
                .setTwoWindingTransformerRatioVariableScalingFactor(0.0045)
                .setShuntVariableScalingFactor(0.101);
        OpenReacResult result = runOpenReac(network, "", parameters, ReportNode.NO_OP);

        // verify buses outside main SC have been excluded
        assertEquals("INFO", result.getIndicators().get("log_level_ampl"));
        assertEquals("2", result.getIndicators().get("log_level_knitro"));
        assertEquals("2", result.getIndicators().get("objective_choice"));
        assertEquals("0.69", result.getIndicators().get("ratio_voltage_target"));
        assertEquals("2", result.getIndicators().get("coeff_alpha"));
        assertEquals("2", result.getIndicators().get("Pnull"));
        assertEquals("2", result.getIndicators().get("Znull"));
        assertEquals("2", result.getIndicators().get("epsilon_nominal_voltage"));
        assertEquals("2", result.getIndicators().get("min_plausible_low_voltage_limit"));
        assertEquals("2", result.getIndicators().get("max_plausible_high_voltage_limit"));
        assertEquals("2", result.getIndicators().get("ignore_voltage_bounds"));
        assertEquals("16", result.getIndicators().get("buses_with_reactive_slacks"));
        assertEquals("12", result.getIndicators().get("PQmax"));
        assertEquals("4", result.getIndicators().get("defaultPmax"));
        assertEquals("1", result.getIndicators().get("defaultPmin"));
        assertEquals("1", result.getIndicators().get("defaultQmaxPmaxRatio"));
        assertEquals("1", result.getIndicators().get("defaultQmin"));
        assertEquals("1", result.getIndicators().get("defaultQmax"));
        assertEquals("1", result.getIndicators().get("minimalQPrange"));
        assertEquals("1", result.getIndicators().get("default_variable_scaling_factor"));
        assertEquals("1", result.getIndicators().get("default_constraint_scaling_factor"));
        assertEquals("1", result.getIndicators().get("reactive_slack_variable_scaling_factor"));
        assertEquals("1", result.getIndicators().get("transformer_ratio_variable_scaling_factor"));
        assertEquals("1", result.getIndicators().get("shunt_variable_scaling_factor"));
    }

    @Test
    void testBusIndicators() throws IOException {
        Network network = HvdcNetworkFactory.createLccWithBiggerComponents();
        network.getBusBreakerView().getBus("b1").setV(400);
        OpenReacResult result = runOpenReac(network, "");

        // verify buses outside main SC have been excluded
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals("16", result.getIndicators().get("nb_substations"));
        assertEquals("16", result.getIndicators().get("nb_bus_in_data_file"));
        assertEquals("16", result.getIndicators().get("nb_bus_in_ACDC_CC"));
        assertEquals("12", result.getIndicators().get("nb_bus_in_AC_CC"));
        assertEquals("4", result.getIndicators().get("nb_bus_in_ACDC_but_out_AC_CC"));
        assertEquals("1", result.getIndicators().get("nb_bus_with_voltage_value"));
    }

    @Test
    void testBranchesIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithSimpleRemoteControl();
        network.getLine("l12").getTerminal2().disconnect();
        network.getLine("l24").getTerminal1().disconnect();
        OpenReacResult result = runOpenReac(network, "openreac-output-branches-opened");

        // verify opened branches have been excluded
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals("4", result.getIndicators().get("nb_branch_in_data_file"));
        assertEquals("2", result.getIndicators().get("nb_branch_in_AC_CC"));
        assertEquals("1", result.getIndicators().get("nb_branch_with_nonsmall_impedance"));
        assertEquals("1", result.getIndicators().get("nb_branch_with_zero_or_small_impedance"));
    }

    // TODO : unit excluded from opti and fixed

    @Test
    void testTransformersIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT1"));
        OpenReacResult result = runOpenReac(network, "", parameters, ReportNode.NO_OP);

        // verify only one rtc has been optimized
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals("1", result.getIndicators().get("nb_transformers_with_variable_ratio"));
        assertEquals("1", result.getIndicators().get("nb_transformers_with_fixed_ratio"));
    }

    // TODO : VSC and LCC converter stations

    @Test
    void testShunts() throws IOException {
        Network network = OpenReacOptimizationAndLoadFlowTest.create();
        // add one shunt that will be fixed in optimization
        network.getVoltageLevel("vl3").newShuntCompensator()
                .setId("SHUNT2")
                .setBus("b3")
                .setConnectableBus("b3")
                .setSectionCount(0)
                .setVoltageRegulatorOn(true)
                .setTargetV(393)
                .setTargetDeadband(5.0)
                .newLinearModel()
                .setMaximumSectionCount(25)
                .setBPerSection(1e-3)
                .add()
                .add();
        // add one shunt that will not be considered in optimization, as it is neither optimized nor connected
        network.getVoltageLevel("vl3").newShuntCompensator()
                .setId("SHUNT3")
                // .setBus("b3")
                .setConnectableBus("b3")
                .setSectionCount(0)
                .setVoltageRegulatorOn(true)
                .setTargetV(393)
                .setTargetDeadband(5.0)
                .newLinearModel()
                .setMaximumSectionCount(25)
                .setBPerSection(1e-3)
                .add()
                .add();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableShuntCompensators(List.of("SHUNT"));
        OpenReacResult result = runOpenReac(network, "");

        // verify only one shunt has been optimized
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals("3", result.getIndicators().get("nb_shunt_in_data_file"));
        assertEquals("3", result.getIndicators().get("nb_shunt_connectable_or_in_AC_CC"));
        assertEquals("1", result.getIndicators().get("nb_shunt_with_fixed_value"));
        assertEquals("1", result.getIndicators().get("nb_shunt_with_variable_value")); // FIXME
    }

    @Test
    void testSvcIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        OpenReacResult result = runOpenReac(network, "");

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals("1", result.getIndicators().get("nb_svc_in_data_file"));
        assertEquals("1", result.getIndicators().get("nb_svc_in_AC_CC"));
        assertEquals("0", result.getIndicators().get("nb_svc_up_and_operating"));
    }

}
