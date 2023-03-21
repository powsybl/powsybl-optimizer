/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplException;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class ReactiveInvestmentOutput implements AmplOutputFile {
    // TODO determine sign for self investments
    private static final boolean IS_SELF_POSITIVE = true;
    private static final int SELF_FACTOR = IS_SELF_POSITIVE ? 1 : -1;
    private static final int CAPACITOR_FACTOR = !IS_SELF_POSITIVE ? 1 : -1;
    private final List<ReactiveInvestment> investments;

    public ReactiveInvestmentOutput() {
        this.investments = new LinkedList<>();
    }

    public List<ReactiveInvestment> getInvestments() {
        return investments;
    }

    @Override
    public String getFileName() {
        return "reactiveopf_results_reactive_slacks.csv";
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> amplMapper) throws IOException {
        List<String> investmentsLines;
        try {
            investmentsLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            // FIXME investements file does not exist if there is none.
            return;
        }
        String headers = investmentsLines.get(0);
        int expectedCols = 6;
        String sep = ";";
        int readCols = headers.split(sep).length;
        if (readCols != expectedCols) {
            throw new PowsyblException(
                    "Error reading " + getFileName() + ", wrong number of columns. Expected: " + expectedCols + ", found:" + readCols);
        } else {
            for (String line : investmentsLines.subList(1, investmentsLines.size())) {
                readLine(line.split(sep));
            }
        }
    }

    private void readLine(String[] tokens) {
        double slackCapacitor = CAPACITOR_FACTOR * readDouble(tokens[2]);
        double slackSelf = SELF_FACTOR * readDouble(tokens[3]);
        String id = readString(tokens[4]);
        String substationId = readString(tokens[5]);
        double slack = slackCapacitor + slackSelf;
        if (slack != slackCapacitor && slack != slackSelf) {
            throw new AmplException(
                    "Error reading reactive investments, can't be self and capacitor at the same time.");
        }
        investments.add(new ReactiveInvestment(id, substationId, slack));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

    /**
     * removes quotes on strings
     */
    private String readString(String str) {
        return str.substring(1, str.length() - 1);
    }

    public static class ReactiveInvestment {
        public final String busId;
        public final String substationId;
        public final double slack;

        public ReactiveInvestment(String busId, String substationId, double slack) {
            this.busId = busId;
            this.substationId = substationId;
            this.slack = slack;
        }
    }
}
