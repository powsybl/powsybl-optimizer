/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

/**
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public enum OpenReacBusesWithReactiveSlackConfig {
    SPECIFIED(0),
    NO_GENERATION(1),

    ALL(2);

    private static final String BUSES_REACTIVE_SLACKS_KEY = "buses_with_reactive_slacks";

    private final int amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the slacks repartition.
     */
    OpenReacBusesWithReactiveSlackConfig(int amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(BUSES_REACTIVE_SLACKS_KEY, Integer.toString(amplKey));
    }

}
