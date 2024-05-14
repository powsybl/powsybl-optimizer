/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.output.AbstractNoThrowOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that reads output from ampl and generates network modifications
 *
 * @param <T> The type of modification read in the output file
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public abstract class AbstractNetworkOutput<T extends NetworkModification> extends AbstractNoThrowOutput {

    protected final Network network;
    protected final List<T> modifications = new ArrayList<>();

    protected AbstractNetworkOutput(Network network) {
        this.network = network;
    }

    public List<T> getModifications() {
        return modifications;
    }

    @Override
    public boolean throwOnMissingFile() {
        triggerErrorState();
        return false;
    }
}
