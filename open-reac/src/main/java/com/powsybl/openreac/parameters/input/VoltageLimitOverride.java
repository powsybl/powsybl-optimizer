/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

/**
 * Business class to store an override of a Voltage level.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class VoltageLimitOverride {
    private final double lowerVoltageLimit;
    private final double upperVoltageLimit;

    public double getLowerVoltageLimit() {
        return lowerVoltageLimit;
    }

    public double getUpperVoltageLimit() {
        return upperVoltageLimit;
    }

    public VoltageLimitOverride(double lowerVoltageLimit, double upperVoltageLimit) {
        this.lowerVoltageLimit = lowerVoltageLimit;
        this.upperVoltageLimit = upperVoltageLimit;
    }
}
