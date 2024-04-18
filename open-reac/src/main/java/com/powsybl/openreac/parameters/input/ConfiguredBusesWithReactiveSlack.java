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
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
public class ConfiguredBusesWithReactiveSlack extends AbstractElementsInput {
    public static final String PARAM_BUSES_FILE_NAME = "param_buses_with_reactive_slack.txt";

    public ConfiguredBusesWithReactiveSlack(List<String> elementIds) {
        super(elementIds);
    }

    @Override
    public String getFileName() {
        return PARAM_BUSES_FILE_NAME;
    }

    @Override
    AmplSubset getElementAmplSubset() {
        return AmplSubset.BUS;
    }
}
