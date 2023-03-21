/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.DefaultAmplNetworkUpdater;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.*;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class ReactiveOpfNetworkApplier extends DefaultAmplNetworkUpdater {

    public ReactiveOpfNetworkApplier(StringToIntMapper<AmplSubset> networkMapper) {
        super(networkMapper);
    }

    @Override
    public void updateNetworkShunt(ShuntCompensator sc, int busNum, double q, double b, int sections) {
        sc.setSectionCount(findSectionCount(sc, b));
    }

    /**
     * As b is continuous in output files, we have to find the shunt closest section that matches with b.
     */
    private int findSectionCount(ShuntCompensator sc, double b) {
        int nbSection = 0;
        if (b <= 0) {
            return 0;
        }
        while (nbSection <= sc.getMaximumSectionCount() && sc.getB(nbSection) < b) {
            ++nbSection;
        }
        if (nbSection == sc.getMaximumSectionCount()) {
            return sc.getMaximumSectionCount();
        } else if (Math.abs(sc.getB(nbSection) - b) < Math.abs(sc.getB(nbSection - 1) - b)) {
            return nbSection;
        } else {
            return nbSection - 1;
        }
    }
}
