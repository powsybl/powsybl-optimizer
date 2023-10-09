/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

public class VoltageLimitOverrideBuilder {

    private VoltageLimitOverride.OverrideKind lowLimitKind;
    private VoltageLimitOverride.OverrideKind highLimitKind;

    private double lowLimitOverride;
    private double highLimitOverride;

    public VoltageLimitOverride build() {
        if (lowLimitKind == null && highLimitKind == null) {
            throw new IllegalStateException("For a valid voltage limit override, the kind of one side must be provided.");
        }
        if (lowLimitOverride == 0 && highLimitOverride == 0) {
            throw new IllegalStateException("For a valid voltage limit override, at least one value must be provided.");
        }
        if (Double.isNaN(lowLimitOverride) || Double.isNaN(highLimitOverride)){
            throw new IllegalStateException("For a valid voltage limit override, no undefined value must be provided.");
        }
        return new VoltageLimitOverride(lowLimitKind, highLimitKind, lowLimitOverride, highLimitOverride);
    }

    public VoltageLimitOverrideBuilder withLowLimitKind(VoltageLimitOverride.OverrideKind lowVoltageLimitKind) {
        this.lowLimitKind = lowVoltageLimitKind;
        return this;
    }

    public VoltageLimitOverrideBuilder withHighLimitKind(VoltageLimitOverride.OverrideKind highVoltageLimitKind) {
        this.highLimitKind = highVoltageLimitKind;
        return this;
    }

    public VoltageLimitOverrideBuilder withLowLimitOverride(double lowVoltageLimitOverride) {
        this.lowLimitOverride = lowVoltageLimitOverride;
        return this;
    }

    public VoltageLimitOverrideBuilder withHighLimitOverride(double highVoltageLimitOverride) {
        this.highLimitOverride = highVoltageLimitOverride;
        return this;
    }

}
