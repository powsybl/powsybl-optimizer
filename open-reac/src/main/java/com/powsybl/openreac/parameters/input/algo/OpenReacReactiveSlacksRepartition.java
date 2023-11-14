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
public enum OpenReacReactiveSlacksRepartition {
    EMPTY_BUSES(0),

    LOAD_BUSES(1),

    ALL_BUSES(2);

    private static final String SLACK_REPARTITION_KEY = "reactive_slacks_repartition";

    private final int amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the slacks repartition.
     */
    OpenReacReactiveSlacksRepartition(int amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(SLACK_REPARTITION_KEY, Integer.toString(amplKey));
    }

}
