/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.ComponentConstants;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveLimits;
import com.powsybl.iidm.network.ShuntCompensatorModelType;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.dc.DcLoadFlowEngine;
import com.powsybl.openloadflow.dc.DcLoadFlowParameters;
import com.powsybl.openloadflow.dc.DcLoadFlowResult;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.LfNetworkStateUpdateParameters;
import com.powsybl.openloadflow.network.ReactivePowerDispatchMode;
import com.powsybl.openloadflow.network.ReferenceBusSelectionMode;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import com.powsybl.openloadflow.util.PerUnit;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.ReferenceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Writes the starting point of the ACOPF into the working variant of the network, on its main synchronous component.
 * <p>
 * The voltage angles and the active powers are those of a DC load flow solved with OpenLoadFlow, the active power
 * mismatch being distributed on generators proportionally to their active power target. Its slack bus, the most
 * meshed bus of the highest nominal voltages, is the angle reference: it is returned to be given to the ACOPF, and
 * the slack terminals carried by the network are neither read nor written. Voltage magnitudes are not written by the
 * DC load flow. The DC load flow engine is used directly
 * rather than the {@link com.powsybl.loadflow.LoadFlow} API on purpose: the OpenLoadFlow provider resets the whole
 * network state, including voltage magnitudes, before writing DC results.
 * <p>
 * The controls the ACOPF optimizes are written first, as requested by {@link ReferenceState}. With
 * {@link ReferenceState#NEUTRAL}, the sections of the non linear shunt compensators are not written: the bounds
 * exported to AMPL for these shunts depend on their current section.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class AcopfInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcopfInitializer.class);

    private AcopfInitializer() {
    }

    /**
     * @return the id of the slack bus of the DC load flow, the angle reference of the ACOPF.
     * @throws PowsyblException if the main synchronous component cannot be computed or if the DC load flow fails.
     */
    public static String initialize(Network network, OpenReacParameters parameters, ReportNode reportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(reportNode);
        if (parameters.getReferenceState() == ReferenceState.NEUTRAL) {
            writeNeutralReferenceState(network, parameters);
        }
        return runDcLoadFlow(network, reportNode);
    }

    private static void writeNeutralReferenceState(Network network, OpenReacParameters parameters) {
        getMainComponentBuses(network).forEach(bus -> bus.setV(bus.getVoltageLevel().getNominalV()));
        // only the transformers whose ratio the AMPL model optimizes (BRANCHCC_REGL_VAR): the others keep their
        // ratio as a data, and no tap would be returned to undo the move on the caller's variant
        parameters.getVariableTwoWindingsTransformers().stream()
                .map(network::getTwoWindingsTransformer)
                .filter(t -> t.hasRatioTapChanger() && isOptimizedRatio(t, parameters))
                .forEach(t -> t.getRatioTapChanger().setTapPosition(getNeutralPosition(t.getRatioTapChanger())));
        // as in the AMPL model, a disconnected variable shunt is considered at its connectable bus
        List<String> nonLinearShunts = new ArrayList<>();
        parameters.getVariableShuntCompensators().stream()
                .map(network::getShuntCompensator)
                .filter(sc -> isInMainComponent(sc.getTerminal().getBusView().getConnectableBus()))
                .forEach(sc -> {
                    if (sc.getModelType() == ShuntCompensatorModelType.LINEAR) {
                        sc.setSectionCount(0);
                    } else {
                        nonLinearShunts.add(sc.getId());
                    }
                });
        if (!nonLinearShunts.isEmpty()) {
            LOGGER.warn("Non linear shunt compensators are left at their section count, their AMPL bounds depend on it: {}", nonLinearShunts);
        }
        // the ACOPF optimizes the reactive power of every VSC converter station of the component, regulating or not
        network.getVscConverterStationStream()
                .filter(vsc -> isInMainComponent(vsc.getTerminal()))
                .forEach(vsc -> vsc.setReactivePowerSetpoint(getNeutralReactivePower(vsc)));
    }

    private static boolean isOptimizedRatio(TwoWindingsTransformer t, OpenReacParameters parameters) {
        // both sides in the component and above the nominal voltage filter of the AMPL model (BUS_ELIGIBLE)
        if (!isInMainComponent(t.getTerminal1()) || !isInMainComponent(t.getTerminal2())
                || t.getTerminal1().getVoltageLevel().getNominalV() < parameters.getMinNominalVoltageIgnoredBus()
                || t.getTerminal2().getVoltageLevel().getNominalV() < parameters.getMinNominalVoltageIgnoredBus()) {
            return false;
        }
        // not a zero impedance branch of the AMPL model (BRANCHZNULL), whose impedance is exported in pu on side 2
        double nominalV2 = t.getTerminal2().getVoltageLevel().getNominalV();
        double zb2 = nominalV2 * nominalV2 / PerUnit.SB;
        double z2 = (t.getR() * t.getR() + t.getX() * t.getX()) / (zb2 * zb2);
        double zNull = parameters.getLowImpedanceThreshold();
        return z2 > zNull * zNull;
    }

    private static int getNeutralPosition(RatioTapChanger ratioTapChanger) {
        return ratioTapChanger.getNeutralPosition().orElseGet(() -> IntStream
                .rangeClosed(ratioTapChanger.getLowTapPosition(), ratioTapChanger.getHighTapPosition())
                .boxed()
                .min(Comparator.comparingDouble(position -> Math.abs(ratioTapChanger.getStep(position).getRho() - 1)))
                .orElseThrow());
    }

    private static double getNeutralReactivePower(VscConverterStation vsc) {
        // middle of the envelope of the PQ diagram, as the ACOPF bounds it
        ReactiveLimits limits = vsc.getReactiveLimits();
        if (limits instanceof ReactiveCapabilityCurve curve) {
            double minQ = curve.getPoints().stream().mapToDouble(ReactiveCapabilityCurve.Point::getMinQ).min().orElseThrow();
            double maxQ = curve.getPoints().stream().mapToDouble(ReactiveCapabilityCurve.Point::getMaxQ).max().orElseThrow();
            return 0.5 * (minQ + maxQ);
        }
        return 0.5 * (limits.getMinQ(0) + limits.getMaxQ(0));
    }

    private static Stream<Bus> getMainComponentBuses(Network network) {
        return network.getBusView().getBusStream().filter(AcopfInitializer::isInMainComponent);
    }

    private static boolean isInMainComponent(Terminal terminal) {
        return isInMainComponent(terminal.getBusView().getBus());
    }

    private static boolean isInMainComponent(Bus bus) {
        return bus != null && bus.getSynchronousComponent().getNum() == ComponentConstants.MAIN_NUM;
    }

    private static String runDcLoadFlow(Network network, ReportNode reportNode) {
        LoadFlowParameters loadFlowParameters = createLoadFlowParameters();
        OpenLoadFlowParameters openLoadFlowParameters = OpenLoadFlowParameters.get(loadFlowParameters);
        DcLoadFlowParameters dcParameters = OpenLoadFlowParameters.createDcParameters(network, loadFlowParameters, openLoadFlowParameters,
                new SparseMatrixFactory(), new EvenShiloachGraphDecrementalConnectivityFactory<>(), false);

        List<DcLoadFlowResult> results = DcLoadFlowEngine.run(network, new LfNetworkLoaderImpl(), dcParameters, reportNode);
        if (results.size() != 1) {
            throw new PowsyblException("DC load flow initialization: expected a single main synchronous component, got " + results.size());
        }
        DcLoadFlowResult result = results.get(0);
        if (result.getNetwork().getValidity() != LfNetwork.Validity.VALID) {
            throw new PowsyblException("DC load flow initialization: main synchronous component cannot be computed ("
                    + result.getNetwork().getValidity() + ")");
        }
        if (!result.isSuccess()) {
            throw new PowsyblException("DC load flow initialization failed: " + result.toComponentResultStatus().statusText());
        }

        result.getNetwork().updateState(createStateUpdateParameters());

        LfBus slackBus = result.getNetwork().getSynchronousNetworks().getFirst().getSlackBuses().getFirst();
        LOGGER.info("DC load flow initialization: slack bus {}, {} MW distributed on generators, {} MW left on slack bus",
                slackBus.getId(), result.getDistributedActivePower() * PerUnit.SB, result.getSlackBusActivePowerMismatch() * PerUnit.SB);
        return slackBus.getId();
    }

    private static LoadFlowParameters createLoadFlowParameters() {
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setDc(true)
                .setComponentMode(LoadFlowParameters.ComponentMode.MAIN_SYNCHRONOUS) // the component optimized by OpenReac
                .setDistributedSlack(true)
                .setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P)
                .setDcUseTransformerRatio(false) // ratios of the variable transformers are decided by the ACOPF
                // phase shifter taps and HVDC active powers are data for the ACOPF: the DC load flow uses the same
                .setPhaseShifterRegulationOn(false)
                .setHvdcAcEmulation(false)
                .setReadSlackBus(false) // the slack bus is always the one selected below
                .setWriteSlackBus(false); // and is given to AMPL as a parameter, not through the network
        OpenLoadFlowParameters.create(parameters)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
                // the ACOPF has its own active power slack handling: a mismatch that cannot be distributed is not an error here
                .setSlackDistributionFailureBehavior(OpenLoadFlowParameters.SlackDistributionFailureBehavior.LEAVE_ON_SLACK_BUS)
                // as the former DCOPF, the mismatch is distributed regardless of the active power limits: the ACOPF
                // starts from the resulting phases only, its own dispatch is bounded
                .setUseActiveLimits(false);
        return parameters;
    }

    private static LfNetworkStateUpdateParameters createStateUpdateParameters() {
        return new LfNetworkStateUpdateParameters(
                false, // reactiveLimits
                false, // writeSlackBus
                false, // phaseShifterRegulationOn
                false, // transformerVoltageControlOn
                false, // transformerReactivePowerControlOn
                false, // loadPowerFactorConstant
                true, // dc: voltage magnitudes are not written
                false, // breakers
                ReactivePowerDispatchMode.Q_EQUAL_PROPORTION,
                false, // writeReferenceTerminals
                ReferenceBusSelectionMode.FIRST_SLACK,
                false); // simulateAutomationSystems
    }
}
