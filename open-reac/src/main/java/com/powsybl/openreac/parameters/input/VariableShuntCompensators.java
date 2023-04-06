/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;

import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 *
 * List of shunts which section that can be modified by OpenReac.
 * timestep num bus id
 */
public class VariableShuntCompensators extends AbstractElementsInput {

    public static final String PARAM_SHUNT_FILE_NAME = "param_shunts.txt";

    public VariableShuntCompensators(List<String> elementIds) {
        super(elementIds);
    }

    @Override
    AmplSubset getElementAmplSubset() {
        return AmplSubset.SHUNT;
    }

    @Override
    public String getFileName() {
        return PARAM_SHUNT_FILE_NAME;
    }

}
