/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import java.util.Objects;

/**
 * Class to store an override of a voltage level voltage limits.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class VoltageLimitOverride {

    private final double deltaLowVoltageLimit;
    private final double deltaHighVoltageLimit;

    public double getDeltaLowVoltageLimit() {
        return deltaLowVoltageLimit;
    }

    public double getDeltaHighVoltageLimit() {
        return deltaHighVoltageLimit;
    }

    public VoltageLimitOverride(double deltaLowVoltageLimit, double deltaHighVoltageLimit) {
        this.deltaLowVoltageLimit = deltaLowVoltageLimit;
        this.deltaHighVoltageLimit = deltaHighVoltageLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VoltageLimitOverride that = (VoltageLimitOverride) o;
        return Double.compare(that.deltaLowVoltageLimit, deltaLowVoltageLimit) == 0
                && Double.compare(that.deltaHighVoltageLimit, deltaHighVoltageLimit) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deltaLowVoltageLimit, deltaHighVoltageLimit);
    }
}
