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
public class MeasurementEstimatesAndResidualsOutput extends AbstractStateEstimatorEstimateOutput {

    public static final int EXPECTED_NB_COLS = 3;
    private static final String SEP = ";";
    public static final int COL_MEASUREMENT_NUMBER = 0;
    public static final int COL_MEASUREMENT_ESTIMATE = 1;
    public static final int COL_MEASUREMENT_RESIDUAL = 2;

    private final Map<Integer, ArrayList<String>> measurementEstimatesAndResiduals = new HashMap<>();

    public Map<Integer, ArrayList<String>> getMeasurementEstimatesAndResiduals() {
        return measurementEstimatesAndResiduals;
    }

    @Override
    public String getFileName() {
        return "se_measurements_estimates_and_residuals.csv";
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
        ArrayList<String> val = new ArrayList<>();
        val.add(tokens[COL_MEASUREMENT_ESTIMATE]);
        val.add(tokens[COL_MEASUREMENT_RESIDUAL]);
        measurementEstimatesAndResiduals.put(measurementNumber, val);
    }

}
