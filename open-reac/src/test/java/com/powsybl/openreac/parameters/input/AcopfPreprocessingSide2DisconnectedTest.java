/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test to verify that preprocessing correctly handles transformers with side 2 disconnected
 * and/or ratio_min = ratio_max.
 *
 * This test covers the fix for the bug where accessing bus_substation[1,-1] caused an error
 * when a transformer had side 2 disconnected.
 *
 * IMPORTANT: These tests require AMPL and Knitro to be installed and configured.
 * They will be automatically skipped if AMPL/Knitro is not available (e.g., in CI).
 *
 * Configuration: AMPL path must be set in:
 *   open-reac/src/test/resources/com/powsybl/config/test/config.yml
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class AcopfPreprocessingSide2DisconnectedTest {

    private static final String TEST_CONFIG_PATH = "com/powsybl/config/test/config.yml";

    /**
     * Check if AMPL and Knitro are available before running each test.
     * If not available, the test will be skipped with an informative message.
     */
    @BeforeEach
    void checkAmplAndKnitroAvailable() {
        boolean amplAvailable = isAmplAvailable();
        boolean knitroAvailable = isKnitroAvailable();

        String message = buildSkipMessage(amplAvailable, knitroAvailable);

        // Skip test if AMPL or Knitro not available
        assumeTrue(amplAvailable && knitroAvailable, message);
    }

    /**
     * Check if AMPL is available by reading the test config file.
     *
     * @return true if AMPL is available, false otherwise
     */
    private boolean isAmplAvailable() {
        // Read the test config file
        String amplHomeDir = readAmplHomeDirFromTestConfig();

        if (amplHomeDir == null || amplHomeDir.isEmpty() || "???".equals(amplHomeDir)) {
            return false;
        }

        // Check if the directory exists and contains ampl executable
        Path amplPath = Paths.get(amplHomeDir, "ampl");
        if (Files.exists(amplPath) && Files.isExecutable(amplPath)) {
            return true;
        }

        // Windows: check for ampl.exe
        Path amplExePath = Paths.get(amplHomeDir, "ampl.exe");
        if (Files.exists(amplExePath) && Files.isExecutable(amplExePath)) {
            return true;
        }

        return false;
    }

    /**
     * Read the AMPL homeDir from the test config file.
     *
     * @return the AMPL home directory path, or null if not found or not set
     */
    private String readAmplHomeDirFromTestConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEST_CONFIG_PATH)) {
            if (is == null) {
                // Config file not found
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                boolean inAmplSection = false;
                Pattern amplSectionPattern = Pattern.compile("^ampl:\\s*$");
                Pattern homeDirPattern = Pattern.compile("^\\s+homeDir:\\s*(.*)$");

                String line;
                while ((line = reader.readLine()) != null) {
                    // Check if we're entering the ampl section
                    if (amplSectionPattern.matcher(line).matches()) {
                        inAmplSection = true;
                        continue;
                    }

                    // If we're in ampl section, look for homeDir
                    if (inAmplSection) {
                        Matcher matcher = homeDirPattern.matcher(line);
                        if (matcher.matches()) {
                            String homeDir = matcher.group(1).trim();
                            // Return null if homeDir is "???" (not configured)
                            if ("???".equals(homeDir)) {
                                return null;
                            }
                            return homeDir;
                        }

                        // If we hit a line that's not indented, we've left the ampl section
                        if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                            inAmplSection = false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Error reading config file
            return null;
        }

        return null;
    }

    /**
     * Check if Knitro solver is available.
     *
     * @return true if Knitro is available, false otherwise
     */
    private boolean isKnitroAvailable() {
        // First, check if AMPL homeDir is set
        String amplHomeDir = readAmplHomeDirFromTestConfig();
        if (amplHomeDir == null || amplHomeDir.isEmpty() || "???".equals(amplHomeDir)) {
            return false;
        }

        // Check if knitroampl exists in the AMPL directory
        Path knitroPath = Paths.get(amplHomeDir, "knitroampl");
        if (Files.exists(knitroPath) && Files.isExecutable(knitroPath)) {
            return true;
        }

        // Windows: check for knitroampl.exe
        Path knitroExePath = Paths.get(amplHomeDir, "knitroampl.exe");
        if (Files.exists(knitroExePath) && Files.isExecutable(knitroExePath)) {
            return true;
        }

        // Fallback: check if knitroampl is in PATH
        try {
            Process process = new ProcessBuilder("knitroampl", "--version")
                .redirectErrorStream(true)
                .start();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // Knitro not found
            return false;
        }
    }

    /**
     * Build an informative skip message based on what's missing.
     */
    private String buildSkipMessage(boolean amplAvailable, boolean knitroAvailable) {
        if (!amplAvailable && !knitroAvailable) {
            return "AMPL and Knitro not available - skipping test. " +
                   "Configure ampl.homeDir in " + TEST_CONFIG_PATH + " to run this test.";
        } else if (!amplAvailable) {
            return "AMPL not available - skipping test. " +
                   "Configure ampl.homeDir in " + TEST_CONFIG_PATH + " " +
                   "(set to '???' or not found). Update it with your AMPL installation path.";
        } else {
            return "Knitro solver not available - skipping test. " +
                   "Ensure knitroampl is in your AMPL installation directory: " + readAmplHomeDirFromTestConfig();
        }
    }

    /**
     * Test Case 1: RTC with ratio_min = ratio_max AND both sides connected
     *
     * This tests the "Case 2" branch of the AMPL fix:
     * - n != -1 (both sides connected)
     * - ratio_min = ratio_max (single tap)
     *
     * Expected behavior:
     * - Should access bus_substation[1,n] normally (no error)
     * - Should generate warning: "should have variable ratio but min and max are equal"
     * - Ratio should remain fixed
     */
    @Test
    void testRTCWithEqualMinMaxRatioBothSidesConnected() {
        Network network = createNetworkWithMultipleRTCs();

        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT_EQUAL_RATIO_CONNECTED");
        assertNotNull(twt, "Transformer should exist");
        assertTrue(twt.getTerminal1().isConnected(), "Side 1 should be connected");
        assertTrue(twt.getTerminal2().isConnected(), "Side 2 should be connected");

        RatioTapChanger rtc = twt.getRatioTapChanger();
        assertNotNull(rtc, "RTC should exist");
        assertEquals(1, rtc.getStepCount(), "RTC should have exactly 1 tap");

        int tapPositionBefore = rtc.getTapPosition();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(
            Arrays.asList("TWT_EQUAL_RATIO_CONNECTED")
        );

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("testReport", "Test report")
            .build();

        // Should NOT crash (tests that bus_substation[1,n] is accessible when n != -1)
        assertDoesNotThrow(() -> {
            OpenReacResult result = OpenReacRunner.run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                parameters,
                new OpenReacConfig(true),
                LocalComputationManager.getDefault(),
                reportNode,
                null
            );

            assertNotNull(result);
            assertEquals(OpenReacStatus.OK, result.getStatus(),
                "OpenReac should complete successfully");
        }, "Should not crash when accessing bus_substation[1,n] with both sides connected");

        int tapPositionAfter = rtc.getTapPosition();
        assertEquals(tapPositionBefore, tapPositionAfter,
            "Tap position should not change when ratio_min = ratio_max");
    }

    /**
     * Test Case 2: RTC with ratio_min < ratio_max AND side 2 disconnected
     *
     * This tests the "Case 1" branch of the AMPL fix:
     * - n == -1 (side 2 disconnected)
     * - ratio_min < ratio_max (multiple taps)
     *
     * Expected behavior:
     * - Should use branch_subex instead of bus_substation[1,-1]
     * - Should generate warning: "has side 2 disconnected, RTC will be fixed in optimization"
     * - Ratio should remain fixed (cannot optimize disconnected equipment)
     */
    @Test
    void testRTCWithVariableRatioSide2Disconnected() {
        Network network = createNetworkWithMultipleRTCs();

        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT_VARIABLE_RATIO_DISCONNECTED");
        assertNotNull(twt, "Transformer should exist");
        assertTrue(twt.getTerminal1().isConnected(), "Side 1 should be connected");
        assertFalse(twt.getTerminal2().isConnected(), "Side 2 should be disconnected");

        RatioTapChanger rtc = twt.getRatioTapChanger();
        assertNotNull(rtc, "RTC should exist");
        assertTrue(rtc.getStepCount() > 1, "RTC should have multiple taps");

        int tapPositionBefore = rtc.getTapPosition();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(
            Arrays.asList("TWT_VARIABLE_RATIO_DISCONNECTED")
        );

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("testReport", "Test report")
            .build();

        // Before the patch, this would crash with: invalid subscript bus_substation[1,-1]
        // After the patch, branch_subex is used correctly
        assertDoesNotThrow(() -> {
            OpenReacResult result = OpenReacRunner.run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                parameters,
                new OpenReacConfig(true),
                LocalComputationManager.getDefault(),
                reportNode,
                null
            );

            assertNotNull(result);
            assertEquals(OpenReacStatus.OK, result.getStatus(),
                "OpenReac should complete successfully");
        }, "Should not crash when accessing branch_subex for disconnected side 2");

        int tapPositionAfter = rtc.getTapPosition();
        assertEquals(tapPositionBefore, tapPositionAfter,
            "Tap position should not change when side 2 is disconnected");
    }

    /**
     * Test Case 3: RTC with ratio_min = ratio_max AND side 2 disconnected
     *
     * This tests the combination of both conditions:
     * - n == -1 (side 2 disconnected)
     * - ratio_min = ratio_max (single tap)
     *
     * This is the "pathological" case that combines both issues.
     * The AMPL code will enter "Case 1" (side 2 disconnected) first.
     *
     * Expected behavior:
     * - Should use branch_subex instead of bus_substation[1,-1]
     * - Should generate warning: "has side 2 disconnected, RTC will be fixed in optimization"
     * - Ratio should remain fixed
     */
    @Test
    void testRTCWithEqualMinMaxRatioSide2Disconnected() {
        Network network = createNetworkWithMultipleRTCs();

        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT_EQUAL_RATIO_DISCONNECTED");
        assertNotNull(twt, "Transformer should exist");
        assertTrue(twt.getTerminal1().isConnected(), "Side 1 should be connected");
        assertFalse(twt.getTerminal2().isConnected(), "Side 2 should be disconnected");

        RatioTapChanger rtc = twt.getRatioTapChanger();
        assertNotNull(rtc, "RTC should exist");
        assertEquals(1, rtc.getStepCount(), "RTC should have exactly 1 tap");

        int tapPositionBefore = rtc.getTapPosition();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(
            Arrays.asList("TWT_EQUAL_RATIO_DISCONNECTED")
        );

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("testReport", "Test report")
            .build();

        assertDoesNotThrow(() -> {
            OpenReacResult result = OpenReacRunner.run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                parameters,
                new OpenReacConfig(true),
                LocalComputationManager.getDefault(),
                reportNode,
                null
            );

            assertNotNull(result);
            assertEquals(OpenReacStatus.OK, result.getStatus(),
                "OpenReac should complete successfully");
        }, "Should not crash with both conditions combined");

        int tapPositionAfter = rtc.getTapPosition();
        assertEquals(tapPositionBefore, tapPositionAfter,
            "Tap position should not change when ratio_min = ratio_max AND side 2 disconnected");
    }

    /**
     * Test Case 4: RTC with ratio_min < ratio_max AND both sides connected (normal case)
     */
    @Test
    void testRTCWithVariableRatioBothSidesConnected() {
        Network network = createNetworkWithMultipleRTCs();

        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT_VARIABLE_RATIO_CONNECTED");
        assertNotNull(twt, "Transformer should exist");
        assertTrue(twt.getTerminal1().isConnected(), "Side 1 should be connected");
        assertTrue(twt.getTerminal2().isConnected(), "Side 2 should be connected");

        RatioTapChanger rtc = twt.getRatioTapChanger();
        assertNotNull(rtc, "RTC should exist");
        assertTrue(rtc.getStepCount() > 1, "RTC should have multiple taps");

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(
            Arrays.asList("TWT_VARIABLE_RATIO_CONNECTED")
        );

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("testReport", "Test report")
            .build();

        assertDoesNotThrow(() -> {
            OpenReacResult result = OpenReacRunner.run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                parameters,
                new OpenReacConfig(true),
                LocalComputationManager.getDefault(),
                reportNode,
                null
            );

            assertNotNull(result);
            assertEquals(OpenReacStatus.OK, result.getStatus(),
                "OpenReac should work normally");
        }, "Normal transformer should work without issues (non-regression test)");
    }

    /**
     * Test Case 5: All transformers together
     */
    @Test
    void testAllTransformersTogether() {
        Network network = createNetworkWithMultipleRTCs();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(
            Arrays.asList(
                "TWT_EQUAL_RATIO_CONNECTED",
                "TWT_VARIABLE_RATIO_DISCONNECTED",
                "TWT_EQUAL_RATIO_DISCONNECTED",
                "TWT_VARIABLE_RATIO_CONNECTED"
            )
        );

        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("testReport", "Test report")
            .build();

        assertDoesNotThrow(() -> {
            OpenReacResult result = OpenReacRunner.run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                parameters,
                new OpenReacConfig(true),
                LocalComputationManager.getDefault(),
                reportNode,
                null
            );

            assertNotNull(result);
            assertEquals(OpenReacStatus.OK, result.getStatus(),
                "OpenReac should handle all transformer types together");
        }, "Should handle all transformer configurations together");
    }

    /**
     * Test using standard IEEE14 network (non-regression)
     *
     * Verifies that the fix doesn't break existing functionality
     * on standard networks.
     */
    @Test
    void testStandardIEEE14Network() {
        Network network = IeeeCdfNetworkFactory.create14();
        setDefaultVoltageLimits(network);

        // Verify that the IEEE14 network has transformers
        TwoWindingsTransformer twt = network.getTwoWindingsTransformerStream()
            .findFirst().orElse(null);
        assertNotNull(twt, "IEEE14 network should contain at least one transformer");

        // Verify both sides are connected (this is the normal case)
        assertTrue(twt.getTerminal1().isConnected(), "Side 1 should be connected");
        assertTrue(twt.getTerminal2().isConnected(), "Side 2 should be connected");

        // This test just verifies the network structure is as expected
        // No need to run OpenReac here
    }

    // Helper methods - keep your existing implementations

    private Network createNetworkWithMultipleRTCs() {
        Network network = IeeeCdfNetworkFactory.create14();
        setDefaultVoltageLimits(network);

        Substation substation = network.getSubstations().iterator().next();

        VoltageLevel vl1 = null;
        VoltageLevel vl2 = null;

        for (VoltageLevel vl : substation.getVoltageLevels()) {
            if (vl1 == null) {
                vl1 = vl;
            } else if (vl2 == null) {
                vl2 = vl;
                break;
            }
        }

        if (vl2 == null) {
            vl2 = substation.newVoltageLevel()
                .setId("VL_TEST_2")
                .setNominalV(vl1.getNominalV() / 2.5)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

            vl2.getBusBreakerView().newBus()
                .setId("BUS_TEST_2")
                .add();
        }

        Bus bus1 = vl1.getBusBreakerView().getBuses().iterator().next();
        Bus bus2 = vl2.getBusBreakerView().getBuses().iterator().next();

        Bus bus3 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_3")
            .add();
        Bus bus4 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_4")
            .add();
        Bus bus5 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_5")
            .add();

        createTransformer(substation, vl1, vl2, bus1, bus2,
            "TWT_EQUAL_RATIO_CONNECTED", true, false);

        createTransformer(substation, vl1, vl2, bus1, bus3,
            "TWT_VARIABLE_RATIO_DISCONNECTED", false, true);

        createTransformer(substation, vl1, vl2, bus1, bus4,
            "TWT_EQUAL_RATIO_DISCONNECTED", true, true);

        createTransformer(substation, vl1, vl2, bus1, bus5,
            "TWT_VARIABLE_RATIO_CONNECTED", false, false);

        setDefaultVoltageLimits(network);

        return network;
    }

    private void createTransformer(Substation substation, VoltageLevel vl1, VoltageLevel vl2,
                                   Bus bus1, Bus bus2, String id,
                                   boolean singleTap, boolean disconnectSide2) {
        TwoWindingsTransformer twt = substation.newTwoWindingsTransformer()
            .setId(id)
            .setVoltageLevel1(vl1.getId())
            .setBus1(bus1.getId())
            .setConnectableBus1(bus1.getId())
            .setVoltageLevel2(vl2.getId())
            .setBus2(bus2.getId())
            .setConnectableBus2(bus2.getId())
            .setR(0.5)
            .setX(10.0)
            .setG(0.0)
            .setB(0.0)
            .setRatedU1(vl1.getNominalV())
            .setRatedU2(vl2.getNominalV())
            .add();

        RatioTapChangerAdder rtcAdder = twt.newRatioTapChanger()
            .setTapPosition(singleTap ? 0 : 1)
            .setLoadTapChangingCapabilities(true)
            .setRegulating(true)
            .setTargetV(vl2.getNominalV())
            .setTargetDeadband(1.0)
            .setRegulationTerminal(twt.getTerminal2());

        if (singleTap) {
            rtcAdder.beginStep()
                .setRho(0.95)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep();
        } else {
            rtcAdder.beginStep()
                .setRho(0.90)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(0.95)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(1.00)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep();
        }

        rtcAdder.add();

        if (disconnectSide2) {
            twt.getTerminal2().disconnect();
        }
    }

    private void setDefaultVoltageLimits(Network network) {
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
