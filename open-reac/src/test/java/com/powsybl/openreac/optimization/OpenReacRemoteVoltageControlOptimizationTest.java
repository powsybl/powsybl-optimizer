/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the remote voltage control in OpenReac optimization.
 * Note that it is not taken into account in optimization model, but the output of
 * should consider this when updating voltage targets.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpenReacRemoteVoltageControlOptimizationTest extends AbstractOpenReacRunnerTest {

    @Test
    void testGeneratorRemoteVoltageControl() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithGeneratorRemoteControl();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g3"));

        OpenReacResult result = runOpenReac(network, "optimization/remote-voltage-control/generator", parameters, false);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(3, result.getGeneratorModifications().size());

        double targetV = result.getVoltageProfile().get("vl4_0").getFirst() * network.getVoltageLevel("vl4").getNominalV();
        // verify generators modification include remote voltage target modification
        assertEquals(targetV, result.getGeneratorModifications().get(0).getModifs().getTargetV());
        assertEquals(targetV, result.getGeneratorModifications().get(1).getModifs().getTargetV());
        // same with generator indicated as constant Q
        assertEquals(targetV, result.getGeneratorModifications().get(2).getModifs().getTargetV());

        result.applyAllModifications(network);
        assertEquals(targetV, network.getGenerator("g1").getTargetV());
        assertEquals(targetV, network.getGenerator("g2").getTargetV());
        assertEquals(targetV, network.getGenerator("g3").getTargetV());
    }

    @Test
    void testStaticVarCompensatorRemoteVoltageControl() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithStaticVarCompensator();
        network.getGenerator("g1")
                .setTargetQ(5);
        StaticVarCompensator svc = network.getStaticVarCompensator("svc1");
        svc.setVoltageSetpoint(390)
            .setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
            .setRegulatingTerminal(network.getGenerator("g1").getTerminal());

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g1")); // fix Q of g1 to ensure voltage difference

        OpenReacResult result = runOpenReac(network, "optimization/remote-voltage-control/svc", parameters, false);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(1, result.getSvcModifications().size());

        double targetV = result.getVoltageProfile().get("vl1_0").getFirst() * network.getVoltageLevel("vl1").getNominalV();
        assertEquals(targetV, result.getSvcModifications().get(0).getVoltageSetpoint());

        result.applyAllModifications(network);
        assertEquals(targetV, svc.getVoltageSetpoint());
    }
}
