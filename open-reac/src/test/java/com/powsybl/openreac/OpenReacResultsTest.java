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
import com.powsybl.iidm.modification.BatteryModification;
import com.powsybl.iidm.modification.ShuntCompensatorModification;
import com.powsybl.iidm.modification.tapchanger.RatioTapPositionModification;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.extensions.VoltageRegulation;
import com.powsybl.openreac.network.BatteryNetworkFactory;
import com.powsybl.openreac.network.ShuntNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void testBatteryTargetVUpdate() throws IOException {
        Network network = BatteryNetworkFactory.createWithVoltageRegulationOn();
        Battery battery = network.getBattery("BATTERY");
        VoltageRegulation voltageRegulation = battery.getExtension(VoltageRegulation.class);
        String regulatedBusId = voltageRegulation.getRegulatingTerminal().getBusView().getBus().getId();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        // make the regulated bus part of the optimized voltage profile
        io.getVoltageProfileOutput().getVoltageProfile().put(regulatedBusId, Pair.of(1.05, 0.));
        io.getNetworkModifications().getBatteryModifications().add(new BatteryModification("BATTERY", null, 5.));

        // apply results without warm start (mock profile buses are not in the network)
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // optimized targetQ is applied, targetV is updated from the voltage profile
        assertEquals(5., battery.getTargetQ());
        assertEquals(1.05 * 400, voltageRegulation.getTargetV(), 1e-9);
    }

    @Test
    void testBatteryTargetVUpdateWithoutVoltageResult() throws IOException {
        Network network = BatteryNetworkFactory.createWithVoltageRegulationOn();
        Battery battery = network.getBattery("BATTERY");
        String regulatedBusId = battery.getExtension(VoltageRegulation.class)
                .getRegulatingTerminal().getBusView().getBus().getId();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getBatteryModifications().add(new BatteryModification("BATTERY", null, 5.));

        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Voltage profile not found for bus " + regulatedBusId, e.getMessage());
    }

    @Test
    void testBatteryUpdateWithoutRegulationBus() throws IOException {
        Network network = BatteryNetworkFactory.createWithVoltageRegulationOn();
        Battery battery = network.getBattery("BATTERY");
        VoltageRegulation voltageRegulation = battery.getExtension(VoltageRegulation.class);
        double initialTargetV = voltageRegulation.getTargetV();
        voltageRegulation.getRegulatingTerminal().disconnect();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getBatteryModifications().add(new BatteryModification("BATTERY", null, 5.));

        // apply results without warm start
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // targetQ is applied but targetV is not updated (regulating bus cannot be resolved)
        assertEquals(5., battery.getTargetQ());
        assertEquals(initialTargetV, voltageRegulation.getTargetV());
    }

    @Test
    void testBatteryUpdateWithVoltageRegulationOff() throws IOException {
        Network network = BatteryNetworkFactory.createWithVoltageRegulationOn();
        Battery battery = network.getBattery("BATTERY");
        VoltageRegulation voltageRegulation = battery.getExtension(VoltageRegulation.class);
        voltageRegulation.setVoltageRegulatorOn(false);
        double initialTargetV = voltageRegulation.getTargetV();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getBatteryModifications().add(new BatteryModification("BATTERY", null, 5.));

        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // targetQ is applied but targetV is left untouched
        assertEquals(5., battery.getTargetQ());
        assertEquals(initialTargetV, voltageRegulation.getTargetV());
    }

    @Test
    void testBatteryUpdateWithoutVoltageRegulationExtension() throws IOException {
        Network network = BatteryNetworkFactory.create();
        Battery battery = network.getBattery("BATTERY");

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        io.getNetworkModifications().getBatteryModifications().add(new BatteryModification("BATTERY", null, 5.));

        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // targetQ is applied, no targetV update is attempted and no exception is raised
        assertEquals(5., battery.getTargetQ());
        assertNull(battery.getExtension(VoltageRegulation.class));
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
