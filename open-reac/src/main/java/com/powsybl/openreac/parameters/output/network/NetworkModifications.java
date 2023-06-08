/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.iidm.modification.GeneratorModification;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Data class to store all outputs resulting of NetworkModification.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class NetworkModifications {

    private final GeneratorNetworkOutput generatorNetworkOutput;

    public NetworkModifications(Network network) {
        generatorNetworkOutput = new GeneratorNetworkOutput(network);
    }

    public List<AmplOutputFile> getOutputFiles() {
        return List.of(generatorNetworkOutput);
    }

    public List<GeneratorModification> getGeneratorModifications() {
        return generatorNetworkOutput.getModifications();
    }
}
