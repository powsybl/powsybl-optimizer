/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olivier Perrin {@literal <olivier.perrin at rte-france.com>}
 */
class ReactiveSlackOutputTest {

    @Test
    void testReactiveSlack() {
        ReactiveSlackOutput.ReactiveSlack reactiveSlack = new ReactiveSlackOutput.ReactiveSlack("busId", "vlId", 10);
        assertEquals("busId", reactiveSlack.getBusId());
        assertEquals("vlId", reactiveSlack.getVoltageLevelId());
        assertEquals(10, reactiveSlack.getSlack());
    }

    @Test
    void readTest() throws IOException {
        ReactiveSlackOutput output = new ReactiveSlackOutput();
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_reactive_slacks.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {

            output.read(reader, null);
            assertEquals(2, output.getSlacks().size());
            ReactiveSlackOutput.ReactiveSlack slack1 = output.getSlacks().get(0);
            ReactiveSlackOutput.ReactiveSlack slack2 = output.getSlacks().get(1);
            Assertions.assertAll(
                    () -> assertEquals(1.6, slack1.getSlack(), 0.01),
                    () -> assertEquals("slack1", slack1.getBusId()),
                    () -> assertEquals("voltageLevel1", slack1.getVoltageLevelId()),
                    () -> assertEquals(Double.NaN, slack2.getSlack()),
                    () -> assertEquals("slack2", slack2.getBusId()),
                    () -> assertEquals("voltageLevel2", slack2.getVoltageLevelId())
            );
        }
    }

}
