/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BranchPenalizationOutput extends AbstractDivergenceAnalyserOutput {

    // The order of parameters is rho, Y, alpha, Xi, g1, b1, g2, b2
    public static final int RHO_PLACE = 0;
    public static final int Y_PLACE = 1;
    public static final int ALPHA_PLACE = 2;
    public static final int XI_PLACE = 3;
    public static final int G1_PLACE = 4;
    public static final int B1_PLACE = 5;
    public static final int G2_PLACE = 6;
    public static final int B2_PLACE = 7;

    public static final int NEW_VAL_FIRST_COL = 3;
    public static final int SLACK_FIRST_COL = 11;

    public static final int EXPECTED_COLS = 28;
    private static final String SEP = ";";

    private final List<BranchPenalization> penalization = new ArrayList<>();

    public List<BranchPenalization> getPenalization() {
        return penalization;
    }

    @Override
    public String getFileName() {
        return "da_branch_penal.csv";
    }

    @Override
    public void read(BufferedReader bufferedReader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        String headers = bufferedReader.readLine(); // consume header

        // Verify number of columns
        int readCols = headers.split(SEP).length;
        if (readCols != EXPECTED_COLS) {
            throw new PowsyblException("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
        }

        // read lines
        bufferedReader.lines().forEach(line -> {
            readLine(line.split(SEP), stringToIntMapper);
            if (!COMMENTED_LINE_TEST.test(line)) {
                readLine(line.split(SEP), stringToIntMapper);
            }
        });
    }

    void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {

        String id = amplMapper.getId(AmplSubset.BRANCH, Integer.parseInt(tokens[0]));

        double rho = readDouble(tokens[NEW_VAL_FIRST_COL + RHO_PLACE]);
        double y = readDouble(tokens[NEW_VAL_FIRST_COL + Y_PLACE]);
        double alpha = readDouble(tokens[NEW_VAL_FIRST_COL + ALPHA_PLACE]);
        double xi = readDouble(tokens[NEW_VAL_FIRST_COL + XI_PLACE]);
        double g1 = readDouble(tokens[NEW_VAL_FIRST_COL + G1_PLACE]);
        double b1 = readDouble(tokens[NEW_VAL_FIRST_COL + B1_PLACE]);
        double g2 = readDouble(tokens[NEW_VAL_FIRST_COL + G2_PLACE]);
        double b2 = readDouble(tokens[NEW_VAL_FIRST_COL + B2_PLACE]);

        double slackRho = readDouble(tokens[SLACK_FIRST_COL + RHO_PLACE]);
        double slackY = readDouble(tokens[SLACK_FIRST_COL + Y_PLACE]);
        double slackAlpha = readDouble(tokens[SLACK_FIRST_COL + ALPHA_PLACE]);
        double slackXi = readDouble(tokens[SLACK_FIRST_COL + XI_PLACE]);
        double slackG1 = readDouble(tokens[SLACK_FIRST_COL + G1_PLACE]);
        double slackB1 = readDouble(tokens[SLACK_FIRST_COL + B1_PLACE]);
        double slackG2 = readDouble(tokens[SLACK_FIRST_COL + G2_PLACE]);
        double slackB2 = readDouble(tokens[SLACK_FIRST_COL + B2_PLACE]);

        BranchPenalization branchPenalization = new BranchPenalization(id, slackRho, slackY, slackAlpha, slackXi, slackG1, slackB1, slackG2, slackB2, // Slacks
                rho, y, alpha, xi, g1, b1, g2, b2);
        penalization.add(branchPenalization);

    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
