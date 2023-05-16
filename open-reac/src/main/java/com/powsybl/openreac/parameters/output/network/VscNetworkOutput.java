/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.VscConverterStationModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VscConverterStation;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class VscNetworkOutput extends AbstractNetworkOutput<VscConverterStationModification> {
    private static final String ELEMENT = "vsc_converter_stations";
    private static final int ID_COLUMN_INDEX = 1;

    /**
     * FIXME Which columns are columns in the ampl should be used Target V or V ?
     */
    private static final int SET_POINT_V_COLUMN_INDEX = 4;
    private static final int SET_POINT_Q_COLUMN_INDEX = 5;
    private final Network network;

    public VscNetworkOutput(Network network) {
        this.network = network;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    protected VscConverterStationModification doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.VSC_CONVERTER_STATION, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        VscConverterStation vscConverterStation = network.getVscConverterStation(id);
        Double targetV = Double.parseDouble(tokens[SET_POINT_V_COLUMN_INDEX]) * vscConverterStation
            .getRegulatingTerminal()
            .getVoltageLevel()
            .getNominalV();
        Double targetQ = Double.parseDouble(tokens[SET_POINT_Q_COLUMN_INDEX]);

        if (targetQ == vscConverterStation.getReactivePowerSetpoint()) {
            targetQ = null;
        }
        if (targetV == vscConverterStation.getVoltageSetpoint()) {
            targetV = null;
        }
        return new VscConverterStationModification(id, targetV, targetQ);
    }
}
