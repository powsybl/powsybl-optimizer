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

    public enum VoltageLimitType {
        HIGH_VOLTAGE_LIMIT,
        LOW_VOLTAGE_LIMIT;

        @Override
        public String toString() {
            return this == HIGH_VOLTAGE_LIMIT ? "HIGH_VOLTAGE_LIMIT" : "LOW_VOLTAGE_LIMIT";
        }
    }

    private final VoltageLimitType type; // indicates if the override is done on low or on high voltage limit

    private final boolean isRelative; // if true, override is relative. if false, override is absolute

    private final double limit; // value of the limit override

    private final String voltageLevelId;

    public VoltageLimitType getVoltageLimitType() {
        return type;
    }

    public boolean isRelative() {
        return isRelative;
    }

    public double getLimit() {
        return limit;
    }

    public String getVoltageLevelId() {
        return voltageLevelId;
    }

    public VoltageLimitOverride(String voltageLevelId, VoltageLimitType type, Boolean isRelative, double limit) {
        if (isRelative == null) {
            throw new InvalidParametersException("The kind of voltage limit override must be specified.");
        }
        if (Double.isNaN(limit)) {
            throw new InvalidParametersException("The voltage limit override must be defined.");
        }
        if (limit <= 0 && !isRelative) {
            throw new InvalidParametersException("The voltage limit override is in absolute value: must be positive.");
        }
        this.voltageLevelId = voltageLevelId;
        this.type = Objects.requireNonNull(type);
        this.isRelative = isRelative;
        this.limit = limit;
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
        return that.voltageLevelId.equals(voltageLevelId)
                && that.type.equals(type)
                && that.isRelative == isRelative
                && Double.compare(that.limit, limit) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(voltageLevelId, type, isRelative, limit);
    }
}
