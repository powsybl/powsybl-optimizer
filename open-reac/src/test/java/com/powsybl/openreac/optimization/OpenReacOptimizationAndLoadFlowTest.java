/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.optimization;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Nicolas PIERRE {@literal <nicolas.pierre at artelys.com>}
 */
class OpenReacOptimizationAndLoadFlowTest extends AbstractOpenReacRunnerTest {

    @Test
    void testRunAsync() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        String subFolder = "optimization/loadflow/openreac-output-ieee14";
        OpenReacParameters parameters = new OpenReacParameters();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of(subFolder + "/reactiveopf_results_generators.csv",
                        subFolder + "/reactiveopf_results_indic.txt",
                        subFolder + "/reactiveopf_results_rtc.csv",
                        subFolder + "/reactiveopf_results_shunts.csv",
                        subFolder + "/reactiveopf_results_static_var_compensators.csv",
                        subFolder + "/reactiveopf_results_vsc_converter_stations.csv",
                        subFolder + "/reactiveopf_results_voltages.csv"));
        // To really run open reac, use the commented line below. Be sure that open-reac/src/test/resources/com/powsybl/config/test/config.yml contains your ampl path
//         try (ComputationManager computationManager = new LocalComputationManager()) {
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            CompletableFuture<OpenReacResult> openReacResults = OpenReacRunner.runAsync(network,
                    network.getVariantManager().getWorkingVariantId(), parameters, new OpenReacConfig(true),
                    computationManager);
            OpenReacResult openReacResult = openReacResults.join();
            assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
        }
    }

    @Test
    void testOnlyGenerator() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-ieee14", new OpenReacParameters(), ReportNode.NO_OP);
    }

    @Test
    void testHvdc() throws IOException {
        Network network = HvdcNetworkFactory.createNetworkWithGenerators2();
        network.getVscConverterStation("cs3").getTerminal().setP(0.0);
        network.getVscConverterStation("cs4").getTerminal().setP(0.0);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1", "g2", "g5", "g6"));
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-vsc", parameters, ReportNode.NO_OP);
    }

    @Test
    void testSvc() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        network.getVoltageLevelStream().forEach(vl -> vl.setLowVoltageLimit(380).setHighVoltageLimit(420));
        network.getStaticVarCompensator("svc1").setVoltageSetpoint(390).setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE);
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1"));
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-svc", parameters, ReportNode.NO_OP);
    }

    @Test
    void testShunt() throws IOException {
        Network network = create();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        assertFalse(shunt.getTerminal().isConnected());
        assertEquals(393, shunt.getTargetV());

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableShuntCompensators(List.of(shunt.getId()));
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-shunt", parameters, reportNode);

        assertEquals(3, reportNode.getChildren().size());
        ReportNode reportShunts = reportNode.getChildren().get(2);
        assertEquals(2, reportShunts.getChildren().size());
        assertEquals("shuntCompensatorDeltaOverThresholdCount", reportShunts.getChildren().get(0).getMessageKey());
        Map<String, TypedValue> values = reportShunts.getChildren().get(0).getValues();
        assertEquals("1", values.get("shuntsCount").toString());
        assertEquals(TypedValue.INFO_SEVERITY.getValue(), values.get("reportSeverity").toString());

        assertEquals("shuntCompensatorDeltaDiscretizedOptimizedOverThreshold", reportShunts.getChildren().get(1).getMessageKey());
        values = reportShunts.getChildren().get(1).getValues();
        assertEquals("SHUNT", values.get("shuntCompensatorId").toString());
        assertEquals("25", values.get("maxSectionCount").toString());
        assertEquals("160.0", values.get("discretizedValue").toString());
        assertEquals("123.4", values.get("optimalValue").toString());
        assertEquals(TypedValue.TRACE_SEVERITY.getValue(), values.get("reportSeverity").toString());

        assertTrue(shunt.getTerminal().isConnected()); // shunt has been reconnected
        assertEquals(420.8, shunt.getTargetV()); // targetV has been updated
    }

    @Test
    void testShuntWithDeltaBetweenDiscretizedAndOptimalReactiveValueUnderThreshold() throws IOException {
        Network network = create();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        assertFalse(shunt.getTerminal().isConnected());
        assertEquals(393, shunt.getTargetV());

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setShuntCompensatorActivationAlertThreshold(100.);
        parameters.addVariableShuntCompensators(List.of(shunt.getId()));
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-shunt", parameters, reportNode);

        assertEquals(2, reportNode.getChildren().size());

        assertTrue(shunt.getTerminal().isConnected()); // shunt has been reconnected
        assertEquals(420.8, shunt.getTargetV()); // targetV has been updated
    }

    @Test
    void testTransformer() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        RatioTapChanger rtc = network.getTwoWindingsTransformer("T2wT").getRatioTapChanger()
                .setTapPosition(2)
                .setTargetDeadband(0)
                .setRegulating(true);
        assertEquals(2, rtc.getTapPosition());
        assertEquals(33.0, rtc.getTargetV());

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("GEN_1"));
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT"));
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-transfo", parameters, ReportNode.NO_OP);
        assertEquals(0, rtc.getTapPosition());
        assertEquals(22.935, rtc.getTargetV());
    }

    @Test
    void testRealNetwork() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        OpenReacParameters parameters = new OpenReacParameters();
        testAllModifAndLoadFlow(network, "optimization/loadflow/openreac-output-real-network", parameters, ReportNode.NO_OP);
    }

    @Test
    void testWarmStart() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        String subFolder = "optimization/loadflow/openreac-output-warm-start";
        OpenReacParameters parameters = new OpenReacParameters();

        runAndApplyAllModifications(network, subFolder, parameters, false, ReportNode.NO_OP); // without warm start, no update
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_1").getV());
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_1").getAngle());
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_2").getV());
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_2").getAngle());
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_3").getV());
        assertEquals(Double.NaN, network.getBusBreakerView().getBus("BUS_3").getAngle());

        runAndApplyAllModifications(network, subFolder, parameters, true, ReportNode.NO_OP);
        assertEquals(119.592, network.getBusBreakerView().getBus("BUS_1").getV());
        assertEquals(0.802, network.getBusBreakerView().getBus("BUS_1").getAngle(), 0.001);
        assertEquals(118.8, network.getBusBreakerView().getBus("BUS_2").getV());
        assertEquals(0, network.getBusBreakerView().getBus("BUS_2").getAngle());
        assertEquals(22.935, network.getBusBreakerView().getBus("BUS_3").getV());
        assertEquals(-4.698, network.getBusBreakerView().getBus("BUS_3").getAngle(), 0.001);
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

}
