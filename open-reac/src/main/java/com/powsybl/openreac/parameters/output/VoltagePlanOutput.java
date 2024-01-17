/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class VoltagePlanOutput extends AbstractNoThrowOutput {

    private static final String ELEMENT = "voltages";
    public static final int EXPECTED_COLS = 5;
    private static final int ID_COLUMN_INDEX = 4;
    private static final int V_COLUMN_INDEX = 2;
    private static final int ANGLE_COLUMN_INDEX = 3;

    public static class BusResult {
        private final String busId;
        private final double v;
        private final double angle;

        public BusResult(String busId, double v, double angle) {
            this.busId = Objects.requireNonNull(busId);
            this.v = v;
            this.angle = angle;
        }

        public String getBusId() {
            return busId;
        }

        public double getV() {
            return v;
        }

        public double getAngle() {
            return angle;
        }
    }

    private final List<BusResult> voltagePlan = new ArrayList<>();

    public List<BusResult> getVoltagePlan() {
        return voltagePlan;
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
    protected void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = AmplIOUtils.removeQuotes(tokens[ID_COLUMN_INDEX]);
        double v = readDouble(tokens[V_COLUMN_INDEX]);
        double angle = readDouble(tokens[ANGLE_COLUMN_INDEX]);
        voltagePlan.add(new BusResult(id, v, angle));
    }

    @Override
    public boolean throwOnMissingFile() {
        triggerErrorState();
        return false;
    }

}
