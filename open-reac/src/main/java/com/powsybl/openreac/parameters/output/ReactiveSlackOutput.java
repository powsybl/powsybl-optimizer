/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * Reactive slacks in load convention.
 */
public class ReactiveSlackOutput extends AbstractNoThrowOutput {
    private static final String ELEMENT = "reactive_slacks";
    public static final int EXPECTED_COLS = 6;
    private static final int BUS_ID_COLUMN_INDEX = 4;
    private static final int VOLTAGE_LEVEL_ID_COLUMN_INDEX = 5;
    private static final int REACTIVE_SLACK_GENERATION_COLUMN_INDEX = 2;
    private static final int REACTIVE_SLACK_LOAD_COLUMN_INDEX = 3;

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
    public String getElement() {
        return ELEMENT;
    }

    @Override
    public int getExpectedColumns() {
        return EXPECTED_COLS;
    }

    @Override
    public boolean throwOnMissingFile() {
        // if the file is missing, we know there is no reactive slack.
        return false;
    }

    protected void doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        // slack capacitor is a generation of reactive power.
        // slack self is a reactive load.
        String id = AmplIOUtils.removeQuotes(tokens[BUS_ID_COLUMN_INDEX]);
        String voltageLevelId = AmplIOUtils.removeQuotes(tokens[VOLTAGE_LEVEL_ID_COLUMN_INDEX]);
        double slackCapacitor = -readDouble(tokens[REACTIVE_SLACK_GENERATION_COLUMN_INDEX]);
        double slackSelf = readDouble(tokens[REACTIVE_SLACK_LOAD_COLUMN_INDEX]);
        double slack = slackCapacitor + slackSelf;
        slacks.add(new ReactiveSlack(id, voltageLevelId, slack));
    }

}
