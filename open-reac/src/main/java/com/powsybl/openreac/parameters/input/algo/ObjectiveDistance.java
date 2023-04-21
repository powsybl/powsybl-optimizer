/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

/**
 * Must be used with {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT}
 * to define the voltage between low and high voltage limits, which OpenReac should converge to.
 * Zero percent means that it should converge to low voltage limits. 100 percents means that it should
 * converge to high voltage limits.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class ObjectiveDistance implements OpenReacAlgoParam {

    private static final String OBJECTIVE_DISTANCE_KEY = "ratio_voltage_target";
    private final double ratio;

    public ObjectiveDistance(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public String getName() {
        return OBJECTIVE_DISTANCE_KEY;
    }

    @Override
    public String getParamValue() {
        return Double.toString(ratio);
    }
}
