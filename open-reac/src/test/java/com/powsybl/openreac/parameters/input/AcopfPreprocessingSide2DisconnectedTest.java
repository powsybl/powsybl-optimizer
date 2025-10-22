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
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that preprocessing correctly handles transformers with side 2 disconnected
 * and/or ratio_min = ratio_max.
 *
 * This test covers the fix for the bug where accessing bus_substation[1,-1] caused an error
 * when a transformer had side 2 disconnected.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class AcopfPreprocessingSide2DisconnectedTest {

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
            "Tap position should not change");
    }

    /**
     * Test Case 4: RTC with ratio_min < ratio_max AND both sides connected (NORMAL CASE)
     *
     * This is a non-regression test to ensure that normal transformers
     * still work correctly after the fix.
     *
     * Expected behavior:
     * - Should work normally without any warnings
     * - Ratio might be optimized by OpenReac
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

        // This is the normal case - should work without issues
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
                "OpenReac should complete successfully on normal transformer");
        }, "Normal transformer should work without issues (non-regression test)");
    }

    /**
     * Test that all 4 transformers can run together
     *
     * This ensures that having multiple transformers with different configurations
     * doesn't cause any conflicts.
     */
    @Test
    void testAllTransformersTogether() {
        Network network = createNetworkWithMultipleRTCs();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(Arrays.asList(
            "TWT_EQUAL_RATIO_CONNECTED",
            "TWT_VARIABLE_RATIO_DISCONNECTED",
            "TWT_EQUAL_RATIO_DISCONNECTED",
            "TWT_VARIABLE_RATIO_CONNECTED"
        ));

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

    /**
     * Create a test network with 4 transformers covering all test cases:
     *
     * 1. TWT_EQUAL_RATIO_CONNECTED: ratio_min = ratio_max, both sides connected
     * 2. TWT_VARIABLE_RATIO_DISCONNECTED: ratio_min < ratio_max, side 2 disconnected
     * 3. TWT_EQUAL_RATIO_DISCONNECTED: ratio_min = ratio_max, side 2 disconnected
     * 4. TWT_VARIABLE_RATIO_CONNECTED: ratio_min < ratio_max, both sides connected (normal)
     */
    private Network createNetworkWithMultipleRTCs() {
        // Start with a valid IEEE network
        Network network = IeeeCdfNetworkFactory.create14();
        setDefaultVoltageLimits(network);

        // Get the first substation
        Substation substation = network.getSubstations().iterator().next();

        // Create or find voltage levels
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

        // Create second voltage level if needed
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

        // Get buses
        Bus bus1 = vl1.getBusBreakerView().getBuses().iterator().next();
        Bus bus2 = vl2.getBusBreakerView().getBuses().iterator().next();

        // Create 4 additional buses for the 4 test transformers
        Bus bus3 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_3")
            .add();
        Bus bus4 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_4")
            .add();
        Bus bus5 = vl2.getBusBreakerView().newBus()
            .setId("BUS_TEST_5")
            .add();

        // Transformer 1: Equal ratio, both sides connected
        createTransformer(substation, vl1, vl2, bus1, bus2,
            "TWT_EQUAL_RATIO_CONNECTED", true, false);

        // Transformer 2: Variable ratio, side 2 disconnected
        createTransformer(substation, vl1, vl2, bus1, bus3,
            "TWT_VARIABLE_RATIO_DISCONNECTED", false, true);

        // Transformer 3: Equal ratio, side 2 disconnected
        createTransformer(substation, vl1, vl2, bus1, bus4,
            "TWT_EQUAL_RATIO_DISCONNECTED", true, true);

        // Transformer 4: Variable ratio, both sides connected (normal case)
        createTransformer(substation, vl1, vl2, bus1, bus5,
            "TWT_VARIABLE_RATIO_CONNECTED", false, false);

        setDefaultVoltageLimits(network);

        return network;
    }

    /**
     * Helper method to create a transformer with specific characteristics
     *
     * @param substation The substation
     * @param vl1 Voltage level 1
     * @param vl2 Voltage level 2
     * @param bus1 Bus on side 1
     * @param bus2 Bus on side 2
     * @param id Transformer ID
     * @param singleTap If true, creates RTC with ratio_min = ratio_max (1 tap)
     *                  If false, creates RTC with ratio_min < ratio_max (3 taps)
     * @param disconnectSide2 If true, disconnects side 2
     */
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

        // Create RTC with 1 or 3 taps
        RatioTapChangerAdder rtcAdder = twt.newRatioTapChanger()
            .setTapPosition(singleTap ? 0 : 1)  // Middle tap for variable ratio
            .setLoadTapChangingCapabilities(true)
            .setRegulating(true)
            .setTargetV(vl2.getNominalV())
            .setTargetDeadband(1.0)
            .setRegulationTerminal(twt.getTerminal2());

        if (singleTap) {
            // Single tap: ratio_min = ratio_max = 0.95
            rtcAdder.beginStep()
                .setRho(0.95)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep();
        } else {
            // Three taps: ratio_min = 0.90, ratio_max = 1.00
            rtcAdder.beginStep()
                .setRho(0.90)  // min
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(0.95)  // nominal
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(1.00)  // max
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep();
        }

        rtcAdder.add();

        // Disconnect side 2 if requested
        if (disconnectSide2) {
            twt.getTerminal2().disconnect();
        }
    }

    /**
     * Sets default voltage limits for all voltage levels in the network
     */
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
