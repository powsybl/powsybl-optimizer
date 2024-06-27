/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.output.network.ShuntCompensatorNetworkOutput;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.powsybl.openreac.network.ShuntNetworkFactory.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
public class ShuntCompensatorNetworkOutputTest {

    @Test
    void readTestNullShunt() throws IOException {
        Network network = create();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "shuntId");
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
            assertEquals(1, output.getModifications().size());
            assertEquals("shuntId", output.getModifications().get(0).getShuntCompensatorId());
            assertNull(output.getModifications().get(0).getConnect());
            assertNull(output.getModifications().get(0).getSectionCount());
        }
    }

}
