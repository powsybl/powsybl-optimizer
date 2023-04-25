/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.TapPositionModification;

import java.util.OptionalInt;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class TapNetworkOutput extends AbstractNetworkOutput<TapPositionModification> {
    private static final String ELEMENT = "rtc";
    private static final int ID_COLUMN_INDEX = 1;
    private static final int TAP_POS_COLUMN_INDEX = 4;

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    protected TapPositionModification doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.RATIO_TAP_CHANGER, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        int tapPosition = Integer.parseInt(tokens[TAP_POS_COLUMN_INDEX]);

        return new TapPositionModification(id,
                TapPositionModification.TransformerElement.TWO_WINDING_TRANSFORMER,
                TapPositionModification.TransformerType.RATIO,
                tapPosition,
                OptionalInt.empty());
    }
}
