/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.iidm.modification.*;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Data class to store all outputs resulting of NetworkModification.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * @implNote This class does nothing exept grouping class extending <code>AbstractNetworkOutput</code>.
 */
public class OpenReacModifications {

    private final GeneratorNetworkOutput genOutput;
    private final ShuntCompensatorNetworkOutput shuntsOutput;
    private final VscNetworkOutput vscOutput;
    private final SvcNetworkOutput svcOutput;
    private final TapNetworkOutput tapOutput;

    public OpenReacModifications(Network network) {
        genOutput = new GeneratorNetworkOutput(network);
        shuntsOutput = new ShuntCompensatorNetworkOutput(network);
        vscOutput = new VscNetworkOutput(network);
        svcOutput = new SvcNetworkOutput(network);
        tapOutput = new TapNetworkOutput();
    }

    public List<AmplOutputFile> getOutputFiles() {
        return List.of(genOutput, shuntsOutput, vscOutput, svcOutput, tapOutput);
    }

    public List<GeneratorModification> getGeneratorModifications() {
        return genOutput.getModifications();
    }

    public List<ShuntCompensatorModification> getShuntModifications() {
        return shuntsOutput.getModifications();
    }

    public List<VscConverterStationModification> getVscModifications() {
        return vscOutput.getModifications();
    }

    public List<StaticVarCompensatorModification> getSvcModifications() {
        return svcOutput.getModifications();
    }

    public List<RatioTapPositionModification> getTapModifications() {
        return tapOutput.getModifications();
    }
}
