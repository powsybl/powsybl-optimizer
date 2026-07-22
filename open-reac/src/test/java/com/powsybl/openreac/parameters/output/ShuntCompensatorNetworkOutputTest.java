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
import java.util.Map;

import static com.powsybl.openreac.network.ShuntNetworkFactory.createWithNonLinearModel;
import static com.powsybl.openreac.network.ShuntNetworkFactory.createWithTwoShuntCompensators;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
class ShuntCompensatorNetworkOutputTest {
    @Test
    void read() throws IOException {
        Network network = createWithNonLinearModel();
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
        Network network = createWithNonLinearModel();
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
        Network network = createWithNonLinearModel();
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
        Network network = createWithNonLinearModel();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "SHUNT");
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            assertThrows(IllegalArgumentException.class, () -> output.read(reader, mapper));
        }
    }

    /**
     * SHUNT and SHUNT2 sit on a 400 kV voltage level and provide sections at b = 0, 1e-3 and
     * 3e-3 S. The optimizer returns 2.4 pu and 3.6 pu, that is 1.5e-3 S and 2.25e-3 S once
     * brought back to siemens, which is what issue #18 asks to expose.
     */
    @Test
    void continuousSusceptanceIsExposedPerShunt() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs();
        Map<String, Double> continuousSusceptance = output.getContinuousSusceptanceByShunt();
        assertEquals(2, continuousSusceptance.size());
        assertEquals(1.5e-3, continuousSusceptance.get("SHUNT"), 1e-9);
        assertEquals(2.25e-3, continuousSusceptance.get("SHUNT2"), 1e-9);
    }

    /**
     * The retained sections are 1 and 2, so the discarded susceptances are 5e-4 S and 7.5e-4 S.
     * At 400 kV those are 80 MVar and 120 MVar. Asserting on the reactive values pins the unit
     * down: raw susceptances would be smaller by a factor 400^2.
     */
    @Test
    void reactiveDeviationIsExpressedInMvar() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs();
        Map<String, Double> reactiveDeviation = output.getReactiveDeviationByShunt();
        assertEquals(2, reactiveDeviation.size());
        assertEquals(80, reactiveDeviation.get("SHUNT"), 1e-6);
        assertEquals(120, reactiveDeviation.get("SHUNT2"), 1e-6);
    }

    @Test
    void reactiveDeviationIsSummedOverShunts() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs();
        double sumOverShunts = output.getReactiveDeviationByShunt().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(200, output.getTotalReactiveDeviation(), 1e-6);
        assertEquals(sumOverShunts, output.getTotalReactiveDeviation(), 1e-6);
    }

    private ShuntCompensatorNetworkOutput readRoundingOutputs() throws IOException {
        Network network = createWithTwoShuntCompensators();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "SHUNT");
        mapper.newInt(AmplSubset.SHUNT, "SHUNT2");
        for (int i = 0; i < 7; i++) {
            mapper.newInt(AmplSubset.BUS, "BUS" + i);
        }
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_shunts_rounding.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
        }
        return output;
    }
}
