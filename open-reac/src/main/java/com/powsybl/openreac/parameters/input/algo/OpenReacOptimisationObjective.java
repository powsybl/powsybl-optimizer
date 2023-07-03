/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

import com.powsybl.openreac.parameters.input.OpenReacParameters;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public enum OpenReacOptimisationObjective {

    /**
     * Use this to minimizes the global generation
     */
    MIN_GENERATION(0),
    /**
     * Use this to target a voltage profile between the low voltage limit
     * and high voltage limit.
     * @see OpenReacParameters#setObjectiveDistance
     */
    BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT(1),
    /**
     * Use this to target a specific voltage profile
     */
    SPECIFIC_VOLTAGE_PROFILE(2);

    private static final String OBJECTIVE_PARAM_KEY = "objective_choice";

    private final int amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the given objective.
     */
    OpenReacOptimisationObjective(int amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(OBJECTIVE_PARAM_KEY, Integer.toString(amplKey));
    }
}
