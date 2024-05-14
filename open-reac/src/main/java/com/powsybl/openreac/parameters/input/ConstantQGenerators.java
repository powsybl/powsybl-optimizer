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
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 *
 * List of generators that are not regulating voltage.
 * timestep num bus id
 */
public class ConstantQGenerators extends AbstractElementsInput {

    public static final String PARAM_GENERATOR_FILE_NAME = "param_generators_reactive.txt";

    public ConstantQGenerators(List<String> elementIds) {
        super(elementIds);
    }

    @Override
    public String getFileName() {
        return PARAM_GENERATOR_FILE_NAME;
    }

    @Override
    AmplSubset getElementAmplSubset() {
        return AmplSubset.GENERATOR;
    }
}
