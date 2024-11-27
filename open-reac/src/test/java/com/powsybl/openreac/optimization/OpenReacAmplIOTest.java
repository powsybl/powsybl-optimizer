/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.optimization;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Nicolas PIERRE {@literal <nicolas.pierre at artelys.com>}
 */
public class OpenReacAmplIOTest extends AbstractOpenReacRunnerTest {

    @Test
    void testInputFile() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        OpenReacParameters parameters = new OpenReacParameters().setObjective(
                        OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT)
                .setObjectiveDistance(70)
                .setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED)
                .addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream()
                        .limit(1)
                        .map(TwoWindingsTransformer::getId)
                        .collect(Collectors.toList()))
                .addConstantQGenerators(
                        network.getGeneratorStream().limit(1).map(Generator::getId).collect(Collectors.toList()))
                .addVariableShuntCompensators(
                        network.getShuntCompensatorStream().limit(1).map(ShuntCompensator::getId).collect(Collectors.toList()))
                .addConfiguredReactiveSlackBuses(
                        network.getBusView().getBusStream().limit(1).map(Bus::getId).collect(Collectors.toList()));

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/expected_inputs/param_algo.txt");
            assertEqualsToRef(execFolder.resolve("param_generators_reactive.txt"), "/expected_inputs/param_generators_reactive.txt");
            assertEqualsToRef(execFolder.resolve("param_shunts.txt"), "/expected_inputs/param_shunts.txt");
            assertEqualsToRef(execFolder.resolve("param_transformers.txt"), "/expected_inputs/param_transformers.txt");
            assertEqualsToRef(execFolder.resolve("param_buses_with_reactive_slack.txt"), "/expected_inputs/param_buses_with_reactive_slack.txt");
        }
    }

    @Test
    void testOutputFileParsing() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        // To parse correctly data from output files, there must be an ID in the Ampl mapper
        // For this we add dummy elements to the network,
        // they will get exported, but the ampl mapper will have IDs for them.
        // All the values are bad, and they are not used.
        // RTC
        network.getTwoWindingsTransformerStream()
                .forEach(t -> t.newRatioTapChanger()
                        .setLowTapPosition(0)
                        .setTapPosition(0)
                        .beginStep()
                        .setR(0.01)
                        .setX(0.0001)
                        .setB(0)
                        .setG(0)
                        .setRho(1.1)
                        .endStep()
                        .add());
        // SVC
        VoltageLevel vl = network.getVoltageLevelStream().iterator().next();
        vl.getBusBreakerView().newBus().setId("bus-1").add();
        vl.newStaticVarCompensator()
                .setId("dummyStaticVarCompensator")
                .setBus("bus-1")
                .setBmin(1.1)
                .setBmax(1.3)
                .setRegulationMode(StaticVarCompensator.RegulationMode.OFF)
                .add();
        // VSC
        vl.newVscConverterStation()
                .setId("dummyVscConverterStation")
                .setConnectableBus("bus-1")
                .setBus("bus-1")
                .setLossFactor(1.1f)
                .setVoltageSetpoint(405.0)
                .setVoltageRegulatorOn(true)
                .add();

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("mock_outputs/reactiveopf_results_generators.csv",
                        "mock_outputs/reactiveopf_results_indic.txt",
                        "mock_outputs/reactiveopf_results_rtc.csv",
                        "mock_outputs/reactiveopf_results_shunts.csv",
                        "mock_outputs/reactiveopf_results_static_var_compensators.csv",
                        "mock_outputs/reactiveopf_results_vsc_converter_stations.csv",
                        "mock_outputs/reactiveopf_results_voltages.csv"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult openReacResult = OpenReacRunner.run(network,
                    network.getVariantManager().getWorkingVariantId(), new OpenReacParameters(), new OpenReacConfig(true),
                    computationManager);

            assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
            assertEquals(1, openReacResult.getShuntsModifications().size());
            assertEquals(2, openReacResult.getTapPositionModifications().size());
            assertEquals(1, openReacResult.getSvcModifications().size());
            assertEquals(1, openReacResult.getVscModifications().size());
            assertEquals(7, openReacResult.getGeneratorModifications().size());
            assertEquals(3, openReacResult.getVoltageProfile().size());
            assertEquals(87, openReacResult.getIndicators().size());
            assertTrue(openReacResult.getReactiveSlacks().isEmpty());
        }
    }

}
