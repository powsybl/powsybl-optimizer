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
import com.powsybl.stateestimator.parameters.output.estimates.BranchStatusEstimate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class NetworkTopologyEstimateOutput extends AbstractStateEstimatorEstimateOutput {

    public static final int EXPECTED_NB_COLS = 6;
    private static final String SEP = ";";

    public static final int COL_BRANCH_ID = 3;
    public static final int COL_ESTIMATED_STATUS = 5;
    public static final String VALID_CLOSED_STATUS = "\'CLOSED\'";
    public static final String VALID_OPENED_STATUS = "\'OPENED\'";

    private final List<BranchStatusEstimate> networkTopologyEstimate = new ArrayList<>();

    public List<BranchStatusEstimate> getNetworkTopologyEstimate() {
        return networkTopologyEstimate;
    }

    @Override
    public String getFileName() {
        return "se_topology_estimate.csv";
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
        if (tokens[COL_ESTIMATED_STATUS].equals(VALID_CLOSED_STATUS) || tokens[COL_ESTIMATED_STATUS].equals(VALID_OPENED_STATUS)) {
            String branchId = tokens[COL_BRANCH_ID].replaceAll("'", "");
            String estimatedStatus = tokens[COL_ESTIMATED_STATUS].replaceAll("'", "");
            networkTopologyEstimate.add(new BranchStatusEstimate(branchId, estimatedStatus));
        }
        else {
            throw new PowsyblException("Status indicated for the branch is not valid. Only 'CLOSED' or 'OPENED' are accepted.");
        }

    }
}
