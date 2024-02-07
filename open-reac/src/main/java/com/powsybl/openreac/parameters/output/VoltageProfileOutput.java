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
import org.jgrapht.alg.util.Pair;

import java.util.*;

/**
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class VoltageProfileOutput extends AbstractNoThrowOutput {

    private static final String ELEMENT = "voltages";
    public static final int EXPECTED_COLS = 5;
    private static final int ID_COLUMN_INDEX = 4;
    private static final int V_COLUMN_INDEX = 2;
    private static final int ANGLE_COLUMN_INDEX = 3;

    private final Map<String, Pair<Double, Double>> voltageProfile = new HashMap<>();

    public Map<String, Pair<Double, Double>> getVoltageProfile() {
        return voltageProfile;
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
        voltageProfile.put(id, Pair.of(v, angle));
    }

    @Override
    public boolean throwOnMissingFile() {
        triggerErrorState();
        return false;
    }

}
