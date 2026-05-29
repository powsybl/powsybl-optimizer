/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.iidm.modification.BatteryModification;
import com.powsybl.iidm.modification.GeneratorModification;
import com.powsybl.iidm.modification.ShuntCompensatorModification;
import com.powsybl.iidm.modification.StaticVarCompensatorModification;
import com.powsybl.iidm.modification.VscConverterStationModification;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Data class to store all outputs resulting of NetworkModification.
 *
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class NetworkModifications {

    private final GeneratorNetworkOutput generatorNetworkOutput;
    private final BatteryNetworkOutput batteryOutput;
    private final ShuntCompensatorNetworkOutput shuntsOutput;
    private final VscNetworkOutput vscOutput;
    private final SvcNetworkOutput svcOutput;
    private final TapPositionNetworkOutput tapPositionOutput;

    public NetworkModifications(Network network, double shuntCompensatorActivationAlertThreshold) {
        generatorNetworkOutput = new GeneratorNetworkOutput(network);
        batteryOutput = new BatteryNetworkOutput(network);
        shuntsOutput = new ShuntCompensatorNetworkOutput(network, shuntCompensatorActivationAlertThreshold);
        vscOutput = new VscNetworkOutput(network);
        svcOutput = new SvcNetworkOutput(network);
        tapPositionOutput = new TapPositionNetworkOutput(network);
    }

    public List<AmplOutputFile> getOutputFiles() {
        return List.of(generatorNetworkOutput, batteryOutput, shuntsOutput, vscOutput, svcOutput, tapPositionOutput);
    }

    public List<GeneratorModification> getGeneratorModifications() {
        return generatorNetworkOutput.getModifications();
    }

    public List<BatteryModification> getBatteryModifications() {
        return batteryOutput.getModifications();
    }

    public List<ShuntCompensatorModification> getShuntModifications() {
        return shuntsOutput.getModifications();
    }

    public List<ShuntCompensatorNetworkOutput.ShuntWithDeltaDiscreteOptimalOverThreshold> getShuntsWithDeltaDiscreteOptimalOverThreshold() {
        return shuntsOutput.getShuntsWithDeltaDiscreteOptimalOverThresholds();
    }

    public List<VscConverterStationModification> getVscModifications() {
        return vscOutput.getModifications();
    }

    public List<StaticVarCompensatorModification> getSvcModifications() {
        return svcOutput.getModifications();
    }

    public List<RatioTapPositionModification> getTapPositionModifications() {
        return tapPositionOutput.getModifications();
    }
}
