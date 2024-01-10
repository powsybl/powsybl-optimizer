/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.iidm.modification.*;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput.ReactiveSlack;
import com.powsybl.openreac.parameters.output.VoltagePlanOutput.BusResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenReac user interface to get results information.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacResult {

    private final OpenReacStatus status;
    private final List<ReactiveSlack> reactiveSlacks;
    private final Map<String, String> indicators;
    private final List<GeneratorModification> generatorModifications;
    private final List<ShuntCompensatorModification> shuntsModifications;
    private final List<VscConverterStationModification> vscModifications;
    private final List<StaticVarCompensatorModification> svcModifications;
    private final List<RatioTapPositionModification> tapPositionModifications;
    private final List<BusResult> voltagePlan;

    /**
     * @param status      the final status of the OpenReac run.
     * @param amplIOFiles a file interface to fetch output file information.
     * @param indicators  a standard map written by the OpenReac ampl model.
     */
    public OpenReacResult(OpenReacStatus status, OpenReacAmplIOFiles amplIOFiles, Map<String, String> indicators) {
        Objects.requireNonNull(amplIOFiles);
        this.status = Objects.requireNonNull(status);
        this.indicators = Map.copyOf(Objects.requireNonNull(indicators));
        this.reactiveSlacks = List.copyOf(amplIOFiles.getReactiveSlackOutput().getSlacks());
        this.generatorModifications = List.copyOf(amplIOFiles.getNetworkModifications().getGeneratorModifications());
        this.shuntsModifications = List.copyOf(amplIOFiles.getNetworkModifications().getShuntModifications());
        this.vscModifications = List.copyOf(amplIOFiles.getNetworkModifications().getVscModifications());
        this.svcModifications = List.copyOf(amplIOFiles.getNetworkModifications().getSvcModifications());
        this.tapPositionModifications = List.copyOf(amplIOFiles.getNetworkModifications().getTapPositionModifications());
        this.voltagePlan = List.copyOf(amplIOFiles.getVoltagePlanOutput().getVoltagePlan());
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveSlack> getReactiveSlacks() {
        return reactiveSlacks;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }

    public List<GeneratorModification> getGeneratorModifications() {
        return generatorModifications;
    }

    public List<ShuntCompensatorModification> getShuntsModifications() {
        return shuntsModifications;
    }

    public List<StaticVarCompensatorModification> getSvcModifications() {
        return svcModifications;
    }

    public List<RatioTapPositionModification> getTapPositionModifications() {
        return tapPositionModifications;
    }

    public List<VscConverterStationModification> getVscModifications() {
        return vscModifications;
    }

    public List<BusResult> getVoltagePlan() {
        return voltagePlan;
    }

    public List<NetworkModification> getAllNetworkModifications() {
        List<NetworkModification> modifs = new ArrayList<>(getGeneratorModifications().size() +
            getShuntsModifications().size() +
            getSvcModifications().size() +
            getTapPositionModifications().size() +
            getVscModifications().size());
        modifs.addAll(getGeneratorModifications());
        modifs.addAll(getShuntsModifications());
        modifs.addAll(getSvcModifications());
        modifs.addAll(getTapPositionModifications());
        modifs.addAll(getVscModifications());
        return modifs;
    }

    public void applyAllModifications(Network network) {
        for (NetworkModification modif : getAllNetworkModifications()) {
            modif.apply(network);
        }

        for (BusResult busResult : getVoltagePlan()) {
            Bus b = network.getBusView().getBus(busResult.getBusId());

            // TODO : update voltage values of b ?

            // Update target of ratio tap changers regulating voltage
            network.getTwoWindingsTransformerStream()
                    .map(RatioTapChangerHolder::getRatioTapChanger)
                    .filter(TapChanger::isRegulating)
                    .filter(ratioTapChanger -> ratioTapChanger.getRegulationTerminal().getBusView().getBus() == b)
                    .forEach(ratioTapChanger -> ratioTapChanger.setTargetV(busResult.getV()));

            // Update target of shunts regulating voltage
            network.getShuntCompensatorStream()
                    .filter(ShuntCompensator::isVoltageRegulatorOn)
                    .filter(shuntCompensator -> shuntCompensator.getRegulatingTerminal().getBusView().getBus() == b)
                    .forEach(shuntCompensator -> shuntCompensator.setTargetV(busResult.getV()));
        }
    }
}
