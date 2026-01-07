/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import org.junit.jupiter.api.Test;

import static com.powsybl.openreac.parameters.input.ReportTestHelper.hasReportWithKey;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class BranchImpedanceValidationTest {

    /**
     * Create a ReportNode for testing
     */
    private ReportNode createReportNode() {
        return ReportNode.newRootReportNode()
                .withMessageTemplate("branchImpedanceValidationTest")
                .build();
    }

    /**
     * Test that French branches with r > 10*|x| throw an error
     */
    @Test
    void testFrenchBranchWithVeryHighImpedanceRatioThrowsError() {
        Network network = createNetworkWithFrenchLine(12.0, 1.0); // ratio = 12 > 10

        OpenReacParameters params = new OpenReacParameters();

        InvalidParametersException exception = assertThrows(InvalidParametersException.class,
                () -> params.checkIntegrity(network, ReportNode.NO_OP));

        assertTrue(exception.getMessage().contains("French branches have r > 10 * |x|"));
        assertTrue(exception.getMessage().contains("'LINE_FR'"));
    }

    /**
     * Test that French branches with 1 < r/|x| <= 10 generate a warning but don't throw
     */
    @Test
    void testFrenchBranchWithModerateImpedanceRatioGeneratesWarning() {
        Network network = createNetworkWithFrenchLine(5.0, 1.0); // ratio = 5, acceptable

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning in ReportNode
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio"));
    }

    /**
     * Test that French branches with r/|x| <= 1 pass validation without warning
     */
    @Test
    void testFrenchBranchWithLowImpedanceRatioPasses() {
        Network network = createNetworkWithFrenchLine(0.5, 1.0); // ratio = 0.5 < 1

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have no warnings about impedance ratio
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio"));
    }

    /**
     * Test that non-French branches with r > |x| generate a warning but don't throw
     */
    @Test
    void testNonFrenchBranchWithHighImpedanceRatioGeneratesWarning() {
        Network network = createNetworkWithInternationalLine(3.0, 2.0); // ratio = 1.5 > 1

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning in ReportNode
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio"));
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio"));
    }

    /**
     * Test that non-French branches with r <= |x| pass validation without warning
     */
    @Test
    void testNonFrenchBranchWithLowImpedanceRatioPasses() {
        Network network = createNetworkWithInternationalLine(0.5, 1.0); // ratio = 0.5 < 1

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have no warnings about non-French branches
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio"));
    }

    /**
     * Test transformer (two windings) with high impedance ratio
     */
    @Test
    void testFrenchTransformerWithVeryHighImpedanceRatioThrowsError() {
        Network network = createNetworkWithFrenchTransformer(15.0, 1.0); // ratio = 15 > 10

        OpenReacParameters params = new OpenReacParameters();

        InvalidParametersException exception = assertThrows(InvalidParametersException.class,
                () -> params.checkIntegrity(network, ReportNode.NO_OP));

        assertTrue(exception.getMessage().contains("French branches have r > 10 * |x|"));
        assertTrue(exception.getMessage().contains("'TRANSFO_FR'"));
    }

    /**
     * Test mixed network with multiple violations
     */
    @Test
    void testMixedNetworkWithMultipleViolations() {
        Network network = createMixedNetwork();

        OpenReacParameters params = new OpenReacParameters();

        InvalidParametersException exception = assertThrows(InvalidParametersException.class,
                () -> params.checkIntegrity(network, ReportNode.NO_OP));

        // Should mention French branch error
        assertTrue(exception.getMessage().contains("French branches have r > 10 * |x|"));

        // Should have counted both French and non-French problematic branches
        // (we can't easily check warnings in the exception case, but they should be logged)
    }

    /**
     * Test with negative reactance (edge case)
     */
    @Test
    void testBranchWithNegativeReactance() {
        Network network = createNetworkWithFrenchLine(5.0, -1.0); // ratio = 5 / |-1| = 5

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw (ratio = 5, which is acceptable)
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning (1 < 5 <= 10)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio"));
    }

    // ========== Tests for low impedance detection (sqrt(r² + x²) <= threshold) ==========

    /**
     * Test that branches with low impedance (small r and x) generate a warning
     * For 400 kV: zBase = 1600 Ω, threshold = 1e-4 × 1600 = 0.16 Ω
     * With low impedance, ratio will always be <= 1 after threshold replacement
     */
    @Test
    void testBranchWithLowImpedanceGeneratesWarning() {
        // r=0.1, x=0.1 -> sqrt(0.01 + 0.01) = 0.141 Ω < 0.16 Ω -> low impedance
        // After replacement x = 0.16 Ω, ratio = 0.1 / 0.16 = 0.625 < 1 -> passes ratio check
        Network network = createNetworkWithFrenchLine(0.1, 0.1);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw (ratio after threshold is < 1)
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning about low impedance
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowImpedance"));

        // Should NOT have ratio warnings (ratio < 1)
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio"));
    }

    /**
     * Test branch with impedance exactly at threshold boundary with maximum possible r
     * r=0.16, x=0 -> sqrt(0.0256 + 0.0) = 0.16 Ω (exactly at threshold)
     */
    @Test
    void testBranchWithImpedanceAtThresholdAndMaxR() {
        // After replacement x = 0.16, ratio = 0.16/0.16 = 1.0 (boundary)
        Network network = createNetworkWithFrenchLine(0.16, 0.0);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have low impedance warning (0.16 <= 0.16 is true)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowImpedance"));

        // Should NOT have ratio warning (1 <= 1 after threshold replacement)
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio"));
    }

    /**
     * Test branch with impedance exactly at threshold boundary with maximum possible x
     * r=0, x=0.16 -> sqrt(0.0 + 0.0256) = 0.16 Ω (exactly at threshold)
     */
    @Test
    void testBranchWithImpedanceAtThresholdAndMaxX() {
        // After replacement x = 0.16, ratio = 0/0.16 = 0.0
        Network network = createNetworkWithFrenchLine(0.0, 0.16);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have low impedance warning (0.16 <= 0.16 is true)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowImpedance"));

        // Should NOT have ratio warning (0 <= 1 after threshold replacement)
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio"));
    }

    /**
     * Test branch with impedance above threshold
     * r=2.0, x=0.2 -> sqrt(4 + 0.04) = 2.01 Ω > 0.16 Ω -> normal check applies
     */
    @Test
    void testBranchJustAboveImpedanceThresholdWithHighRatio() {
        // Above threshold: ratio = 2.0 / 0.2 = 10 (exactly at French warning boundary)
        Network network = createNetworkWithFrenchLine(2.0, 0.2);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw (ratio = 10, condition for error is ratio > 10)
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should NOT have low impedance warning
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowImpedance"));

        // Should have warning about acceptable high ratio (1 < 10 <= 10)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.frenchBranchWithAcceptableHighImpedanceRatio"));
    }

    /**
     * Test non-French branch with low impedance
     */
    @Test
    void testNonFrenchBranchWithLowImpedance() {
        // International line with low impedance
        Network network = createNetworkWithInternationalLine(0.1, 0.1);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have low impedance warning (applies to all branches)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowImpedance"));

        // Should NOT have ratio warning (ratio < 1 after threshold replacement)
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbNonFrenchBranchesWithHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nonFrenchBranchWithHighImpedanceRatio"));
    }

    // ========== Helper methods to create test networks ==========

    /**
     * Create a network with a French line (both substations in France)
     */
    private Network createNetworkWithFrenchLine(double r, double x) {
        Network network = Network.create("test", "test");

        Substation s1 = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();

        Substation s2 = network.newSubstation()
                .setId("S2")
                .setCountry(Country.FR)
                .add();

        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("VL2")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        vl1.getBusBreakerView().newBus().setId("B1").add();
        vl2.getBusBreakerView().newBus().setId("B2").add();

        network.newLine()
                .setId("LINE_FR")
                .setVoltageLevel1("VL1")
                .setBus1("B1")
                .setVoltageLevel2("VL2")
                .setBus2("B2")
                .setR(r)
                .setX(x)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        return network;
    }

    /**
     * Create a network with an international line (France - Germany)
     */
    private Network createNetworkWithInternationalLine(double r, double x) {
        Network network = Network.create("test", "test");

        Substation s1 = network.newSubstation()
                .setId("S1_FR")
                .setCountry(Country.FR)
                .add();

        Substation s2 = network.newSubstation()
                .setId("S2_DE")
                .setCountry(Country.DE)
                .add();

        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("VL2")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        vl1.getBusBreakerView().newBus().setId("B1").add();
        vl2.getBusBreakerView().newBus().setId("B2").add();

        network.newLine()
                .setId("LINE_INTL")
                .setVoltageLevel1("VL1")
                .setBus1("B1")
                .setVoltageLevel2("VL2")
                .setBus2("B2")
                .setR(r)
                .setX(x)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        return network;
    }

    /**
     * Create a network with a French transformer
     */
    private Network createNetworkWithFrenchTransformer(double r, double x) {
        Network network = Network.create("test", "test");

        Substation s = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();

        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("VL2")
                .setNominalV(225.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(220.0)
                .setHighVoltageLimit(245.0)
                .add();

        vl1.getBusBreakerView().newBus().setId("B1").add();
        vl2.getBusBreakerView().newBus().setId("B2").add();

        s.newTwoWindingsTransformer()
                .setId("TRANSFO_FR")
                .setVoltageLevel1("VL1")
                .setBus1("B1")
                .setVoltageLevel2("VL2")
                .setBus2("B2")
                .setR(r)
                .setX(x)
                .setG(0.0)
                .setB(0.0)
                .setRatedU1(400.0)
                .setRatedU2(225.0)
                .add();

        return network;
    }

    /**
     * Create a mixed network with multiple branches of different types
     */
    private Network createMixedNetwork() {
        Network network = Network.create("test", "test");

        // French substations
        Substation sFr1 = network.newSubstation()
                .setId("S_FR1")
                .setCountry(Country.FR)
                .add();

        Substation sFr2 = network.newSubstation()
                .setId("S_FR2")
                .setCountry(Country.FR)
                .add();

        // German substation
        Substation sDe = network.newSubstation()
                .setId("S_DE")
                .setCountry(Country.DE)
                .add();

        VoltageLevel vlFr1 = sFr1.newVoltageLevel()
                .setId("VL_FR1")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        VoltageLevel vlFr2 = sFr2.newVoltageLevel()
                .setId("VL_FR2")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        VoltageLevel vlDe = sDe.newVoltageLevel()
                .setId("VL_DE")
                .setNominalV(400.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380.0)
                .setHighVoltageLimit(420.0)
                .add();

        vlFr1.getBusBreakerView().newBus().setId("B_FR1").add();
        vlFr2.getBusBreakerView().newBus().setId("B_FR2").add();
        vlDe.getBusBreakerView().newBus().setId("B_DE").add();

        // French line with very high ratio (will throw error)
        network.newLine()
                .setId("LINE_FR_BAD")
                .setVoltageLevel1("VL_FR1")
                .setBus1("B_FR1")
                .setVoltageLevel2("VL_FR2")
                .setBus2("B_FR2")
                .setR(12.0)  // ratio = 12 > 10 -> ERROR
                .setX(1.0)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        // French line with moderate ratio (will generate warning)
        network.newLine()
                .setId("LINE_FR_WARN")
                .setVoltageLevel1("VL_FR1")
                .setBus1("B_FR1")
                .setVoltageLevel2("VL_FR2")
                .setBus2("B_FR2")
                .setR(3.0)  // ratio = 3, 1 < 3 <= 10 -> WARNING
                .setX(1.0)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        // International line with high ratio (will generate warning)
        network.newLine()
                .setId("LINE_INTL_WARN")
                .setVoltageLevel1("VL_FR1")
                .setBus1("B_FR1")
                .setVoltageLevel2("VL_DE")
                .setBus2("B_DE")
                .setR(2.0)  // ratio = 2 > 1 -> WARNING
                .setX(1.0)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        return network;
    }
}
