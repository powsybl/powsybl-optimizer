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
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
class ShuntCompensatorNetworkOutputTest {
    @Test
    void read() throws IOException {
        Network network = create();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "SHUNT");
        for (int i = 0; i < 7; i++) {
            mapper.newInt(AmplSubset.BUS, "BUS" + i);
        }
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
            assertEquals(1, output.getModifications().size());
            assertEquals("SHUNT", output.getModifications().get(0).getShuntCompensatorId());
            assertNull(output.getModifications().get(0).getConnect());
            assertEquals(0, output.getModifications().get(0).getSectionCount());
        }
    }

    @Test
    void readNullShuntCompensator() throws IOException {
        Network network = create();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "wrongId");
        for (int i = 0; i < 7; i++) {
            mapper.newInt(AmplSubset.BUS, "BUS" + i);
        }
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
            assertEquals(0, output.getModifications().size());
        }
    }

    @Test
    void noShuntNumberInMapper() throws IOException {
        Network network = create();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            assertThrows(IllegalArgumentException.class, () -> output.read(reader, mapper));
        }
    }

    @Test
    void noBusNumberInMapper() throws IOException {
        Network network = create();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "SHUNT");
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            assertThrows(IllegalArgumentException.class, () -> output.read(reader, mapper));
        }
    }

}
