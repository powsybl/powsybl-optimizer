/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.openreac.exceptions.InvalidParametersException;

import java.util.Objects;

/**
 * Class to store an override of a voltage level voltage limits.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class VoltageLimitOverride {

    public enum OverrideSide {
        HIGH, LOW;

        @Override
        public String toString() {
            return this == HIGH ? "HIGH" : "LOW";
        }
    }

    private final OverrideSide side; // indicates if the override is done on low/high voltage limit
    private final boolean isRelative; // if true, override is absolute. if false, override is absolute
    private final double limitOverride; // value of the override

    public OverrideSide getSide() {
        return side;
    }

    public boolean isRelative() {
        return isRelative;
    }

    public double getLimitOverride() {
        return limitOverride;
    }

    public VoltageLimitOverride(OverrideSide side, boolean isRelative, double limitOverride) {
        if (Double.isNaN(limitOverride) || limitOverride <= 0 && !isRelative) {
            throw new InvalidParametersException("The voltage limit override is incorrect. " +
                    "It must be defined and > 0 if absolute.");
        }
        this.side = side;
        this.isRelative = isRelative;
        this.limitOverride = limitOverride;
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
        return that.side == side
                && that.isRelative == isRelative
                && Double.compare(that.limitOverride, limitOverride) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(side, isRelative, limitOverride);
    }
}
