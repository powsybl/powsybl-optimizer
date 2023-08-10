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
import com.powsybl.divergenceanalyser.parameters.output.modifications.BusPenalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BusPenalizationOutput implements AmplOutputFile {

    public static final int NUM_BUS_COL = 0;
    public static final int NEW_TARGET_V_COL = 1;
    public static final int SLACK_TARGET_V_COL = 2;
    public static final int EXPECTED_COLS = 5;
    private static final String SEP = ";";

    private final List<BusPenalization> penalization = new ArrayList<>();

    public List<BusPenalization> getPenalization() {
        return penalization;
    }

    @Override
    public String getFileName() {
        return "da_bus_penal.csv";
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

        String id = amplMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[NUM_BUS_COL]));
        double newTargetV = readDouble(tokens[NEW_TARGET_V_COL]);
        double slackTargetV = readDouble(tokens[SLACK_TARGET_V_COL]);

        penalization.add(new BusPenalization(id, slackTargetV, newTargetV));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }
}
