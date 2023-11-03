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
public enum OpenReacAmplLogLevel {

    DEBUG("DEBUG"),

    INFO("INFO"),

    WARNING("WARNING"),

    ERROR("ERROR");

    private static final String LOG_AMPL_PARAM_KEY = "log_level_ampl";

    private final String amplKey;

    /**
     * @param amplKey value used in param_algo.txt to define the ampl log level.
     */
    OpenReacAmplLogLevel(String amplKey) {
        this.amplKey = amplKey;
    }

    public OpenReacAlgoParam toParam() {
        return new OpenReacAlgoParamImpl(LOG_AMPL_PARAM_KEY, amplKey);
    }
}
