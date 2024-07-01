/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class NetworkIndicatorsOutput extends AbstractStateEstimatorIndicatorsOutput {

    @Override
    public String getFileName() {
        return "se_network_indic.txt";
    }
}
