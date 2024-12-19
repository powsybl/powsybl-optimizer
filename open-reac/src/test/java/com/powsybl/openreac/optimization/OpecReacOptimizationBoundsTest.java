/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the generator bounds in OpenReac optimization.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpecReacOptimizationBoundsTest extends AbstractOpenReacRunnerTest {

    @Test
    void testGeneratorsMaxPBounds() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        // due to the disconnection, the network is now imbalanced
        // and max p of generators is not enough to provide balance
        network.getLine("l45").disconnect();
        OpenReacResult result = runOpenReac(network, "optimization/bounds/generators-pmax-too-small", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());

        // increase max p of generators to allow power balance
        network.getGenerator("g2").setMaxP(2.5);
        network.getGenerator("g3").setMaxP(2.5);
        result = runOpenReac(network, "optimization/bounds/generators-pmax", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
    }

    @Test
    void testGeneratorsMinPBounds() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        // due to the modifications, the network is now imbalanced
        // and min p of generators is not small enough to provide balance
        network.getLine("l45").disconnect();
        network.getLoad("l4").setP0(3);
        network.getGenerator("g2").setMinP(2);
        network.getGenerator("g3").setMinP(2);
        OpenReacResult result = runOpenReac(network, "optimization/bounds/generators-pmin-too-high", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());

        // decrease min p of generators to allow power balance
        // but targetP will be fixed in optimization, because it is too close of maxP
        network.getGenerator("g2").setMinP(1);
        network.getGenerator("g3").setMinP(1);
        result = runOpenReac(network, "optimization/bounds/generators-target-p-too-close-pmax", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());

        // increase max p of generators to allow modification of targetP in optimization
        network.getGenerator("g2").setMaxP(2.5);
        network.getGenerator("g3").setMaxP(2.5);
        result = runOpenReac(network, "optimization/bounds/generators-pmin", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
    }

//    @Test
//    void testGeneratorQmaxPmaxRatioBounds() throws IOException {
//        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
//        network.getLine("l45").disconnect();
//        network.getLoad("l4").setP0(4).setQ0(2);
//
//        OpenReacParameters parameters = new OpenReacParameters();
//        OpenReacResult result = runOpenReac(network, "optimization/bounds/qmax-pmax-default-ratio", parameters, true);
//        assertEquals(OpenReacStatus.OK, result.getStatus());
//        // there are slacks as Q bounds are not large enough
//        assertTrue(Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")) > 0);
//
//        parameters.setDefaultQmaxPmaxRatio(1);
//        result = runOpenReac(network, "optimization/bounds/same-qmax-pmax", parameters, true);
//        assertEquals(OpenReacStatus.OK, result.getStatus());
//        // Q bounds are large enough to remove reactive slacks in optimization
//        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")));
//    }

    @Test
    void testGeneratorQBoundsTargetPInsidePminPmax() throws IOException {
        Network network = createWithTwoBuses(101, 50, 150, 0, -100, 100, 150, -200, 200, 150);
        assertConvergence(network);
        network = createWithTwoBuses(101, 50, 150, 0, -100, 100, 150, -150, 150, 150);
        assertDivergence(network);
    }

    @Test
    void testGeneratorQBoundsTargetPInsidePminPmax2() throws IOException {
        Network network = createWithTwoBuses(-10, -20, -5, -20, -30, 30, -5, -5, 5, -10);
        assertConvergence(network);
        network = createWithTwoBuses(-10, -20, -5, -20, -18, 18, -5, -5, 5, -10);
        assertDivergence(network);
    }

    @Test
    void testGeneratorQBoundsTargetPInside0Pmax() throws IOException {
        Network network = createWithTwoBuses(101, -50, 150, 0, -0, 0, 150, -300, 300, 150);
        assertConvergence(network);
        network = createWithTwoBuses(101, -50, 150, 0, -0, 0, 150, -100, 100, 150);
        assertDivergence(network);
    }

    @Test
    void testGeneratorQBoundsTargetPInside0Pmax2() throws IOException {
        Network network = createWithTwoBuses(-4, -20, -5, -5, -5, 5, 0, -1, 1, -4);
        assertConvergence(network);
        network = createWithTwoBuses(-4, -20, -5, -5, -4.25, 4.25, 0, -1, 1, -4);
        assertDivergence(network);
    }

    @Test
    void testGeneratorQBoundsTargetPInside0Pmin() throws IOException {
        Network network = createWithTwoBuses(101, 150, 200,
                0, -0, 0,
                150, -150, 150, 100);
        assertConvergence(network);
        network = createWithTwoBuses(101, 150, 200,
                0, -0, 0,
                150, -140, 140, 100);
        assertDivergence(network);
    }

    @Test
    void testGeneratorQBoundsTargetPInside0Pmin2() throws IOException {
        Network network = createWithTwoBuses(-5.1, -10, 10,
                0, -0, 0,
                -10, -4, 4,
                -2);
        assertConvergence(network);
        network = createWithTwoBuses(-5.1, -10, 10,
                0, -0, 0,
                -10, -3.8, 3.8,
                -2);
        assertDivergence(network);
    }

    void assertConvergence(Network network) throws IOException {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setMinPlausibleLowVoltageLimit(0.4);
        parameters.setMaxPlausibleHighVoltageLimit(1.6);
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED);
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    void assertDivergence(Network network) throws IOException {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED);
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    /**
     * g1        ld1
     * |          |
     * b1---------b2
     *      l1
     */
    public static Network createWithTwoBuses(double g1TargetP, double g1PMin, double g1PMax,
                                             double pValue1, double g1QMin1, double g1QMax1,
                                             double pValue2, double g1QMin2, double g1QMax2,
                                             double ld1TargetQ) {
        Network network = Network.create("q-bounds", "test");
        Substation s1 = network.newSubstation()
                .setId("S1")
                .add();
        Substation s2 = network.newSubstation()
                .setId("S2")
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("vl1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        vl1.newGenerator()
                .setId("g1")
                .setConnectableBus("b1")
                .setBus("b1")
                .setTargetP(g1TargetP)
                .setTargetV(390)
                .setMinP(g1PMin)
                .setMaxP(g1PMax)
                .setVoltageRegulatorOn(true)
                .add();
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(pValue1)
                .setMinQ(g1QMin1)
                .setMaxQ(g1QMax1)
                .endPoint()
                .beginPoint()
                .setP(pValue2)
                .setMinQ(g1QMin2)
                .setMaxQ(g1QMax2)
                .endPoint()
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
                .setP0(g1TargetP)
                .setQ0(ld1TargetQ)
                .add();
        network.newLine()
                .setId("l1")
                .setBus1("b1")
                .setBus2("b2")
                .setR(0.1)
                .setX(3)
                .add();
        return network;
    }

}
