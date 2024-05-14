/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public class TapPositionNetworkOutput extends AbstractNetworkOutput<RatioTapPositionModification> {
    private static final String ELEMENT = "rtc";
    public static final int EXPECTED_COLS = 3;
    private static final int TRANSFO_ID_COLUMN_INDEX = 1;
    private static final int TAP_POS_COLUMN_INDEX = 2;

    public TapPositionNetworkOutput(Network network) {
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
        String transfoId = stringToIntMapper.getId(AmplSubset.BRANCH, Integer.parseInt(tokens[TRANSFO_ID_COLUMN_INDEX]));
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(transfoId);
        if (twt == null || !twt.hasRatioTapChanger()) {
            throw new PowsyblException("Error parsing rtc from " + getFileName() + ", invalid number.");
        }
        int tapPosition = -1 + twt.getRatioTapChanger().getLowTapPosition()
            + Integer.parseInt(tokens[TAP_POS_COLUMN_INDEX]);
        modifications.add(new RatioTapPositionModification(transfoId, tapPosition));
    }
}
