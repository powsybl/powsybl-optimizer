/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.iidm.modification.*;
import com.powsybl.iidm.modification.tapchanger.AbstractTapPositionModification;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput.ReactiveSlack;
import com.powsybl.openreac.parameters.output.network.ShuntCompensatorNetworkOutput;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * OpenReac user interface to get results information.
 *
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
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
    private final ShuntCompensatorNetworkOutput shuntsOutput;
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
        this.shuntsOutput = amplIOFiles.getNetworkModifications().getShuntsOutput();
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

    public ShuntCompensatorNetworkOutput getShuntsOutput() {
        return shuntsOutput;
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

        // update target of ratio tap changers specified as variable by user
        getTapPositionModifications().stream()
                .map(AbstractTapPositionModification::getTransformerId)
                .map(network::getTwoWindingsTransformer)
                .forEach(transformer -> {
                    RatioTapChanger ratioTapChanger = transformer.getRatioTapChanger();
                    if (ratioTapChanger != null) {
                        Optional<Bus> bus = getRegulatingBus(ratioTapChanger.getRegulationTerminal(), transformer.getId());
                        bus.ifPresent(b -> {
                            Pair<Double, Double> busUpdate = voltageProfile.get(b.getId());
                            if (busUpdate != null) {
                                ratioTapChanger.setTargetV(busUpdate.getFirst() * b.getVoltageLevel().getNominalV());
                            } else {
                                throw new IllegalStateException("Voltage profile not found for bus " + b.getId());
                            }
                        });
                    }
                });

        // update target of shunts specified as variable by user
        getShuntsModifications().stream()
                .map(ShuntCompensatorModification::getShuntCompensatorId)
                .map(network::getShuntCompensator)
                .forEach(shunt -> {
                    Optional<Bus> bus = getRegulatingBus(shunt.getRegulatingTerminal(), shunt.getId());
                    bus.ifPresent(b -> {
                        Pair<Double, Double> busUpdate = voltageProfile.get(b.getId());
                        if (busUpdate != null) {
                            shunt.setTargetV(busUpdate.getFirst() * b.getVoltageLevel().getNominalV());
                        } else {
                            throw new IllegalStateException("Voltage profile not found for bus " + b.getId());
                        }
                    });
                });

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

    Optional<Bus> getRegulatingBus(Terminal terminal, String elementId) {
        if (terminal == null) {
            LOGGER.warn("Regulating terminal of element {} is null.", elementId);
            return Optional.empty();
        }
        Bus bus = terminal.getBusView().getBus();
        if (bus == null) {
            LOGGER.warn("Bus of regulating terminal of element {} is null.", elementId);
            return Optional.empty();
        }
        return Optional.ofNullable(bus);
    }
}
