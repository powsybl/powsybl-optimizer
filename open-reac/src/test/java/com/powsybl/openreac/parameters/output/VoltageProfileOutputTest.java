/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
class VoltageProfileOutputTest {

    @Test
    void readTest() throws IOException {
        VoltageProfileOutput output = new VoltageProfileOutput();
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_voltages.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {

            output.read(reader, null);
            assertEquals(3, output.getVoltageProfile().size());
            Pair<Double, Double> busVoltage1 = output.getVoltageProfile().get("bus1");
            Pair<Double, Double> busVoltage2 = output.getVoltageProfile().get("bus2");
            Pair<Double, Double> busVoltage3 = output.getVoltageProfile().get("bus3");
            Assertions.assertAll(
                    () -> assertEquals(0.8, busVoltage1.getFirst()),
                    () -> assertEquals(1.1, busVoltage1.getSecond()),
                    () -> assertEquals(1.2, busVoltage2.getFirst()),
                    () -> assertEquals(Double.NaN, busVoltage2.getSecond()),
                    () -> assertEquals(Double.NaN, busVoltage3.getFirst()),
                    () -> assertEquals(0.11, busVoltage3.getSecond())
            );
        }
    }

    @Test
    void testErrorState() {
        VoltageProfileOutput output = new VoltageProfileOutput();
        boolean throwOnMissingFile = output.throwOnMissingFile();
        assertFalse(throwOnMissingFile);
        assertTrue(output.isErrorState());
    }
}
