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
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * OpenReac user interface to get results information.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacResult.class);
    private final OpenReacStatus status;
    private final List<ReactiveSlack> reactiveSlacks;
    private final Map<String, String> indicators;
    private final List<GeneratorModification> generatorModifications;
    private final List<ShuntCompensatorModification> shuntsModifications;
    private final List<VscConverterStationModification> vscModifications;
    private final List<StaticVarCompensatorModification> svcModifications;
    private final List<RatioTapPositionModification> tapPositionModifications;
    private final HashMap<String, Pair<Double, Double>> voltageProfile;
    private boolean updateNetworkWithVoltages = true;

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
        this.voltageProfile = new HashMap<>(amplIOFiles.getVoltageProfileOutput().getVoltageProfile());
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

    public Map<String, Pair<Double, Double>> getVoltageProfile() {
        return voltageProfile;
    }

    public boolean isUpdateNetworkWithVoltages() {
        return updateNetworkWithVoltages;
    }

    public void setUpdateNetworkWithVoltages(boolean updateNetworkWithVoltages) {
        this.updateNetworkWithVoltages = updateNetworkWithVoltages;
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

        // update target of ratio tap changers regulating voltage
        network.getTwoWindingsTransformerStream()
                .filter(RatioTapChangerHolder::hasRatioTapChanger)
                .map(RatioTapChangerHolder::getRatioTapChanger)
                .filter(TapChanger::isRegulating)
                .forEach(ratioTapChanger -> Optional.ofNullable(ratioTapChanger.getRegulationTerminal()).ifPresentOrElse(
                    regulationTerminal -> Optional.ofNullable(regulationTerminal.getBusView().getBus()).ifPresentOrElse(
                        bus -> Optional.ofNullable(voltageProfile.get(bus.getId())).ifPresentOrElse(
                            busUpdate -> {
                                double v = busUpdate.getFirst();
                                ratioTapChanger.setTargetV(v * bus.getVoltageLevel().getNominalV());
                            }, () -> {
                                throw new IllegalStateException("Voltage profile not found for bus " + bus.getId());
                            }),
                        () -> LOGGER.warn("Bus of regulation terminal is null.")),
                    () -> LOGGER.warn("Regulation terminal of ratio tap changer is null.")));

        // update target of shunts regulating voltage
        network.getShuntCompensatorStream()
                .filter(ShuntCompensator::isVoltageRegulatorOn)
                .forEach(shuntCompensator -> Optional.ofNullable(shuntCompensator.getRegulatingTerminal()).ifPresentOrElse(
                    regulationTerminal -> Optional.ofNullable(regulationTerminal.getBusView().getBus()).ifPresentOrElse(
                        bus -> Optional.ofNullable(voltageProfile.get(bus.getId())).ifPresentOrElse(
                            busUpdate -> {
                                double v = busUpdate.getFirst();
                                shuntCompensator.setTargetV(v * bus.getVoltageLevel().getNominalV());
                            }, () -> {
                                throw new IllegalStateException("Voltage profile not found for bus " + bus.getId());
                            }),
                        () -> LOGGER.warn("Bus of regulation terminal is null.")),
                    () -> LOGGER.warn("Regulating terminal of shunt compensator {} is null.", shuntCompensator.getId())));

        // update voltages of the buses
        if (isUpdateNetworkWithVoltages()) {
            for (var busUpdate : voltageProfile.entrySet()) {
                Optional.ofNullable(network.getBusView().getBus(busUpdate.getKey())).ifPresentOrElse(
                    bus -> {
                        double v = busUpdate.getValue().getFirst();
                        double angle = busUpdate.getValue().getSecond();
                        bus.setV(v * bus.getVoltageLevel().getNominalV());
                        bus.setAngle(Math.toDegrees(angle));
                    }, () -> {
                        throw new IllegalStateException("Bus " + busUpdate.getKey() + " not found in network " + network.getId());
                    });
            }
        }
    }
}
