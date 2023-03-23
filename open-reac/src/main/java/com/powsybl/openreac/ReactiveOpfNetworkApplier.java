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
        findSectionCount(sc, b);
    }

    /**
     * As b is continuous in output files, we have to find the shunt closest section that matches with b.
     */
    private void findSectionCount(ShuntCompensator sc, double b) {
        double minDistance = Math.abs(b - sc.getB());
        double distance;
        int sectionCount = -1;
        for (int i = 0; i <= sc.getMaximumSectionCount(); i++) {
            distance = Math.abs(b - sc.getB(i));
            if (distance < minDistance) {
                minDistance = distance;
                sectionCount = i;
            }
        }
        if (sectionCount != -1) {
            sc.setSectionCount(sectionCount);
        }
    }
}
