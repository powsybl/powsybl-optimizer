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
class OpenReacOptimizationBoundsTest extends AbstractOpenReacRunnerTest {

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
    void testGeneratorQBoundsAtTargetP() throws IOException {
        // verify feasibility when minQ/maxQ are read on the reactive capability curve at targetP
        Network network = create(101, 50, 150, 150);
        setSymmetricReactiveCapabilityCurve(network.getGenerator("g1"), 0, 100, 100, 170, 150, 100);
        testAllModifAndLoadFlow(network, "optimization/bounds/generator-q-bounds-at-target-p/valid", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        network = create(101, 50, 150, 150);
        setSymmetricReactiveCapabilityCurve(network.getGenerator("g1"), 0, 100, 100, 130, 150, 100);
        OpenReacResult result = runOpenReac(network, "optimization/bounds/generator-q-bounds-at-target-p/invalid", parameters, true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testGeneratorQBoundsExtrapolated() throws IOException {
        // verify feasibility when minQ/maxQ are extrapolated as targetP is outside the reactive capability curve
        Network network = create(102, 50, 100, 150);
        setSymmetricReactiveCapabilityCurve(network.getGenerator("g1"), 50, 0, 100, 148);
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
        testAllModifAndLoadFlow(network, "optimization/bounds/generator-q-bounds-extrapolated/valid", parameters, ReportNode.NO_OP);

        // verify non convergence due to insufficient bounds
        setSymmetricReactiveCapabilityCurve(network.getGenerator("g1"), 50, 0, 100, 140);
        OpenReacResult result = runOpenReac(network, "optimization/bounds/generator-q-bounds-extrapolated/invalid", parameters, true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    private static void setSymmetricReactiveCapabilityCurve(Generator generator, double... pAndMaxQ) {
        ReactiveCapabilityCurveAdder adder = generator.newReactiveCapabilityCurve();
        for (int i = 0; i < pAndMaxQ.length; i += 2) {
            adder.beginPoint().setP(pAndMaxQ[i]).setMinQ(-pAndMaxQ[i + 1]).setMaxQ(pAndMaxQ[i + 1]).endPoint();
        }
        adder.add();
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
                .setLowVoltageLimit(360)
                .setHighVoltageLimit(440)
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
                .setLowVoltageLimit(360)
                .setHighVoltageLimit(440)
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
