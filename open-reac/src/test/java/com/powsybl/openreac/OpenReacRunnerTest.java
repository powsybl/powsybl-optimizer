/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.network.ShuntNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
 */
class OpenReacRunnerTest {
    protected FileSystem fileSystem;
    protected Path tmpDir;

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = Files.createDirectory(fileSystem.getPath("tmp"));
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    private void assertEqualsToRef(Path p, String refFileName) throws IOException {
        try (InputStream actual = Files.newInputStream(p)) {
            ComparisonUtils.compareTxt(getClass().getResourceAsStream(refFileName), actual);
        }
    }

    private Path getAmplExecPath() throws IOException {
        Path execFolder;
        try (Stream<Path> walk = Files.walk(tmpDir)) {
            execFolder = walk.limit(2).collect(Collectors.toList()).get(1);
        }
        return execFolder;
    }

    @Test
    void testInputFile() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();
        OpenReacParameters parameters = new OpenReacParameters().setObjective(
                OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT)
            .setObjectiveDistance(70)
            .addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream()
                .limit(1)
                .map(TwoWindingsTransformer::getId)
                .collect(Collectors.toList()))
            .addConstantQGenerators(
                network.getGeneratorStream().limit(1).map(Generator::getId).collect(Collectors.toList()))
            .addVariableShuntCompensators(
                network.getShuntCompensatorStream().limit(1).map(ShuntCompensator::getId).collect(Collectors.toList()));

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
            List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
            localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/expected_inputs/param_algo.txt");
            assertEqualsToRef(execFolder.resolve("param_generators_reactive.txt"),
                "/expected_inputs/param_generators_reactive.txt");
            assertEqualsToRef(execFolder.resolve("param_shunts.txt"), "/expected_inputs/param_shunts.txt");
            assertEqualsToRef(execFolder.resolve("param_transformers.txt"), "/expected_inputs/param_transformers.txt");
        }
    }

    @Test
    public void testOutputFileParsing() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();
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
                "mock_outputs/reactiveopf_results_indic.txt", "mock_outputs/reactiveopf_results_rtc.csv",
                "mock_outputs/reactiveopf_results_shunts.csv",
                "mock_outputs/reactiveopf_results_static_var_compensators.csv",
                "mock_outputs/reactiveopf_results_vsc_converter_stations.csv"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
            localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult openReacResult = OpenReacRunner.run(network,
                network.getVariantManager().getWorkingVariantId(), new OpenReacParameters(), new OpenReacConfig(true),
                computationManager);

            assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
            assertEquals(1, openReacResult.getShuntsModifications().size());
            assertEquals(2, openReacResult.getTapModifications().size());
            assertEquals(1, openReacResult.getSvcModifications().size());
            assertEquals(1, openReacResult.getVscModifications().size());
            assertEquals(54, openReacResult.getGeneratorModifications().size());
            assertEquals(78, openReacResult.getIndicators().size());
            assertTrue(openReacResult.getReactiveSlacks().isEmpty());
        }
    }

    private void testAllModifAndLoadFlow(Network network, String subFolder) throws IOException {
        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
            List.of(subFolder + "/reactiveopf_results_generators.csv",
                subFolder + "/reactiveopf_results_indic.txt",
                subFolder + "/reactiveopf_results_rtc.csv",
                subFolder + "/reactiveopf_results_shunts.csv",
                subFolder + "/reactiveopf_results_static_var_compensators.csv",
                subFolder + "/reactiveopf_results_vsc_converter_stations.csv"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
            localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult openReacResult = OpenReacRunner.run(network,
                network.getVariantManager().getWorkingVariantId(), new OpenReacParameters(), new OpenReacConfig(true),
                computationManager);
            openReacResult.applyAllModifications(network);
        }
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isOk());
    }

    @Test
    public void testOnlyGenerator() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        testAllModifAndLoadFlow(network, "openreac-output-ieee14");
    }

    @Test
    public void testHvdc() throws IOException {
        Network network = HvdcNetworkFactory.createNetworkWithGenerators();
        testAllModifAndLoadFlow(network, "openreac-output-hvdc");
    }

    @Test
    public void testSvc() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        testAllModifAndLoadFlow(network, "openreac-output-svc");
    }

    @Test
    public void testVsc() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();
        testAllModifAndLoadFlow(network, "openreac-output-vsc");
    }

    @Test
    public void testShuntReconnection() throws IOException {
        Network network = ShuntNetworkFactory.create();
        testAllModifAndLoadFlow(network, "openreac-output-shunt");
    }

}
