/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class DivergenceAnalyserConfig {
    /**
     * Default parameters
     */

    // For debug
    private static final boolean DEFAULT_DEBUG = true;

    private final boolean debug;

    public DivergenceAnalyserConfig(boolean debug) {
        this.debug = debug;
    }

    public static DivergenceAnalyserConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static DivergenceAnalyserConfig load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig("divergenceanalyser")
                .map(config -> new DivergenceAnalyserConfig(config.getBooleanProperty("debug", DEFAULT_DEBUG)))
                .orElse(new DivergenceAnalyserConfig(false));
    }

    public boolean isDebug() {
        return debug;
    }

}
