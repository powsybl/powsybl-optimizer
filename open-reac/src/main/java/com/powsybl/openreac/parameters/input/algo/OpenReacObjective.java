/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public enum OpenReacObjective implements OpenReacAlgoParam {

    MIN_GENERATION(0), BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE(1), SPECIFIC_VOLTAGE_PROFILE(2);

    private static final String OBJECTIVE_PARMA_KEY = "objective_choice";
    private final String name;
    private final int amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the given objective.
     */
    OpenReacObjective(int amplKey) {
        this.name = OBJECTIVE_PARMA_KEY;
        this.amplKey = amplKey;
    }

    public String getName() {
        return this.name;
    }
    public String getParamValue() {
        return Integer.toString(amplKey);
    }
}
