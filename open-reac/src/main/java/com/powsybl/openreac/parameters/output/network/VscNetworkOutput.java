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
    public static final int EXPECTED_COLS = 8;
    private static final int ID_COLUMN_INDEX = 1;
    private static final int SET_POINT_V_COLUMN_INDEX = 4;
    private static final int SET_POINT_Q_COLUMN_INDEX = 5;
    private final Network network;

    public VscNetworkOutput(Network network) {
        super(network);
        this.network = network;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    public int getExpectedColumns() {
        return EXPECTED_COLS;
    }

    @Override
    protected void doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.VSC_CONVERTER_STATION, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        VscConverterStation vscConverterStation = network.getVscConverterStation(id);
        Double targetV = readDouble(tokens[SET_POINT_V_COLUMN_INDEX]) * vscConverterStation
            .getRegulatingTerminal()
            .getVoltageLevel()
            .getNominalV();
        Double targetQ = readDouble(tokens[SET_POINT_Q_COLUMN_INDEX]);

        if (targetQ == vscConverterStation.getReactivePowerSetpoint()) {
            targetQ = null;
        }
        if (targetV == vscConverterStation.getVoltageSetpoint()) {
            targetV = null;
        }
        modifications.add(new VscConverterStationModification(id, targetV, targetQ));
    }
}
