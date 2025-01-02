/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the generator bounds in OpenReac optimization.
 * Note that depending on the solver, the results may vary. Only the feasibility is tested,
 * meaning that it verifies whether the generator bounds allow for finding
 * a feasible solution or not.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpecReacOptimizationBoundsTest extends AbstractOpenReacRunnerTest {

    private OpenReacParameters parameters;

    @Override
    @BeforeEach
    public void setUp() throws IOException {
        super.setUp();
        parameters = new OpenReacParameters();
        // remove reactive slacks to ensure non convergence in case of infeasibility
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED);
    }

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

    @Test
    void testGeneratorQmaxPmaxRatioBounds() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        network.getLine("l45").disconnect();
        network.getLoad("l4").setP0(4).setQ0(2);

        OpenReacResult result = runOpenReac(network, "optimization/bounds/qmax-pmax-default-ratio", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // there are slacks as Q bounds are not large enough
        assertTrue(Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")) > 0);

        parameters.setDefaultQmaxPmaxRatio(1);
        result = runOpenReac(network, "optimization/bounds/same-qmax-pmax", parameters, true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // Q bounds are large enough to remove reactive slacks in optimization
        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")));
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenMinPMaxP() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and MaxP (0 < minP < targetP < maxP)
        Network network = create(101, 50, 150, 150);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-100)
                .setMaxQ(100)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-200)
                .setMaxQ(200)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network = create(101, 50, 150, 150);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-100)
                .setMaxQ(100)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-150)
                .setMaxQ(150)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenMinPMaxP2() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and MaxP (minP < targetP < maxP < 0)
        Network network = create(-10, -20, -5, -10);
        setDefaultVoltageLimits(network, 0.85, 1.15);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-20)
                .setMinQ(-30)
                .setMaxQ(30)
                .endPoint()
                .beginPoint()
                .setP(-5)
                .setMinQ(-5)
                .setMaxQ(5)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-20)
                .setMinQ(-18)
                .setMaxQ(18)
                .endPoint()
                .beginPoint()
                .setP(-5)
                .setMinQ(-5)
                .setMaxQ(5)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenP0MaxP() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and P0 (minP < 0 < targetP < maxP)
        Network network = create(101, -50, 150, 150);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-300)
                .setMaxQ(300)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-100)
                .setMaxQ(100)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenP0MaxP2() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and P0 (maxP < targetP < 0)
        Network network = create(-4, -20, -5, -4);
        setDefaultVoltageLimits(network, 0.85, 1.15);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-5)
                .setMinQ(-5)
                .setMaxQ(5)
                .endPoint()
                .beginPoint()
                .setP(0)
                .setMinQ(-1)
                .setMaxQ(1)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(-5)
                .setMinQ(-4.25)
                .setMaxQ(4.25)
                .endPoint()
                .beginPoint()
                .setP(0)
                .setMinQ(-1)
                .setMaxQ(1)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenP0MinP() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and P0 (0 < targetP < minP)
        Network network = create(101, 150, 200, 100);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-150)
                .setMaxQ(150)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(150)
                .setMinQ(-140)
                .setMaxQ(140)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsInterpolatedBetweenP0MinP2() throws IOException {
        // verify feasibility when minQ/maxQ are interpolated between bounds at MinP and P0 (minP < targetP < 0 < maxP)
        Network network = create(-5.1, -10, 10, -2);
        setDefaultVoltageLimits(network, 0.85, 1.15);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(-10)
                .setMinQ(-4)
                .setMaxQ(4)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(-10)
                .setMinQ(-3)
                .setMaxQ(3)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsTakenAtMaxP() throws IOException {
        // verify feasibility when minQ/maxQ are taken at maxP (0 < maxP < targetP)
        Network network = create(102, 50, 100, 150);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(50)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(100)
                .setMinQ(-152)
                .setMaxQ(152)
                .endPoint()
                .add();
        // add a generator to allow convergence, as variable P of g1 will be fixed to 102 in optimization
        network.getVoltageLevel("vl1").newGenerator()
                .setId("g2")
                .setConnectableBus("b1")
                .setBus("b1")
                .setTargetP(1)
                .setTargetV(390)
                .setMinP(0)
                .setMaxP(2)
                .setVoltageRegulatorOn(true)
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(50)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(100)
                .setMinQ(-149)
                .setMaxQ(149)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsTakenAtMinP() throws IOException {
        // verify feasibility when minQ/maxQ are taken at minP (targetP < minP < 0)
        Network network = create(-0.5, 0, 10, -0.25);
        setDefaultVoltageLimits(network, 0.85, 1.15);
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-100)
                .setMaxQ(100)
                .endPoint()
                .beginPoint()
                .setP(2)
                .setMinQ(-10)
                .setMaxQ(10)
                .endPoint()
                .add();
        testAllModifAndLoadFlow(network, "", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network.getGenerator("g1").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(0)
                .setMaxQ(0)
                .endPoint()
                .beginPoint()
                .setP(-10)
                .setMinQ(-1.99)
                .setMaxQ(1.99)
                .endPoint()
                .add();
        OpenReacResult result = runOpenReac(network, "", parameters, false);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    /**
     *  g1        ld1
     *  |          |
     *  b1---------b2
     *       l1
     */
    public static Network create(double targetP, double minP, double maxP, double loadTargetQ) {
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
                .setTargetP(targetP)
                .setTargetV(390)
                .setMinP(minP)
                .setMaxP(maxP)
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
                .setP0(targetP)
                .setQ0(loadTargetQ)
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
