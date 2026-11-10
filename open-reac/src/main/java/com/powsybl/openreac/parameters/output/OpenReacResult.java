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
import com.powsybl.iidm.network.regulation.RegulationMode;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.output.FixedParallelTransformersOutput.FixedParallelTransformer;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput.ReactiveSlack;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.DoubleConsumer;

/**
 * OpenReac user interface to get results information.
 *
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class OpenReacResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacResult.class);
    private final OpenReacStatus status;
    private final List<ReactiveSlack> reactiveSlacks;
    private final List<FixedParallelTransformer> fixedParallelTransformers;
    private final Map<String, String> indicators;
    private final List<GeneratorModification> generatorModifications;
    private final List<BatteryModification> batteryModifications;
    private final List<ShuntCompensatorModification> shuntsModifications;
    private final List<VscConverterStationModification> vscModifications;
    private final List<StaticVarCompensatorModification> svcModifications;
    private final List<RatioTapPositionModification> tapPositionModifications;
    private final HashMap<String, Pair<Double, Double>> voltageProfile;
    private final Map<String, Double> continuousSusceptanceByShunt;
    private final double totalAbsoluteReactiveDeviation;
    private final Map<String, Double> reactiveDeviationByShunt;
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
        this.fixedParallelTransformers = List.copyOf(amplIOFiles.getFixedParallelTransformersOutput().getFixedTransformers());
        this.generatorModifications = List.copyOf(amplIOFiles.getNetworkModifications().getGeneratorModifications());
        this.batteryModifications = List.copyOf(amplIOFiles.getNetworkModifications().getBatteryModifications());
        this.shuntsModifications = List.copyOf(amplIOFiles.getNetworkModifications().getShuntModifications());
        this.vscModifications = List.copyOf(amplIOFiles.getNetworkModifications().getVscModifications());
        this.svcModifications = List.copyOf(amplIOFiles.getNetworkModifications().getSvcModifications());
        this.tapPositionModifications = List.copyOf(amplIOFiles.getNetworkModifications().getTapPositionModifications());
        this.voltageProfile = new HashMap<>(amplIOFiles.getVoltageProfileOutput().getVoltageProfile());
        this.continuousSusceptanceByShunt = Map.copyOf(amplIOFiles.getNetworkModifications().getContinuousSusceptanceByShunt());
        this.totalAbsoluteReactiveDeviation = amplIOFiles.getNetworkModifications().getTotalAbsoluteReactiveDeviation();
        this.reactiveDeviationByShunt = Map.copyOf(amplIOFiles.getNetworkModifications().getReactiveDeviationByShunt());
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveSlack> getReactiveSlacks() {
        return reactiveSlacks;
    }

    /**
     * The transformers pinned by the AMPL model within a degenerate parallel bundle
     * (POINT/EMPTY effective intersection), each with the bundle it belongs to and the common
     * effective ratio it was fixed at. Informational only: the resulting tap change is already
     * carried by {@link #getTapPositionModifications()}, so these are not network modifications
     * and are not part of {@link #getAllNetworkModifications()}. A transformer the user declared
     * variable can appear here, meaning it was fixed to its bundle's shared ratio rather than
     * optimized freely, to avoid circulating reactive flows.
     */
    public List<FixedParallelTransformer> getFixedParallelTransformers() {
        return fixedParallelTransformers;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }

    public List<GeneratorModification> getGeneratorModifications() {
        return generatorModifications;
    }

    public List<BatteryModification> getBatteryModifications() {
        return batteryModifications;
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

    public Map<String, Double> getContinuousSusceptanceByShunt() {
        return continuousSusceptanceByShunt;
    }

    public double getTotalAbsoluteReactiveDeviation() {
        return totalAbsoluteReactiveDeviation;
    }

    public Map<String, Double> getReactiveDeviationByShunt() {
        return reactiveDeviationByShunt;
    }

    public boolean isUpdateNetworkWithVoltages() {
        return updateNetworkWithVoltages;
    }

    public void setUpdateNetworkWithVoltages(boolean updateNetworkWithVoltages) {
        this.updateNetworkWithVoltages = updateNetworkWithVoltages;
    }

    public List<NetworkModification> getAllNetworkModifications() {
        List<NetworkModification> modifs = new ArrayList<>(getGeneratorModifications().size() +
            getBatteryModifications().size() +
            getShuntsModifications().size() +
            getSvcModifications().size() +
            getTapPositionModifications().size() +
            getVscModifications().size());
        modifs.addAll(getGeneratorModifications());
        modifs.addAll(getBatteryModifications());
        modifs.addAll(getShuntsModifications());
        modifs.addAll(getSvcModifications());
        modifs.addAll(getTapPositionModifications());
        modifs.addAll(getVscModifications());
        return modifs;
    }

    /**
     * Applies all the optimization results to the given network.
     * <p>First, all network modifications (generators, batteries, shunts, static var compensators,
     * tap positions and VSC converter stations) are applied. Then the voltage targets of the
     * regulating ratio tap changers, shunts and batteries are updated from the optimized voltage
     * profile. Finally, if enabled with {@link #setUpdateNetworkWithVoltages(boolean)}, bus voltages
     * and angles are updated.</p>
     *
     * @param network the network to update
     * @throws IllegalStateException if a bus of the voltage profile (or the regulating bus of an
     *                               element) is missing from the network or the voltage profile
     */
    public void applyAllModifications(Network network) {
        for (NetworkModification modif : getAllNetworkModifications()) {
            modif.apply(network);
        }
        updateRatioTapChangerTargets(network);
        updateShuntTargets(network);
        updateBatteryTargets(network);
        updateBusVoltages(network);
    }

    /** Updates target of ratio tap changers specified as variable by user. */
    private void updateRatioTapChangerTargets(Network network) {
        getTapPositionModifications().stream()
                .map(AbstractTapPositionModification::getTransformerId)
                .map(network::getTwoWindingsTransformer)
                .forEach(transformer -> {
                    RatioTapChanger ratioTapChanger = transformer.getRatioTapChanger();
                    if (ratioTapChanger != null) {
                        updateTargetV(ratioTapChanger.getRegulatingTerminal(), transformer.getId(), ratioTapChanger.getVoltageRegulation()::setTargetValue);
                    }
                });
    }

    /** Updates target of shunts specified as variable by user. */
    private void updateShuntTargets(Network network) {
        getShuntsModifications().stream()
                .map(ShuntCompensatorModification::getShuntCompensatorId)
                .map(network::getShuntCompensator)
                .forEach(shunt ->
                        updateTargetV(shunt.getRegulatingTerminal(), shunt.getId(), v -> {
                            if (shunt.getVoltageRegulation() == null || shunt.getVoltageRegulation().getTerminal() == null) {
                                shunt.setLocalTargetV(v);
                            } else {
                                shunt.getVoltageRegulation().setTargetValue(v);
                            }
                        }));
    }

    /** Updates target voltage of regulating batteries. */
    private void updateBatteryTargets(Network network) {
        getBatteryModifications().stream()
                .map(BatteryModification::getBatteryId)
                .map(network::getBattery)
                .filter(Objects::nonNull)
                .filter(battery -> battery.isRegulatingWithMode(RegulationMode.VOLTAGE))
                .forEach(battery ->
                    updateTargetV(battery.getRegulatingTerminal(), battery.getId(), v -> {
                        if (battery.getVoltageRegulation() == null || battery.getVoltageRegulation().getTerminal() == null) {
                            battery.setLocalTargetV(v);
                        } else {
                            battery.getVoltageRegulation().setTargetValue(v);
                        }
                    }));
    }

    private void updateBusVoltages(Network network) {
        if (!isUpdateNetworkWithVoltages()) {
            return;
        }
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

    /**
     * Updates the target voltage of an element from the optimized voltage profile, at its regulating bus.
     * If the regulating bus cannot be resolved (null or disconnected regulating terminal), the update is
     * silently skipped with a warning. If the bus is resolved but missing from the voltage profile, an
     * {@link IllegalStateException} is thrown.
     */
    private void updateTargetV(Terminal regulatingTerminal, String elementId, DoubleConsumer targetVSetter) {
        Optional<Bus> bus = getRegulatingBus(regulatingTerminal, elementId);
        bus.ifPresent(b -> {
            Pair<Double, Double> busUpdate = voltageProfile.get(b.getId());
            if (busUpdate != null) {
                targetVSetter.accept(busUpdate.getFirst() * b.getVoltageLevel().getNominalV());
            } else {
                throw new IllegalStateException("Voltage profile not found for bus " + b.getId());
            }
        });
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
