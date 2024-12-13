/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the equipment bounds in OpenReac optimization.
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

    @Test
    void testGeneratorRectangularQBounds() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        network.getLine("l45").disconnect();
        network.getLoad("l4").setP0(4).setQ0(2);

        // set reactive limits to both generators
        network.getGenerator("g2").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-0.25)
                .setMaxQ(0.25)
                .endPoint()
                .beginPoint()
                .setP(2)
                .setMinQ(-2)
                .setMaxQ(2)
                .endPoint()
                .add();
        network.getGenerator("g3").newReactiveCapabilityCurve()
                .beginPoint()
                .setP(0)
                .setMinQ(-0.25)
                .setMaxQ(0.25)
                .endPoint()
                .beginPoint()
                .setP(2)
                .setMinQ(-2)
                .setMaxQ(2)
                .endPoint()
                .add();

        OpenReacResult result = runOpenReac(network, "optimization/bounds/generator-rectangular-bounds", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // rectangular bounds in ACOPF implies Q bounds are not large enough to remove reactive slacks in optimization
        assertTrue(Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")) > 0);
    }

    @Test
    void testGeneratorQmaxPmaxRatioBounds() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithTwoVoltageControls();
        network.getLine("l45").disconnect();
        network.getLoad("l4").setP0(4).setQ0(2);

        OpenReacParameters parameters = new OpenReacParameters();
        OpenReacResult result = runOpenReac(network, "optimization/bounds/qmax-pmax-default-ratio", parameters, true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // there are slacks as Q bounds are not large enough
        assertTrue(Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")) > 0);

        parameters.setDefaultQmaxPmaxRatio(1);
        result = runOpenReac(network, "optimization/bounds/same-qmax-pmax", parameters, true);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // Q bounds are large enough to remove reactive slacks in optimization
        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_reactive_slacks")));
    }

}
