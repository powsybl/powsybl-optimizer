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
import com.powsybl.openreac.exceptions.IncompatibleModelException;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * Reactive slacks in load convention.
 */
public class ReactiveSlackOutput extends AbstractNoThrowOutput {

    public static final int EXPECTED_COLS = 6;
    private static final String SEP = ";";

    public static class ReactiveSlack {
        public final String busId;
        public final String voltageLevelId;
        public final double slack;

        public ReactiveSlack(String busId, String voltageLevelId, double slack) {
            this.busId = Objects.requireNonNull(busId);
            this.voltageLevelId = Objects.requireNonNull(voltageLevelId);
            this.slack = slack;
        }

        public String getBusId() {
            return busId;
        }

        public String getVoltageLevelId() {
            return voltageLevelId;
        }

        public double getSlack() {
            return slack;
        }
    }

    private final List<ReactiveSlack> slacks = new ArrayList<>();

    public List<ReactiveSlack> getSlacks() {
        return slacks;
    }

    @Override
    public String getFileName() {
        return "reactiveopf_results_reactive_slacks.csv";
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> amplMapper) {
        List<String> reactiveSlackLines;
        // if the file is missing, we know there is no reactive slack.
        if (Files.isRegularFile(path)) {
            try {
                reactiveSlackLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // File reading went wrong
                triggerErrorState();
                return;
            }
            String headers = reactiveSlackLines.get(0);
            int readCols = headers.split(SEP).length;
            if (readCols != EXPECTED_COLS) {
                triggerErrorState();
                throw new IncompatibleModelException("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
            } else {
                for (String line : reactiveSlackLines.subList(1, reactiveSlackLines.size())) {
                    readLine(line.split(SEP));
                }
            }
        }
    }

    private void readLine(String[] tokens) {
        // slack capacitor is a generation of reactive power.
        // slack self is a reactive load.
        double slackCapacitor = -readDouble(tokens[2]);
        double slackSelf = readDouble(tokens[3]);
        String id = AmplIOUtils.removeQuotes(tokens[4]);
        String voltageLevelId = AmplIOUtils.removeQuotes(tokens[5]);
        double slack = slackCapacitor + slackSelf;
        if (slack != slackCapacitor && slack != slackSelf) {
            throw new AmplException("Error reading reactive slacks, can't be self and capacitor at the same time.");
        }
        slacks.add(new ReactiveSlack(id, voltageLevelId, slack));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
