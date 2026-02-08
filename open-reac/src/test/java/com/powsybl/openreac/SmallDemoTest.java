/**
 * Copyright (c) 2026, Artelys (http://www.artelys.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmallDemoTest {

    // Demo: 
    // (1) Prepare IEEE 14-bus network (generator bounds, voltage limits).
    // (2) Run OpenReac optimisation. 
    // (3) Apply results to network.
    // (4) Run AC load flow to check convergence.
    @Test
    void testOpenReac() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();

        // Generator active power bounds (override IEEE 14 defaults)
        network.getGenerator("B1-G").setMinP(0).setMaxP(100);
        network.getGenerator("B2-G").setMinP(0).setMaxP(100);

        // Voltage limits per voltage level (required for reactive OPF)
        setDefaultVoltageLimits(network);

        OpenReacParameters parameters = new OpenReacParameters();

        // Run OpenReac reactive optimisation
        OpenReacResult openReacResult = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(),
                parameters, new OpenReacConfig(true), new LocalComputationManager(), ReportNode.NO_OP, null);
        assertEquals(OpenReacStatus.OK, openReacResult.getStatus());

        // Apply voltage plan and setpoints to the network
        openReacResult.applyAllModifications(network);

        // Load flow parameters aligned with OpenReac for consistent comparison
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setDistributedSlack(false)
                .setUseReactiveLimits(true)
                .setReadSlackBus(false);
        OpenLoadFlowParameters.create(loadFlowParameters)
                .setLowImpedanceBranchMode(OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED);

        // Run AC load flow to check convergence
        LoadFlowResult result = LoadFlow.run(network, loadFlowParameters);
        assertTrue(result.isFullyConverged());
    }

    void setDefaultVoltageLimits(Network network) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(0.8 * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(1.2 * vl.getNominalV());
            }
        }
    }

}
