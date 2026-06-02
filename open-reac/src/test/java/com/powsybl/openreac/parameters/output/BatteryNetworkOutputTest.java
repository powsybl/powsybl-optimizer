/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.BatteryModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.output.network.BatteryNetworkOutput;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.powsybl.openreac.network.BatteryNetworkFactory.create;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class BatteryNetworkOutputTest {

    @Test
    void read() throws IOException {
        Network network = create();
        BatteryNetworkOutput output = new BatteryNetworkOutput(network);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.BATTERY, "BATTERY");
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_batteries.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
            assertEquals(1, output.getModifications().size());
            BatteryModification modif = output.getModifications().getFirst();
            assertEquals("BATTERY", modif.getBatteryId());
            // Active power is not optimized by OpenReac for batteries.
            assertNull(modif.getTargetP());
            assertEquals(25.5, modif.getTargetQ());
        }
    }

    @Test
    void noBatteryNumberInMapper() throws IOException {
        Network network = create();
        BatteryNetworkOutput output = new BatteryNetworkOutput(network);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_batteries.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            assertThrows(IllegalArgumentException.class, () -> output.read(reader, mapper));
        }
    }
}
