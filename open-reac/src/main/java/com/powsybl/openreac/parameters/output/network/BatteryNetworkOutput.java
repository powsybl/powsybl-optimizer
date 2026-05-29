/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.BatteryModification;
import com.powsybl.iidm.network.Network;

/**
 * Reads the OpenReac results for batteries and produces the corresponding reactive
 * power modifications. Active power is not optimized by OpenReac, so only the reactive
 * setpoint (targetQ) is written back; targetP is left untouched.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class BatteryNetworkOutput extends AbstractNetworkOutput<BatteryModification> {
    private static final String ELEMENT = "batteries";
    public static final int EXPECTED_COLS = 9;
    private static final int ID_COLUMN_INDEX = 1;
    private static final int TARGET_Q_COLUMN_INDEX = 6;

    public BatteryNetworkOutput(Network network) {
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
    protected void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.BATTERY, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        double targetQ = readDouble(tokens[TARGET_Q_COLUMN_INDEX]);
        // targetP = null: OpenReac does not optimize the battery active power.
        modifications.add(new BatteryModification(id, null, targetQ));
    }
}
