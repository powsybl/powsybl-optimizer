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
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
        InputStream solvingOptionsFileContent = options.getParameterFileAsStream(new StringToIntMapper<>(AmplSubset.class));

        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Check the file content
        System.out.print(IOUtils.toString(solvingOptionsFileContent, StandardCharsets.UTF_8));
        assertEquals("max_time_solving 17\nsolving_mode 2\n", outputStreamCaptor.toString());
    }

    /**
     * Verify the export of the penalization options.
     */
    @Test
    void testPenalizationOptionsExport() throws IOException {
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        HashMap<String, Integer> penalizationOptions = parameters.getPenalizationOptions();

        // Update the divergence analysis parameters
        parameters.setB1Penal(true).setYPenal(true).setXiPenal(true).setTargetVUnitsPenal(true);

        // Get file content of solving_options.txt
        PenalizationOptions options = new PenalizationOptions(penalizationOptions);
        InputStream penalizationOptionsFileContent = options.getParameterFileAsStream(new StringToIntMapper<>(AmplSubset.class));

        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Check the file content
        System.out.print(IOUtils.toString(penalizationOptionsFileContent, StandardCharsets.UTF_8));
        String expected = "g_shunt_2 0\nb_shunt_1 1\nxi 1\ng_shunt_1 0\nb_shunt_2 0\ntarget_v_units 1\n"
                + "target_v_svc 0\nadmittance 1\nphase_shift 0\nrho_transformer 0\n";
        assertEquals(expected, outputStreamCaptor.toString());
    }

}
