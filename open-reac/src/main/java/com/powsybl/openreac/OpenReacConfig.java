/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class OpenReacConfig {

    private static final boolean DEFAULT_DEBUG = false;

    private final boolean debug;

    public OpenReacConfig(boolean debug) {
        this.debug = debug;
    }

    public static OpenReacConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static OpenReacConfig load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig("open-reac")
                .map(config -> new OpenReacConfig(config.getBooleanProperty("debug", DEFAULT_DEBUG)))
                .orElse(new OpenReacConfig(false));
    }

    public boolean isDebug() {
        return debug;
    }
}
