/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

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
    public static String initialize(Network network, ReportNode reportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(reportNode);
        return runDcLoadFlow(network, reportNode);
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
                .setSlackDistributionFailureBehavior(OpenLoadFlowParameters.SlackDistributionFailureBehavior.LEAVE_ON_SLACK_BUS);
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
