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
public enum OpenReacSolverLogLevel {

    NOTHING(0),

    ONLY_RESULTS(1),

    EVERYTHING(2);

    private static final String LOG_SOLVER_PARAM_KEY = "log_level_knitro";

    private final int amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the solver log level.
     */
    OpenReacSolverLogLevel(int amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(LOG_SOLVER_PARAM_KEY, Integer.toString(amplKey));
    }

}
