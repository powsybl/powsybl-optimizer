/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.ShuntCompensatorModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class ShuntCompensatorNetworkOutput extends AbstractNetworkOutput<ShuntCompensatorModification> {
    private static final String ELEMENT = "shunts";
    public static final int EXPECTED_COLS = 6;
    private static final int ID_COLUMN_INDEX = 1;
    private static final int B_COLUMN_INDEX = 3;
    private static final int BUS_COLUMN_INDEX = 2;

    public ShuntCompensatorNetworkOutput(Network network) {
        super(network);
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
        String id = stringToIntMapper.getId(AmplSubset.SHUNT, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        double b = readDouble(tokens[B_COLUMN_INDEX]);
        String busId = stringToIntMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[BUS_COLUMN_INDEX]));
        Boolean reconnect = null;
        ShuntCompensator shuntCompensator = network.getShuntCompensator(id);
        if (busId != null && shuntCompensator != null && busId.equals(
            shuntCompensator.getTerminal().getBusView().getConnectableBus().getId())) {
            reconnect = true;
        }
        modifications.add(new ShuntCompensatorModification(id, reconnect, shuntCompensator == null ? null : findSectionCount(shuntCompensator, b)));
    }

    /**
     * As b is continuous in output files, we have to find the shunt closest section that matches with b.
     */
    private int findSectionCount(ShuntCompensator sc, double b) {
        double minDistance = Math.abs(b - sc.getB());
        double distance;
        int sectionCount = sc.getSectionCount();
        for (int i = 0; i <= sc.getMaximumSectionCount(); i++) {
            distance = Math.abs(b - sc.getB(i));
            if (distance < minDistance) {
                minDistance = distance;
                sectionCount = i;
            }
        }
        return sectionCount;
    }
}
