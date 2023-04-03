/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

/**
 * Must be used with {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE}
 * to define the voltage ratio relative to nominal voltage, which OpenReac should converge to.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OptimisationVoltageRatio implements OpenReacAlgoParam {
    private static final String RATIO_OBJECTIVE_VOLTAGE_KEY = "ratio_voltage_target";
    private final double ratio;

    public OptimisationVoltageRatio(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public String getName() {
        return RATIO_OBJECTIVE_VOLTAGE_KEY;
    }

    @Override
    public String getParamValue() {
        return Double.toString(ratio);
    }
}
