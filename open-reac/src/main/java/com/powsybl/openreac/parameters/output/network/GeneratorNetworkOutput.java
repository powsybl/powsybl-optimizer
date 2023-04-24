/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.GeneratorModification;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.network.Network;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class GeneratorNetworkOutput extends AbstractNetworkOutput<GeneratorModification> {
    private static final String ELEMENT = "generators";
    private static final int ID_COLUMN_INDEX = 1;
    private static final int TARGET_V_COLUMN_INDEX = 4;
    private static final int TARGET_Q_COLUMN_INDEX = 6;
    private final Network network;

    public GeneratorNetworkOutput(Network network) {
        this.network = network;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    protected GeneratorModification doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.GENERATOR, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        double targetV = Double.parseDouble(tokens[TARGET_V_COLUMN_INDEX]);
        double targetQ = Double.parseDouble(tokens[TARGET_Q_COLUMN_INDEX]);

        GeneratorModification.Modifs modifs = new GeneratorModification.Modifs();
        if (targetQ != network.getGenerator(id).getTargetQ()) {
            modifs.setTargetQ(targetQ);
        }
        if (targetV != network.getGenerator(id).getTargetV()) {
            modifs.setTargetV(targetV);
        }
        return new GeneratorModification(id, modifs);
    }
}
