/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.exceptions.IncompatibleModelException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class VoltagePlanOutput extends AbstractNoThrowOutput {

    public static final int EXPECTED_COLS = 4;
    private static final String SEP = ";";

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
    public String getFileName() {
        return "reactiveopf_results_voltages.csv";
    }

    @Override
    public void read(BufferedReader reader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        String headers = reader.readLine();
        int readCols = headers.split(SEP).length;
        if (readCols != EXPECTED_COLS) {
            triggerErrorState();
            throw new IncompatibleModelException("Error reading " + getFileName() + ", wrong number of columns. Expected: " + EXPECTED_COLS + ", found:" + readCols);
        } else {
            String line = reader.readLine();
            while (line != null) {
                readLine(line.split(SEP), stringToIntMapper);
                line = reader.readLine();
            }
        }
    }

    @Override
    public boolean throwOnMissingFile() {
        triggerErrorState();
        return false;
    }

    private void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[1]));
        double v = readDouble(tokens[2]);
        double angle = readDouble(tokens[3]);
        voltagePlan.add(new BusResult(id, v, angle));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

}
