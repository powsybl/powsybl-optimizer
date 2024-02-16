/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.divergenceanalyser.parameters.input.PenalizationOptions;
import com.powsybl.divergenceanalyser.parameters.input.SolvingOptions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class ExportOptionsTest {

    /**
     * Verify the export of the solving options.
     */
    @Test
    void testSolvingOptionsExport() throws IOException {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> solvingOptions = parameters.getSolvingOptions();

        // Update the divergence analysis parameters
        parameters.setResolutionMPEC();
        parameters.setMaxTimeSolving(17);

        // Get file content of solving_options.txt
        SolvingOptions options = new SolvingOptions(solvingOptions);
        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            options.write(writer, new StringToIntMapper<>(AmplSubset.class));
            String data = w.toString();
            String ref = String.join(System.lineSeparator(), "max_time_solving 17", "solving_mode 2")
                    + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    /**
     * Verify the export of the penalization options.
     */
    @Test
    void testPenalizationOptionsExport() throws IOException {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalizationOptions = parameters.getPenalizationOptions();

        // Update the divergence analysis parameters
        parameters.setB1Penal(true)
                .setYPenal(true)
                .setXiPenal(true)
                .setTargetVUnitsPenal(true);

        // Get file content of solving_options.txt
        PenalizationOptions options = new PenalizationOptions(penalizationOptions);
        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            options.write(writer, new StringToIntMapper<>(AmplSubset.class));
            String data = w.toString();
            String ref = String.join(System.lineSeparator(), "g_shunt_2 0", "b_shunt_1 1", "xi 1", "g_shunt_1 0",
                    "b_shunt_2 0", "target_v_units 1", "target_v_svc 0", "admittance 1", "phase_shift 0", "rho_transformer 0")
                    + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

}
