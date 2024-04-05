/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class MeasurementResidualsOutput extends AbstractStateEstimatorEstimateOutput {

    public static final int EXPECTED_NB_COLS = 2;
    private static final String SEP = ";";
    public static final int COL_MEASUREMENT_NUMBER = 0;
    public static final int COL_RESIDUAL_VALUE = 1;

    private final Map<Integer, String> measurementResiduals = new HashMap<>();

    public Map<Integer, String> getMeasurementResiduals() {
        return measurementResiduals;
    }

    @Override
    public String getFileName() {
        return "se_measure_residuals.csv";
    }

    @Override
    public void read(BufferedReader bufferedReader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Consume header
        String headers = bufferedReader.readLine();
        // Verify number of columns
        int readNbCols = headers.split(SEP).length;
        if (readNbCols != EXPECTED_NB_COLS) {
            throw new PowsyblException("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_NB_COLS + ", found:" + readNbCols);
        }
        // Read lines
        bufferedReader.lines().forEach(line -> {
            if (!COMMENTED_LINE_TEST.test(line)) {
                readLine(line.split(SEP), stringToIntMapper);
            }
        });
    }

    void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {
        Integer measurementNumber = Integer.parseInt(tokens[COL_MEASUREMENT_NUMBER]);
        String residualValue = tokens[COL_RESIDUAL_VALUE];
        measurementResiduals.put(measurementNumber, residualValue);
    }

}
