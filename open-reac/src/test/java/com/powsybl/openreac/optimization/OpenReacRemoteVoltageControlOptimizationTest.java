/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
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
 * It is not considered in optimization model, but the output of
 * optimization should take this into account when updating voltage targets.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpenReacRemoteVoltageControlOptimizationTest extends AbstractOpenReacRunnerTest {

    @Test
    void testGeneratorRemoteVoltageControl() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithGeneratorRemoteControl();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(List.of("g2"));

        // run open reac and apply results
        OpenReacResult result = runOpenReac(network, "", false);
        assertEquals(OpenReacStatus.OK, result.getStatus());
        // TODO : verify directly the results of open reac

        result.applyAllModifications(network);
        Bus b4 = network.getBusBreakerView().getBus("b4");
        assertEquals(b4.getV(), network.getGenerator("g1").getTargetV());
        // verify V target of fixed Q generator has also been updated
        assertEquals(b4.getV(), network.getGenerator("g2").getTargetV());
        assertEquals(b4.getV(), network.getGenerator("g3").getTargetV());
    }

}
