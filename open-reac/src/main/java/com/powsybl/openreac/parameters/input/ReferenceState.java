/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

/**
 * State of the controls the ACOPF starts from and, for those carrying a penalty in the objective function, is pulled
 * back to: voltage magnitudes, ratios of the variable transformers, sections of the variable shunt compensators and
 * reactive power of the VSC converter stations. The voltage magnitudes of this state are thus the
 * target of the objective {@link com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective#SPECIFIC_VOLTAGE_PROFILE},
 * in place of the profile of the network.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public enum ReferenceState {
    /**
     * The state of the network, which may be the result of a previous optimization.
     */
    NETWORK,
    /**
     * Voltage magnitudes at 1 pu, transformers at their neutral tap, linear shunt compensators disconnected, VSC
     * converter stations at the middle of their reactive power range, as generators and batteries are in the ACOPF.
     * Non linear shunt compensators are left as they are.
     */
    NEUTRAL
}
