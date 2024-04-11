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
import com.powsybl.stateestimator.parameters.output.estimates.BranchPowersEstimate;
import com.powsybl.stateestimator.parameters.output.estimates.BusStateEstimate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class NetworkPowersEstimateOutput extends AbstractStateEstimatorEstimateOutput {

    public static final int EXPECTED_NB_COLS = 7;
    private static final String SEP = ";";
    public static final int COL_BRANCH_ID = 0;
    public static final int COL_FIRST_BUS_ID = 1;
    public static final int COL_SECOND_BUS_ID = 2;
    public static final int COL_ACTIVE_POWER_END1 = 3;
    public static final int COL_ACTIVE_POWER_END2 = 4;
    public static final int COL_REACTIVE_POWER_END1 = 5;
    public static final int COL_REACTIVE_POWER_END2 = 6;

    private final List<BranchPowersEstimate> networkPowersEstimate = new ArrayList<>();

    public List<BranchPowersEstimate> getNetworkPowersEstimate() {
        return networkPowersEstimate;
    }

    @Override
    public String getFileName() {
        return "se_network_powers_estimate.csv";
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
        String branchId = tokens[COL_BRANCH_ID].replaceAll("'", "");
        String firstBusId = tokens[COL_FIRST_BUS_ID].replaceAll("'", "");
        String secondBusId = tokens[COL_SECOND_BUS_ID].replaceAll("'", "");
        double activePowerEnd1 = readDouble(tokens[COL_ACTIVE_POWER_END1]);
        double activePowerEnd2 = readDouble(tokens[COL_ACTIVE_POWER_END2]);
        double reactivePowerEnd1 = readDouble(tokens[COL_REACTIVE_POWER_END1]);
        double reactivePowerEnd2 = readDouble(tokens[COL_REACTIVE_POWER_END2]);
        networkPowersEstimate.add(new BranchPowersEstimate(branchId, firstBusId, secondBusId,
                activePowerEnd1, activePowerEnd2, reactivePowerEnd1, reactivePowerEnd2));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
