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
import java.util.List;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateVectorEstimateOutput extends AbstractStateEstimatorEstimateOutput {

    public static final int EXPECTED_NB_COLS = 4;
    private static final String SEP = ";";
    public static final int COL_BUS_ID = 1;
    public static final int COL_V_ESTIMATED = 2;
    public static final int COL_THETA_ESTIMATED = 3;

    private final List<BusStateEstimate> stateVectorEstimate = new ArrayList<>();

    public List<BusStateEstimate> getStateVectorEstimate() {
        return stateVectorEstimate;
    }

    @Override
    public String getFileName() {
        return "se_state_estimate.csv";
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
        String busId = tokens[COL_BUS_ID].replaceAll("'", "");
        double V = readDouble(tokens[COL_V_ESTIMATED]);
        double theta = readDouble(tokens[COL_THETA_ESTIMATED]);
        stateVectorEstimate.add(new BusStateEstimate(busId, V, theta));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
