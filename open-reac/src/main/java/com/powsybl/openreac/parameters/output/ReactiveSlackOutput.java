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
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.parameters.IncompatibleModelError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * Reactive slacks in load convention.
 */
public class ReactiveSlackOutput extends AbstractNoThrowOutput {

    public static class ReactiveSlack {
        public final String busId;
        public final String substationId; // FIXME
        public final double slack;

        public ReactiveSlack(String busId, String substationId, double slack) {
            this.busId = busId;
            this.substationId = substationId;
            this.slack = slack;
        }
    }

    private final List<ReactiveSlack> slacks;

    public ReactiveSlackOutput() {
        this.slacks = new LinkedList<>();
    }

    public List<ReactiveSlack> getSlacks() {
        return slacks;
    }

    @Override
    public String getFileName() {
        return "reactiveopf_results_reactive_slacks.csv";
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> amplMapper) {
        List<String> investmentsLines;
        try {
            investmentsLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // File reading went wrong, this can happen when the ampl crashed, or didn't converge. We must not throw to the user.
            triggerErrorState();
            return;
        }
        String headers = investmentsLines.get(0);
        int expectedCols = 6;
        String sep = ";";
        int readCols = headers.split(sep).length;
        if (readCols != expectedCols) {
            triggerErrorState();
            throw new IncompatibleModelError("Error reading " + getFileName() + ", wrong number of columns. Expected: " + expectedCols + ", found:" + readCols);
        } else {
            for (String line : investmentsLines.subList(1, investmentsLines.size())) {
                readLine(line.split(sep));
            }
        }
    }

    private void readLine(String[] tokens) {
        // slack capacitor is a generation of reactive power.
        // slack self is a reactive load.
        double slackCapacitor = -readDouble(tokens[2]);
        double slackSelf = readDouble(tokens[3]);
        String id = readString(tokens[4]);
        String substationId = readString(tokens[5]);
        double slack = slackCapacitor + slackSelf;
        if (slack != slackCapacitor && slack != slackSelf) {
            throw new AmplException("Error reading reactive slacks, can't be self and capacitor at the same time.");
        }
        slacks.add(new ReactiveSlack(id, substationId, slack));
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
}
