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
    void testDefaultParamAlgoExport() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/openreac-input-algo-parameters/default.txt");
        }
    }

    @Test
    void testConsistentVoltageLimitsParamAlgoExport() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters()
                .setMinPlausibleLowVoltageLimit(0.7888)
                .setMaxPlausibleHighVoltageLimit(1.3455);

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/openreac-input-algo-parameters/minmax_plausible_voltage_limits.txt");
        }

    }

    @Test
    void testInputFile() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
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
            assertEquals(2, openReacResult.getTapPositionModifications().size());
            assertEquals(1, openReacResult.getSvcModifications().size());
            assertEquals(1, openReacResult.getVscModifications().size());
            assertEquals(54, openReacResult.getGeneratorModifications().size());
            assertEquals(78, openReacResult.getIndicators().size());
            assertTrue(openReacResult.getReactiveSlacks().isEmpty());
        }
    }

    private void testAllModifAndLoadFlow(Network network, String subFolder, OpenReacParameters parameters) throws IOException {
        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
            List.of(subFolder + "/reactiveopf_results_generators.csv",
                subFolder + "/reactiveopf_results_indic.txt",
                subFolder + "/reactiveopf_results_rtc.csv",
                subFolder + "/reactiveopf_results_shunts.csv",
                subFolder + "/reactiveopf_results_static_var_compensators.csv",
                subFolder + "/reactiveopf_results_vsc_converter_stations.csv"));
        // To really run open reac, use the commentede line below. Be sure that open-reac/src/test/resources/com/powsybl/config/test/config.yml contains your ampl path
        // try (ComputationManager computationManager = new LocalComputationManager()) {
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
            localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult openReacResult = OpenReacRunner.run(network,
                network.getVariantManager().getWorkingVariantId(), parameters, new OpenReacConfig(true),
                computationManager);
            assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
            openReacResult.applyAllModifications(network);
        }
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isOk());
    }

    @Test
    public void testOnlyGenerator() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        testAllModifAndLoadFlow(network, "openreac-output-ieee14", new OpenReacParameters());
    }

    @Test
    public void testHvdc() throws IOException {
        Network network = HvdcNetworkFactory.createNetworkWithGenerators2();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        network.getVscConverterStation("cs3").getTerminal().setP(0.0);
        network.getVscConverterStation("cs4").getTerminal().setP(0.0);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1", "g2", "g5", "g6"));
        testAllModifAndLoadFlow(network, "openreac-output-vsc", parameters);
    }

    @Test
    public void testSvc() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        network.getVoltageLevelStream().forEach(vl -> vl.setLowVoltageLimit(380).setHighVoltageLimit(420));
        network.getStaticVarCompensator("svc1").setVoltageSetpoint(390).setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1"));
        testAllModifAndLoadFlow(network, "openreac-output-svc", parameters);
    }

    @Test
    public void testShuntReconnection() throws IOException {
        Network network = create();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableShuntCompensators(List.of("SHUNT"));
        testAllModifAndLoadFlow(network, "openreac-output-shunt", parameters);
    }

    @Test
    public void testTransformer() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        network.getTwoWindingsTransformer("T2wT").getRatioTapChanger().setTapPosition(2);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("GEN_1"));
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT"));
        testAllModifAndLoadFlow(network, "openreac-output-transfo", parameters);
    }

    @Test
    public void testRealNetwork() throws IOException {
        // Network {CC0 SC0}: 53 generators have an inconsistent target voltage and have been discarded from voltage control
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();
        testAllModifAndLoadFlow(network, "openreac-output-real-network", parameters);
    }

    public static Network create() {
        Network network = Network.create("svc", "test");
        Substation s1 = network.newSubstation()
                .setId("S1")
                .add();
        Substation s2 = network.newSubstation()
                .setId("S2")
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("vl1")
                .setNominalV(400)
                .setHighVoltageLimit(420)
                .setLowVoltageLimit(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        vl1.newGenerator()
                .setId("g1")
                .setConnectableBus("b1")
                .setBus("b1")
                .setTargetP(101.3664)
                .setTargetV(390)
                .setMinP(0)
                .setMaxP(150)
                .setVoltageRegulatorOn(true)
                .add();
        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("vl2")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        vl2.newLoad()
                .setId("ld1")
                .setConnectableBus("b2")
                .setBus("b2")
                .setP0(101)
                .setQ0(150)
                .add();
        VoltageLevel vl3 = s2.newVoltageLevel()
                .setId("vl3")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl3.getBusBreakerView().newBus()
                .setId("b3")
                .add();
        vl3.newShuntCompensator()
                .setId("SHUNT")
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
        network.newLine()
                .setId("l1")
                .setBus1("b1")
                .setBus2("b2")
                .setR(1)
                .setX(3)
                .add();
        network.newLine()
                .setId("l2")
                .setBus1("b3")
                .setBus2("b2")
                .setR(1)
                .setX(3)
                .add();
        return network;
    }

    void setDefaultVoltageLimits(Network network) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(0.8 * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(1.2 * vl.getNominalV());
            }
        }
    }
}
