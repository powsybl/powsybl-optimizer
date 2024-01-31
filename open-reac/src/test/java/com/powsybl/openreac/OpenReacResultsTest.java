package com.powsybl.openreac;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.openreac.network.ShuntNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenReacResultsTest {

    @Test
    void testTransformerUpdateWithoutVoltageResult() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        RatioTapChanger rtc = network.getTwoWindingsTransformer("T2wT").getRatioTapChanger()
                .setTargetDeadband(0)
                .setRegulating(true);
        String regulatedBusId = rtc.getRegulationTerminal().getBusView().getBus().getId();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Voltage profile not found for bus " + regulatedBusId, e.getMessage());
    }

    @Test
    void testTransformerUpdateWithNullRegulatedBus() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        RatioTapChanger rtc = network.getTwoWindingsTransformer("T2wT").getRatioTapChanger()
                .setTargetDeadband(0)
                .setRegulating(true);
        rtc.getRegulationTerminal().disconnect();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());

        // apply results without warm start
        results.setUpdateNetworkWithVoltages(false);
        results.applyAllModifications(network);

        // target V not updated
        assertEquals(33, rtc.getTargetV());
    }

    @Test
    void testShuntUpdateWithoutVoltageResult() throws IOException {
        Network network = ShuntNetworkFactory.create();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        String regulatedBusId = shunt.getRegulatingTerminal().getBusView().getBus().getId();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> results.applyAllModifications(network));
        assertEquals("Voltage profile not found for bus " + regulatedBusId, e.getMessage());
    }

    @Test
    void testShuntUpdateWithNullRegulatedBus() throws IOException {
        Network network = ShuntNetworkFactory.create();
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        shunt.getRegulatingTerminal().disconnect();

        OpenReacAmplIOFiles io = getIOWithMockVoltageProfile(network);
        OpenReacResult results = new OpenReacResult(OpenReacStatus.OK, io, new HashMap<>());

        // apply results without warm start
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

    private OpenReacAmplIOFiles getIOWithMockVoltageProfile(Network network) throws IOException {
        OpenReacAmplIOFiles io = new OpenReacAmplIOFiles(new OpenReacParameters(), network, true);
        try (InputStream input = getClass().getResourceAsStream("/mock_outputs/reactiveopf_results_voltages.csv");
             InputStreamReader in = new InputStreamReader(input);
             BufferedReader reader = new BufferedReader(in)) {
            io.getVoltageProfileOutput().read(reader, null);
        }
        return io;
    }
}
