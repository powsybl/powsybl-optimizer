/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
public enum ReactiveSlackBusesMode {
    CONFIGURED("CONFIGURED"),
    NO_GENERATION("NO_GENERATION"),
    ALL("ALL");

    private static final String REACTIVE_SLACK_BUSES_KEY = "buses_with_reactive_slacks";

    private final String amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the slacks repartition.
     */
    ReactiveSlackBusesMode(String amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(REACTIVE_SLACK_BUSES_KEY, amplKey);
    }

}
