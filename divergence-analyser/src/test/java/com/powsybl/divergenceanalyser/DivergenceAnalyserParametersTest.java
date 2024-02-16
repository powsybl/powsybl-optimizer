/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class DivergenceAnalyserParametersTest {

    @Test
    void testDefaultValues() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        assertTrue(parameters.getPenalizationOptions().values()
                .stream()
                .allMatch(value -> value == 0));
        assertEquals(120, parameters.getSolvingOptions().get("max_time_solving"));
        assertEquals(0, parameters.getSolvingOptions().get("solving_mode"));
    }

    @Test
    void testSolvingOptions() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> solvingOptions = parameters.getSolvingOptions();

        // update of solving_mode
        parameters.setResolutionNlp();
        assertEquals(solvingOptions.get("solving_mode"), 1);
        parameters.setResolutionMPEC();
        assertEquals(solvingOptions.get("solving_mode"), 2);
        parameters.setResolutionMinlp();
        assertEquals(solvingOptions.get("solving_mode"), 0);

        // wrong values
        assertThrows(IllegalArgumentException.class, () -> parameters.setSolvingMode(4));
        assertThrows(IllegalArgumentException.class, () -> parameters.setSolvingMode(-1));

        // update of max_time_solving
        parameters.setMaxTimeSolving(68);
        assertEquals(solvingOptions.get("max_time_solving"), 68);

        // wrong values
        assertThrows(IllegalArgumentException.class, () -> parameters.setMaxTimeSolving(0));
        assertThrows(IllegalArgumentException.class, () -> parameters.setMaxTimeSolving(-5));
    }

    @Test
    void testAllPenalization() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        // every penalization is activated
        parameters.setAllPenalization(true);
        assertTrue(parameters.getPenalizationOptions().values()
                .stream()
                .allMatch(value -> value == 1));

        // no penalization is activated
        parameters.setAllPenalization(false);
        assertTrue(parameters.getPenalizationOptions().values()
                .stream()
                .allMatch(value -> value == 0));
    }

    /**
     * Verify a penalization is removed if user asks it.
     */
    @Test
    void testRemovePenalizationOptions() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalization = parameters.getPenalizationOptions();

        parameters.setAllPenalization(true);
        assertEquals(penalization.size(), 10);
        assertFalse(penalization.containsValue(0));

        parameters.setTargetVUnitsPenal(false);
        assertEquals(penalization.get("target_v_units"), 0);

        parameters.setTargetVSvcPenal(false);
        assertEquals(penalization.get("target_v_svc"), 0);

        parameters.setYPenal(false);
        assertEquals(penalization.get("admittance"), 0);

        parameters.setXiPenal(false);
        assertEquals(penalization.get("xi"), 0);

        parameters.setRhoTransformerPenal(false);
        assertEquals(penalization.get("rho_transformer"), 0);

        parameters.setAlphaPSTPenal(false);
        assertEquals(penalization.get("phase_shift"), 0);

        parameters.setG1Penal(false);
        assertEquals(penalization.get("g_shunt_1"), 0);

        parameters.setB1Penal(false);
        assertEquals(penalization.get("b_shunt_1"), 0);

        parameters.setG2Penal(false);
        assertEquals(penalization.get("g_shunt_2"), 0);

        parameters.setB2Penal(false);
        assertEquals(penalization.get("b_shunt_2"), 0);

        assertEquals(penalization.size(), 10);
        assertFalse(penalization.containsValue(1));
    }

    /**
     * Verify a penalization is added if user asks it.
     */
    @Test
    void testAddPenalizationOptions() {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalization = parameters.getPenalizationOptions();

        assertEquals(penalization.size(), 10);
        assertFalse(penalization.containsValue(1));

        parameters.setTargetVUnitsPenal(true);
        assertEquals(penalization.get("target_v_units"), 1);

        parameters.setTargetVSvcPenal(true);
        assertEquals(penalization.get("target_v_svc"), 1);

        parameters.setYPenal(true);
        assertEquals(penalization.get("admittance"), 1);

        parameters.setXiPenal(true);
        assertEquals(penalization.get("xi"), 1);

        parameters.setRhoTransformerPenal(true);
        assertEquals(penalization.get("rho_transformer"), 1);

        parameters.setAlphaPSTPenal(true);
        assertEquals(penalization.get("phase_shift"), 1);

        parameters.setG1Penal(true);
        assertEquals(penalization.get("g_shunt_1"), 1);

        parameters.setB1Penal(true);
        assertEquals(penalization.get("b_shunt_1"), 1);

        parameters.setG2Penal(true);
        assertEquals(penalization.get("g_shunt_2"), 1);

        parameters.setB2Penal(true);
        assertEquals(penalization.get("b_shunt_2"), 1);

        assertEquals(penalization.size(), 10);
        assertFalse(penalization.containsValue(0));
    }

}
