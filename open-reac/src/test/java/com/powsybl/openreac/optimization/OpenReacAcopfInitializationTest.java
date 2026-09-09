/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.ReferenceState;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the ACOPF initialization performed by {@link OpenReacRunner} before the AMPL export: the AMPL
 * model receives the requested starting point, while the network handed by the caller is left untouched.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class OpenReacAcopfInitializationTest extends AbstractOpenReacRunnerTest {

    // columns of ampl_network_buses.txt (extended AMPL export version)
    private static final int V_COLUMN = 5;
    private static final int THETA_COLUMN = 6;
    // columns of ampl_network_rtc.txt
    private static final int RTC_TAP_COLUMN = 2;
    private static final int SLACK_COLUMN = 9;

    @Test
    void testAmplReceivesInitializedAnglesAndSlackBus() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        SlackTerminal.reset(network);
        setDefaultVoltageLimits(network);
        String variantId = network.getVariantManager().getWorkingVariantId();
        List<String> variantIdsBefore = List.copyOf(network.getVariantManager().getVariantIds());
        Map<String, Double> anglesBefore = network.getBusView().getBusStream().collect(Collectors.toMap(Bus::getId, Bus::getAngle));

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(List.of("optimization/indicators/bus-test/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult result = OpenReacRunner.run(network, variantId, new OpenReacParameters(), new OpenReacConfig(true), computationManager);
            assertEquals(OpenReacStatus.OK, result.getStatus());

            List<String[]> buses = readTable(getAmplExecPath().resolve("ampl_network_buses.txt"));
            assertEquals(14, buses.size());
            // the angle reference is given as a parameter, not through the network
            assertTrue(buses.stream().noneMatch(b -> "true".equals(b[SLACK_COLUMN])));
            String slackBusId = readSlackBusId(getAmplExecPath().resolve("param_algo.txt"));
            assertEquals(0, Double.parseDouble(getBus(buses, slackBusId)[THETA_COLUMN]), 1e-6);
            assertTrue(buses.stream().anyMatch(b -> Math.abs(Double.parseDouble(b[THETA_COLUMN])) > 0.01),
                    "AMPL must receive the angles of the DC load flow, not a flat start");
        }

        // the network handed by the caller is left as it was
        assertEquals(variantIdsBefore, List.copyOf(network.getVariantManager().getVariantIds()));
        assertEquals(variantId, network.getVariantManager().getWorkingVariantId());
        network.getBusView().getBuses().forEach(b -> assertEquals(anglesBefore.get(b.getId()), b.getAngle(), 1e-6));
        assertTrue(network.getVoltageLevelStream().allMatch(vl -> vl.getExtension(SlackTerminal.class) == null),
                "no slack terminal is written");
    }

    @Test
    void testSlackTerminalOfTheNetworkIsNotTheAngleReference() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        SlackTerminal.reset(network);
        setDefaultVoltageLimits(network);
        Bus leafBus = network.getBusBreakerView().getBus("B14");
        SlackTerminal.attach(leafBus);
        String slackVoltageLevelId = leafBus.getVoltageLevel().getId();
        String variantId = network.getVariantManager().getWorkingVariantId();

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(List.of("optimization/indicators/bus-test/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, variantId, new OpenReacParameters(), new OpenReacConfig(true), computationManager);

            // the slack terminal of the network is exported, the angle reference given to AMPL is another bus
            List<String[]> buses = readTable(getAmplExecPath().resolve("ampl_network_buses.txt"));
            String leafBusId = leafBus.getConnectedTerminalStream().findFirst().orElseThrow().getBusView().getBus().getId();
            assertEquals("true", getBus(buses, leafBusId)[SLACK_COLUMN]);
            String slackBusId = readSlackBusId(getAmplExecPath().resolve("param_algo.txt"));
            assertNotEquals(leafBusId, slackBusId);
            assertEquals(0, Double.parseDouble(getBus(buses, slackBusId)[THETA_COLUMN]), 1e-6);
            assertNotEquals(0, Double.parseDouble(getBus(buses, leafBusId)[THETA_COLUMN]), 1e-3);
        }

        // the caller's slack terminal is still there, on the same bus, and is the only one
        SlackTerminal slackTerminal = network.getVoltageLevel(slackVoltageLevelId).getExtension(SlackTerminal.class);
        assertNotNull(slackTerminal);
        assertNotNull(slackTerminal.getTerminal());
        assertEquals(leafBus.getId(), slackTerminal.getTerminal().getBusBreakerView().getBus().getId());
        assertEquals(1, network.getVoltageLevelStream().filter(vl -> vl.getExtension(SlackTerminal.class) != null).count());
    }

    @Test
    void testAmplReceivesTheNeutralReferenceState() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        setDefaultVoltageLimits(network);
        String variantId = network.getVariantManager().getWorkingVariantId();
        Map<String, Double> voltagesBefore = network.getBusView().getBusStream().collect(Collectors.toMap(Bus::getId, Bus::getV));
        RatioTapChanger ratioTapChanger = network.getTwoWindingsTransformer("T2wT1").getRatioTapChanger();
        int tapPositionBefore = ratioTapChanger.getTapPosition();
        int neutralPosition = ratioTapChanger.getNeutralPosition().orElseThrow();
        assertNotEquals(tapPositionBefore, neutralPosition);
        OpenReacParameters parameters = new OpenReacParameters()
                .setReferenceState(ReferenceState.NEUTRAL)
                .addVariableTwoWindingsTransformers(List.of("T2wT1"));

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(List.of("optimization/indicators/bus-test/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, variantId, parameters, new OpenReacConfig(true), computationManager);

            List<String[]> buses = readTable(getAmplExecPath().resolve("ampl_network_buses.txt"));
            assertEquals(network.getBusView().getBusStream().count(), buses.size());
            buses.forEach(b -> assertEquals(1, Double.parseDouble(b[V_COLUMN]), 1e-6));
            // the AMPL export numbers taps from 1, the transformer id is the last column
            Map<String, Integer> exportedTaps = readTable(getAmplExecPath().resolve("ampl_network_rtc.txt")).stream()
                    .collect(Collectors.toMap(r -> r[r.length - 1].replace("\"", ""), r -> Integer.parseInt(r[RTC_TAP_COLUMN])));
            assertEquals(neutralPosition - ratioTapChanger.getLowTapPosition() + 1, exportedTaps.get("T2wT1"));
            assertEquals(tapPositionBefore - ratioTapChanger.getLowTapPosition() + 1, exportedTaps.get("T2wT2"));
        }

        network.getBusView().getBuses().forEach(b -> assertEquals(voltagesBefore.get(b.getId()), b.getV(), 1e-6));
        assertEquals(tapPositionBefore, ratioTapChanger.getTapPosition());
    }

    private static String[] getBus(List<String[]> buses, String id) {
        return buses.stream().filter(b -> ("\"" + id + "\"").equals(b[b.length - 1])).findFirst().orElseThrow();
    }

    private static String readSlackBusId(Path paramAlgo) throws IOException {
        return Files.readAllLines(paramAlgo).stream()
                .filter(line -> line.startsWith("slack_bus_id "))
                .map(line -> line.substring("slack_bus_id ".length()).replace("\"", ""))
                .findFirst().orElseThrow();
    }

    private static List<String[]> readTable(Path path) throws IOException {
        return Files.readAllLines(path).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.trim().split("\\s+"))
                .toList();
    }
}
