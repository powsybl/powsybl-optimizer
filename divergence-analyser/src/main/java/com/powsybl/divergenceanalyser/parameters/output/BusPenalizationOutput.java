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
import com.powsybl.divergenceanalyser.parameters.output.modifications.BusPenalization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class BusPenalizationOutput extends AbstractDivergenceAnalyserOutput {
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

        String id = amplMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[NUM_BUS_COL]));
        double newTargetV = readDouble(tokens[NEW_TARGET_V_COL]);
        double slackTargetV = readDouble(tokens[SLACK_TARGET_V_COL]);
        penalization.add(new BusPenalization(id, slackTargetV, newTargetV));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }
}