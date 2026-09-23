/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.network.ShuntNetworkFactory;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the indicators returned by OpenReac optimization.
 * Note that some indicators might depend on optimization process,
 * and can not be tested in unit tests.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpenReacOptimizationIndicatorsTest extends AbstractOpenReacRunnerTest {

    @Test
    void testParametrizationIndicators() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        OpenReacParameters parameters = new OpenReacParameters()
                .setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE)
                .setObjectiveDistance(70)
                .setLogLevelAmpl(OpenReacAmplLogLevel.WARNING)
                .setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS)
                .setMinPlausibleLowVoltageLimit(0.655)
                .setMaxPlausibleHighVoltageLimit(1.425)
                .setReactiveSlackBusesMode(ReactiveSlackBusesMode.NO_GENERATION)
                .setActivePowerVariationRate(0.999)
                .setMinPlausibleActivePowerThreshold(0.02)
                .setLowImpedanceThreshold(1e-5)
                .setMinNominalVoltageIgnoredBus(0.75)
                .setMinNominalVoltageIgnoredVoltageBounds(0.85)
                .setPQMax(4001)
                .setLowActivePowerDefaultLimit(0.1)
                .setHighActivePowerDefaultLimit(1001)
                .setDefaultQmaxPmaxRatio(0.2)
                .setDefaultMinimalQPRange(2.)
                .setDefaultVariableScalingFactor(1.001)
                .setDefaultConstraintScalingFactor(0.999)
                .setReactiveSlackVariableScalingFactor(1e-2)
                .setTwoWindingTransformerRatioVariableScalingFactor(2e-3)
                .setShuntVariableScalingFactor(0.11)
                .setOptimizationAfterRounding(true);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/input-parameters-test", parameters, true);

        assertEquals("WARNING", result.getIndicators().get("log_level_ampl"));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("log_level_knitro")));
        assertEquals(2, Integer.parseInt(result.getIndicators().get("objective_choice")));
        assertEquals(0.7, Double.parseDouble(result.getIndicators().get("ratio_voltage_target")));
        assertEquals(0.999, Double.parseDouble(result.getIndicators().get("coeff_alpha")));
        assertEquals(0.02, Double.parseDouble(result.getIndicators().get("Pnull")));
        assertEquals(1e-5, Double.parseDouble(result.getIndicators().get("Znull")));
        assertEquals(0.75, Double.parseDouble(result.getIndicators().get("epsilon_nominal_voltage")));
        assertEquals(0.655, Double.parseDouble(result.getIndicators().get("min_plausible_low_voltage_limit")));
        assertEquals(1.425, Double.parseDouble(result.getIndicators().get("max_plausible_high_voltage_limit")));
        assertEquals(0.85, Double.parseDouble(result.getIndicators().get("ignore_voltage_bounds")));
        assertEquals("NO_GENERATION", result.getIndicators().get("buses_with_reactive_slacks"));
        assertEquals(4001, Double.parseDouble(result.getIndicators().get("PQmax")));
        assertEquals(1001, Double.parseDouble(result.getIndicators().get("defaultPmax")));
        assertEquals(0.1, Double.parseDouble(result.getIndicators().get("defaultPmin")));
        assertEquals(0.2, Double.parseDouble(result.getIndicators().get("defaultQmaxPmaxRatio")));
        assertEquals(-200.2, Double.parseDouble(result.getIndicators().get("defaultQmin")));
        assertEquals(200.2, Double.parseDouble(result.getIndicators().get("defaultQmax")));
        assertEquals(2.0, Double.parseDouble(result.getIndicators().get("minimalQPrange")));
        assertEquals(1.001, Double.parseDouble(result.getIndicators().get("default_variable_scaling_factor")));
        assertEquals(0.999, Double.parseDouble(result.getIndicators().get("default_constraint_scaling_factor")));
        assertEquals(0.01, Double.parseDouble(result.getIndicators().get("reactive_slack_variable_scaling_factor")));
        assertEquals(0.002, Double.parseDouble(result.getIndicators().get("transformer_ratio_variable_scaling_factor")));
        assertEquals(0.11, Double.parseDouble(result.getIndicators().get("shunt_variable_scaling_factor")));
        assertTrue(Boolean.parseBoolean(result.getIndicators().get("optimization_after_rounding")));
    }

    @Test
    void testBusIndicators() throws IOException {
        Network network = HvdcNetworkFactory.createLccWithBiggerComponents();
        network.getBusBreakerView().getBus("b1").setV(400);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/bus-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(16, Integer.parseInt(result.getIndicators().get("nb_substations")));
        assertEquals(16, Integer.parseInt(result.getIndicators().get("nb_bus_in_data_file")));
        // verify buses outside main SC have been excluded
        assertEquals(12, Integer.parseInt(result.getIndicators().get("nb_bus_in_main_SC")));
        assertEquals(4, Integer.parseInt(result.getIndicators().get("nb_bus_out_of_main_SC")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_bus_with_voltage_value")));
        assertEquals("vl1_0", result.getIndicators().get("slack_bus"));
    }

    @Test
    void testEmptyMainSynchronousComponentIndicators() throws IOException {
        Network network = createLowNominalVoltageNetwork();
        OpenReacResult result = runOpenReac(network, "optimization/indicators/empty-main-sc-test", true);

        // every bus is below the default epsilon_nominal_voltage (1kV): nothing can be optimized,
        // and the AMPL process exits before the slack bus check and the ACOPF
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
        assertEquals("NOK", result.getIndicators().get("final_status"));
        assertEquals("UNDEFINED", result.getIndicators().get("slack_bus"));
    }

    @Test
    void testBranchesIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithSimpleRemoteControl();
        network.getLine("l12").getTerminal2().disconnect();
        network.getLine("l24").getTerminal1().disconnect();
        // due to disconnected lines, increase max P of generators in main CC
        network.getGenerator("g3").setMaxP(3);
        network.getGenerator("g4").setMaxP(3);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/branches-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(4, Integer.parseInt(result.getIndicators().get("nb_branch_in_data_file")));
        // verify opened branches are considered in optimization
        assertEquals(4, Integer.parseInt(result.getIndicators().get("nb_branch_in_main_SC")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_main_SC_side_1_opened")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_branch_in_main_SC_side_2_opened")));
        // verify opened branches can be considered as zero impedance branches
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_branch_with_nonsmall_impedance")));
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_branch_with_zero_or_small_impedance")));
    }

    @Test
    void testGeneratorIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithGeneratorRemoteControl();
        network.getGenerator("g1").setTargetQ(50);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1", "g3"));
        OpenReacResult result = runOpenReac(network, "optimization/indicators/generators-test", parameters, true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(3, Integer.parseInt(result.getIndicators().get("nb_unit_in_data_file")));
        assertEquals(3, Integer.parseInt(result.getIndicators().get("nb_unit_in_main_SC")));
        assertEquals(3, Integer.parseInt(result.getIndicators().get("nb_unit_up_and_running")));
        // verify that only the generators indicated as constant and with defined target Q are fixed in optimization
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_unit_with_fixed_reactive_power")));
        // verify other generators are optimized
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_unit_with_variable_reactive_power")));
    }

    @Test
    void testTransformersIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT1"));
        OpenReacResult result = runOpenReac(network, "optimization/indicators/transfo-test", parameters, true);

        // verify only one rtc has been optimized
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_variable_ratio")));
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_fixed_ratio")));
    }

    @Test
    void testVscIndicators() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();

        OpenReacResult result = runOpenReac(network, "optimization/indicators/vsc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_in_data_file")));
        // verify only vsc in main cc is optimized
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_up_and_running")));
    }

    @Test
    void testLccIndicators() throws IOException {
        Network network = HvdcNetworkFactory.createLcc();

        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_lcc_converter_in_data_file")));
        // verify only lcc in main cc is considered in optimization
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_lcc_converter_up_and_running")));
    }

    @Test
    void testShuntsIndicators() throws IOException {
        Network network = ShuntNetworkFactory.createWithLinearModel();
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
        OpenReacResult result = runOpenReac(network, "optimization/indicators/shunt-test", parameters, true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(3, Integer.parseInt(result.getIndicators().get("nb_shunt_in_data_file")));
        assertEquals(3, Integer.parseInt(result.getIndicators().get("nb_shunt_connectable_or_in_main_SC")));
        // verify disconnected shunt, and not optimized, is not considered in shunts with fixed values
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_shunt_with_fixed_value")));
        // verify only one shunt has been optimized (even if it was disconnected)
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_shunt_with_variable_value")));
    }

    @Test
    void testSvcIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        network.getVoltageLevel("vl2").newStaticVarCompensator()
                .setId("svc2")
                .setConnectableBus("b2")
                .setBus("b2")
                .setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
                .setVoltageSetpoint(400)
                .setRegulating(true)
                .setBmin(-0.008)
                .setBmax(0.008)
                .add();
        OpenReacResult result = runOpenReac(network, "optimization/indicators/svc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_svc_in_data_file")));
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_svc_in_main_SC")));
        // verify only one svc is regulating
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_svc_up_and_operating")));
    }

    @Test
    void testBatteryIndicators() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithGeneratorRemoteControl();
        network.getVoltageLevel("vl4")
                .newBattery()
                .setId("bat1")
                .setMinP(-10)
                .setMaxP(10)
                .setTargetP(2)
                .setBus("b4")
                .setConnectableBus("b4")
                .setTargetQ(0)
                .add();
        network.getVoltageLevel("vl1")
                .newBattery()
                .setId("bat2")
                .setMinP(-9)
                .setMaxP(11)
                .setTargetP(1)
                .setBus("b1")
                .setConnectableBus("b1")
                .setTargetQ(0.2)
                .add();
        OpenReacResult result = runOpenReac(network, "optimization/indicators/battery-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_batteries")));
        // verify the sum of max and min active power of the batteries
        assertEquals(21, Double.parseDouble(result.getIndicators().get("sum_batteries_pmax")));
        assertEquals(-19, Double.parseDouble(result.getIndicators().get("sum_batteries_pmin")));
    }

    /**
     * Network whose main synchronous component (the largest one, hence sc=0) only contains buses
     * with a nominal voltage below the default epsilon_nominal_voltage (1kV), so that it is empty
     * once the nominal voltage filter is applied. A smaller synchronous component at 400kV is
     * added so that the network passes the consistency check requiring more than one voltage
     * level above epsilon_nominal_voltage.
     */
    private static Network createLowNominalVoltageNetwork() {
        Network network = Network.create("low-nominal-voltage", "test");
        Substation s = network.newSubstation()
                .setId("S1")
                .add();
        // main synchronous component: three buses below epsilon_nominal_voltage
        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("vl1")
                .setNominalV(0.4)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        vl1.newGenerator()
                .setId("g1")
                .setBus("b1")
                .setConnectableBus("b1")
                .setTargetP(1)
                .setTargetV(0.4)
                .setMinP(0)
                .setMaxP(2)
                .setVoltageRegulatorOn(true)
                .add();
        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("vl2")
                .setNominalV(0.4)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        vl2.newLoad()
                .setId("ld2")
                .setBus("b2")
                .setConnectableBus("b2")
                .setP0(1)
                .setQ0(0.2)
                .add();
        VoltageLevel vl3 = s.newVoltageLevel()
                .setId("vl3")
                .setNominalV(0.4)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl3.getBusBreakerView().newBus()
                .setId("b3")
                .add();
        network.newLine()
                .setId("l12")
                .setBus1("b1")
                .setConnectableBus1("b1")
                .setBus2("b2")
                .setConnectableBus2("b2")
                .setR(0.01)
                .setX(0.03)
                .add();
        network.newLine()
                .setId("l23")
                .setBus1("b2")
                .setConnectableBus1("b2")
                .setBus2("b3")
                .setConnectableBus2("b3")
                .setR(0.01)
                .setX(0.03)
                .add();
        // smaller synchronous component above epsilon_nominal_voltage
        VoltageLevel vl4 = s.newVoltageLevel()
                .setId("vl4")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl4.getBusBreakerView().newBus()
                .setId("b4")
                .add();
        vl4.newGenerator()
                .setId("g4")
                .setBus("b4")
                .setConnectableBus("b4")
                .setTargetP(10)
                .setTargetV(400)
                .setMinP(0)
                .setMaxP(20)
                .setVoltageRegulatorOn(true)
                .add();
        VoltageLevel vl5 = s.newVoltageLevel()
                .setId("vl5")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl5.getBusBreakerView().newBus()
                .setId("b5")
                .add();
        vl5.newLoad()
                .setId("ld5")
                .setBus("b5")
                .setConnectableBus("b5")
                .setP0(10)
                .setQ0(2)
                .add();
        network.newLine()
                .setId("l45")
                .setBus1("b4")
                .setConnectableBus1("b4")
                .setBus2("b5")
                .setConnectableBus2("b5")
                .setR(1)
                .setX(3)
                .add();
        return network;
    }

}
