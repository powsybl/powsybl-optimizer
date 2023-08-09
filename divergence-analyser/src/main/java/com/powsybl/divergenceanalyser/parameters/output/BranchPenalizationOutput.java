/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.divergenceanalyser.parameters.output.modifications.BranchPenalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BranchPenalizationOutput implements AmplOutputFile {

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
    public void read(Path outputPath, StringToIntMapper<AmplSubset> networkAmplMapper) {
        List<String> outputLines;

        if (Files.isRegularFile(outputPath)) {
            try {
                outputLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // File reading went wrong
                return;
            }

            String headers = outputLines.get(0);
            int readCols = headers.split(SEP).length;
            if (readCols != EXPECTED_COLS) {
                try {
                    throw new Exception("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                for (String line : outputLines.subList(1, outputLines.size())) {
                    readLine(line.split(SEP), networkAmplMapper);
                }
            }
        }
    }

    private void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {

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

        penalization.add(
                new BranchPenalization(id, slackRho, slackY, slackAlpha, slackXi, slackG1, slackB1, slackG2, slackB2, // Slacks
                rho, y, alpha, xi, g1, b1, g2, b2)); // New values

    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
