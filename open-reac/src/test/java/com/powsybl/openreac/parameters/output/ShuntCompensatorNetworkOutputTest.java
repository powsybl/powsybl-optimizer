/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
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

    private static final double NOMINAL_V = 400;

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
     * SHUNT and SHUNT2 sit on a 400 kV voltage level and provide sections at b = 0, 1e-3 and 3e-3 S.
     * The optimizer returns b = 2.4 pu and 3.6 pu on the SB = 100 MVA base, that is
     * b * SB / Vnom^2 = 2.4 * 100 / 400^2 = 1.5e-3 S and 3.6 * 100 / 400^2 = 2.25e-3 S.
     */
    @Test
    void continuousSusceptanceIsExposedPerShunt() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs("/mock_outputs/reactiveopf_results_shunts_rounding.csv");
        Map<String, Double> continuousSusceptance = output.getContinuousSusceptanceByShunt();
        assertEquals(2, continuousSusceptance.size());
        assertEquals(2.4 * AmplConstants.SB / (NOMINAL_V * NOMINAL_V), continuousSusceptance.get("SHUNT"), 1e-9);
        assertEquals(3.6 * AmplConstants.SB / (NOMINAL_V * NOMINAL_V), continuousSusceptance.get("SHUNT2"), 1e-9);
    }

    /**
     * The retained sections are 1 and 2, so the deviations in susceptance are 1e-3 - 1.5e-3 = -5e-4 S
     * and 3e-3 - 2.25e-3 = +7.5e-4 S: the discretization takes reactive power away from SHUNT and
     * adds some to SHUNT2. In MVar at nominal voltage, -5e-4 * 400^2 = -80 and 7.5e-4 * 400^2 = +120.
     */
    @Test
    void reactiveDeviationIsSignedAndInMvar() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs("/mock_outputs/reactiveopf_results_shunts_rounding.csv");
        Map<String, Double> reactiveDeviation = output.getReactiveDeviationByShunt();
        assertEquals(2, reactiveDeviation.size());
        assertEquals(-80, reactiveDeviation.get("SHUNT"), 1e-6);
        assertEquals(120, reactiveDeviation.get("SHUNT2"), 1e-6);
    }

    /**
     * The signed deviations are -80 and +120 MVar, so their net sum would be +40 MVar: asserting
     * 200 pins the total as the sum of the absolute deviations, not as the net balance.
     */
    @Test
    void totalIsTheSumOfAbsoluteDeviations() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs("/mock_outputs/reactiveopf_results_shunts_rounding.csv");
        double sumOfAbsoluteDeviations = output.getReactiveDeviationByShunt().values().stream().mapToDouble(Math::abs).sum();
        assertEquals(200, output.getTotalAbsoluteReactiveDeviation(), 1e-6);
        assertEquals(sumOfAbsoluteDeviations, output.getTotalAbsoluteReactiveDeviation(), 1e-6);
    }

    /**
     * A shunt carrying the AMPL invalid-value sentinel poisons the total to NaN, as documented,
     * rather than being silently skipped from the sum. The per-shunt maps keep NaN for the
     * culprit, which stays identifiable, and the valid shunt keeps its values.
     */
    @Test
    void invalidValuePoisonsTotalToNan() throws IOException {
        ShuntCompensatorNetworkOutput output = readRoundingOutputs("/mock_outputs/reactiveopf_results_shunts_rounding_invalid.csv");
        assertTrue(Double.isNaN(output.getTotalAbsoluteReactiveDeviation()));
        assertTrue(Double.isNaN(output.getContinuousSusceptanceByShunt().get("SHUNT")));
        assertTrue(Double.isNaN(output.getReactiveDeviationByShunt().get("SHUNT")));
        assertEquals(3.6 * AmplConstants.SB / (NOMINAL_V * NOMINAL_V), output.getContinuousSusceptanceByShunt().get("SHUNT2"), 1e-9);
        assertEquals(120, output.getReactiveDeviationByShunt().get("SHUNT2"), 1e-6);
    }

    private ShuntCompensatorNetworkOutput readRoundingOutputs(String resourceName) throws IOException {
        Network network = createWithTwoShuntCompensators();
        ShuntCompensatorNetworkOutput output = new ShuntCompensatorNetworkOutput(network, 0);
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        mapper.newInt(AmplSubset.SHUNT, "SHUNT");
        mapper.newInt(AmplSubset.SHUNT, "SHUNT2");
        mapper.newInt(AmplSubset.BUS, "b1");
        try (InputStream input = getClass().getResourceAsStream(resourceName);
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            output.read(reader, mapper);
        }
        return output;
    }
}
