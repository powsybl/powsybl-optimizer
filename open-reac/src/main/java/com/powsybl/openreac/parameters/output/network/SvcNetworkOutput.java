/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.StaticVarCompensatorModification;
import com.powsybl.iidm.network.Network;

import java.util.OptionalDouble;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class SvcNetworkOutput extends AbstractNetworkOutput<StaticVarCompensatorModification> {
    private static final String ELEMENT = "static_var_compensators";
    private static final int ID_COLUMN_INDEX = 1;
    private static final int SET_POINT_V_COLUMN_INDEX = 3;
    private static final int SET_POINT_Q_COLUMN_INDEX = 4;
    private final Network network;

    public SvcNetworkOutput(Network network) {
        this.network = network;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    protected StaticVarCompensatorModification doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.STATIC_VAR_COMPENSATOR, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        OptionalDouble targetV = OptionalDouble.of(Double.parseDouble(tokens[SET_POINT_V_COLUMN_INDEX]));
        OptionalDouble targetQ = OptionalDouble.of(Double.parseDouble(tokens[SET_POINT_Q_COLUMN_INDEX]));

        if (targetQ.getAsDouble() == network.getStaticVarCompensator(id).getReactivePowerSetpoint()) {
            targetQ = OptionalDouble.empty();
        }
        if (targetV.getAsDouble() == network.getStaticVarCompensator(id).getVoltageSetpoint()) {
            targetV = OptionalDouble.empty();
        }
        return new StaticVarCompensatorModification(id, targetV, targetQ);
    }
}
