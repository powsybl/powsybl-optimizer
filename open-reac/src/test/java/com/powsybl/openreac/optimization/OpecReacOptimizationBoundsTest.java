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
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        network.getLine("l45").getTerminal2().disconnect();
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
        network.getLine("l45").getTerminal2().disconnect();
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

}
