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
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class VoltageLimitOverride {

    public enum OverrideKind {
        ABSOLUTE, RELATIVE;
    }

    private final OverrideKind lowLimitKind;
    private final OverrideKind highLimitKind;

    private final double lowLimitOverride;
    private final double highLimitOverride;

    public double getLowLimitOverride() {
        return lowLimitOverride;
    }

    public double getHighLimitOverride() {
        return highLimitOverride;
    }

    public OverrideKind getLowLimitKind() {
        return lowLimitKind;
    }

    public OverrideKind getHighLimitKind() {
        return highLimitKind;
    }

    public VoltageLimitOverride(OverrideKind lowLimitKind, OverrideKind highLimitKind,
                                double lowLimitOverride, double highLimitOverride) {
        this.lowLimitKind = lowLimitKind;
        this.highLimitKind = highLimitKind;
        this.lowLimitOverride = lowLimitOverride;
        this.highLimitOverride = highLimitOverride;
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
        return that.lowLimitKind == lowLimitKind
                && Double.compare(that.lowLimitOverride, lowLimitOverride) == 0
                && that.highLimitKind == highLimitKind
                && Double.compare(that.highLimitOverride, highLimitOverride) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowLimitKind, lowLimitOverride, highLimitKind, highLimitOverride);
    }
}
