/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.modification.ShuntCompensatorModification;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.openreac.network.ShuntNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.FixedParallelTransformersOutput;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
class OpenReacResultsTest {

    @Test
    void testTransformerTargetVUpdateWithoutVoltageResult() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        String t2wtId = "T2wT";
        RatioTapChanger rtc = network.getTwoWindingsTransformer(t2wtId).getRatioTapChanger()
                .setTargetDeadband(0)
                .setRegulating(true);

        // add transformer as variable for target V update
        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getTapPositionModifications().add(new RatioTapPositionModification(t2wtId, 0));

        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Voltage profile not found for bus " + rtc.getRegulationTerminal().getBusView().getBus().getId(), e.getMessage());
    }

    @Test
    void testTransformerTargetVUpdateWithoutRegulationBus() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        String t2wtId = "T2wT";
        RatioTapChanger rtc = network.getTwoWindingsTransformer(t2wtId).getRatioTapChanger()
                .setTargetDeadband(0)
                .setRegulating(true);
        rtc.getRegulationTerminal().disconnect();

        // add transformer as variable for target V update
        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getTapPositionModifications().add(new RatioTapPositionModification(t2wtId, 0));

        // apply results without warm start (to avoid exception)
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // target V is not updated
        assertEquals(33, rtc.getTargetV());
    }

    @Test
    void testShuntTargetVUpdateWithoutVoltageResult() throws IOException {
        Network network = ShuntNetworkFactory.createWithNonLinearModel();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        String regulatedBusId = shunt.getRegulatingTerminal().getBusView().getBus().getId();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getShuntModifications().add(new ShuntCompensatorModification("SHUNT", true, 0));

        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Voltage profile not found for bus " + regulatedBusId, e.getMessage());
    }

    @Test
    void testShuntUpdateWithoutRegulationBus() throws IOException {
        Network network = ShuntNetworkFactory.createWithNonLinearModel();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        shunt.getRegulatingTerminal().disconnect();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getShuntModifications().add(new ShuntCompensatorModification("SHUNT", null, 0));

        // apply results without warm start
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // target V not updated
        assertEquals(393, shunt.getTargetV());
    }

    @Test
    void testWrongVoltageResult() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        String idBusNotFound = io.getVoltageProfileOutput().getVoltageProfile().keySet().iterator().next();
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Bus " + idBusNotFound + " not found in network " + network.getId(), e.getMessage());
    }

    @Test
    void testFixedParallelTransformersExposedInResult() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        OpenReacAmplIOFiles io = new OpenReacAmplIOFiles(new OpenReacParameters(), null, network, true, ReportNode.NO_OP);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_fixed_parallel_transformers.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            io.getFixedParallelTransformersOutput().read(reader, null);
        }

        OpenReacResult result = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        List<FixedParallelTransformersOutput.FixedParallelTransformer> fixed = result.getFixedParallelTransformers();

        // the two members of the degenerate bundle are surfaced, in file order, with their
        // bundle, id and fixed effective ratio
        assertEquals(2, fixed.size());
        assertEquals(3, fixed.get(0).getBundle());
        assertEquals("T1", fixed.get(0).getTransformerId());
        assertEquals(1.043478, fixed.get(0).getFixedEffectiveRho(), 1e-6);
        assertEquals(3, fixed.get(1).getBundle());
        assertEquals("T2", fixed.get(1).getTransformerId());
        assertEquals(0.958, fixed.get(1).getFixedEffectiveRho(), 1e-6);

        // informational output: the tap change is carried by tap position modifications, so
        // fixed parallel transformers must not leak into the network modifications
        assertTrue(result.getAllNetworkModifications().isEmpty());
    }

    private OpenReacAmplIOFiles getIOWithMockVoltageProfile(Network network) throws IOException {
        OpenReacAmplIOFiles io = new OpenReacAmplIOFiles(new OpenReacParameters(), null, network, true, ReportNode.NO_OP);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_voltages.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            io.getVoltageProfileOutput().read(reader, null);
        }
        return io;
    }
}
