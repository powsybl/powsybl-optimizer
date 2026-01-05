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

        // Should have no warnings about impedance
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
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
    }

    /**
     * Test that branches with very low reactance generate a warning and use threshold for ratio check
     */
    @Test
    void testBranchWithVeryLowReactanceGeneratesWarningAndUsesThreshold() {
        // x = 1e-5 < threshold (1e-4), so x will be replaced by 1e-4
        // ratio = 10.0 / 1e-4 = 100,000 >> 10, so this should throw
        Network network = createNetworkWithFrenchLine(10.0, 1e-5);

        OpenReacParameters params = new OpenReacParameters();

        // Should throw because ratio with threshold is still very high (100,000 > 10)
        InvalidParametersException exception = assertThrows(InvalidParametersException.class,
                () -> params.checkIntegrity(network, ReportNode.NO_OP));

        assertTrue(exception.getMessage().contains("French branches have r > 10 * |x|"));
    }

    /**
     * Test that branches with low reactance that result in acceptable ratio after threshold replacement
     */
    @Test
    void testBranchWithLowReactanceButAcceptableRatioAfterThreshold() {
        // x = 1e-5 < threshold (1e-4), so x will be replaced by 1e-4
        // ratio = 5e-4 / 1e-4 = 5, which is acceptable (1 < 5 <= 10)
        Network network = createNetworkWithFrenchLine(5e-4, 1e-5);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw (ratio with threshold is 5, which is acceptable)
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning about low reactance
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowReactance"));

        // Should also have warning about acceptable high ratio (1 < 5 <= 10)
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
    }

    /**
     * Test that branches with low reactance and low resistance pass all checks with only low reactance warning
     */
    @Test
    void testBranchWithLowReactanceAndLowResistance() {
        // x = 1e-5 < threshold (1e-4), so x will be replaced by 1e-4
        // ratio = 5e-5 / 1e-4 = 0.5 < 1, which passes
        Network network = createNetworkWithFrenchLine(5e-5, 1e-5);

        OpenReacParameters params = new OpenReacParameters();
        ReportNode reportNode = createReportNode();

        // Should not throw (ratio with threshold is 0.5 < 1)
        assertDoesNotThrow(() -> params.checkIntegrity(network, reportNode));

        // Should have warning about low reactance
        assertTrue(hasReportWithKey(reportNode, "optimizer.openreac.nbBranchesWithLowReactance"));

        // Should NOT have ratio warnings (ratio < 1)
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithAcceptableHighImpedanceRatio"));
        assertFalse(hasReportWithKey(reportNode, "optimizer.openreac.nbFrenchBranchesWithHighImpedanceRatio"));
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
