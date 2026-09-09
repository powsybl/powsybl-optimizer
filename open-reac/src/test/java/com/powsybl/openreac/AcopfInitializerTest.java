/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class AcopfInitializerTest {

    private static final double DELTA = 1e-6;

    @Test
    void testAnglesAreWrittenAndVoltagesAreKept() {
        Network network = IeeeCdfNetworkFactory.create14();
        Map<String, Double> voltagesBefore = network.getBusView().getBusStream()
                .collect(Collectors.toMap(Bus::getId, Bus::getV));

        String slackBusId = initialize(network);

        // the angle reference is the most meshed bus at the highest nominal voltage
        assertEquals(getBusViewBusId(network, "B4"), slackBusId);
        assertEquals(0, network.getBusView().getBus(slackBusId).getAngle(), DELTA);

        // every bus carries an angle, and the flat start is left behind
        assertTrue(network.getBusView().getBusStream().allMatch(b -> !Double.isNaN(b.getAngle())));
        assertTrue(network.getBusView().getBusStream().anyMatch(b -> Math.abs(b.getAngle()) > 1));

        // voltage magnitudes are the input of the optimization and are never touched
        network.getBusView().getBuses().forEach(b -> assertEquals(voltagesBefore.get(b.getId()), b.getV(), DELTA));
    }

    @Test
    void testSlackTerminalsOfTheNetworkAreNeitherReadNorWritten() {
        Network network = IeeeCdfNetworkFactory.create14();
        SlackTerminal.reset(network);
        // bus 14 is a leaf of the network: it would never be chosen by the most meshed selection
        SlackTerminal.attach(network.getBusBreakerView().getBus("B14"));

        String slackBusId = initialize(network);

        assertEquals(getBusViewBusId(network, "B4"), slackBusId);
        assertEquals(0, network.getBusView().getBus(slackBusId).getAngle(), DELTA);
        List<Bus> slackBuses = getSlackBuses(network);
        assertEquals(1, slackBuses.size());
        assertEquals(getBusViewBusId(network, "B14"), slackBuses.get(0).getId());
        assertNotEquals(0, slackBuses.get(0).getAngle(), 1e-3);
    }

    @Test
    void testActivePowerMismatchIsDistributedOnGenerators() {
        Network network = IeeeCdfNetworkFactory.create14();
        double generation = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        double load = network.getLoadStream().mapToDouble(Load::getP0).sum();
        assertTrue(Math.abs(generation - load) > 1, "IEEE 14 must be unbalanced for this test to be meaningful");
        Map<String, Double> targetsBefore = network.getGeneratorStream()
                .collect(Collectors.toMap(Generator::getId, Generator::getTargetP));

        initialize(network);

        // generation carried by the terminals is now balanced with the load (no losses in DC)
        double terminalGeneration = network.getGeneratorStream().mapToDouble(g -> -g.getTerminal().getP()).sum();
        assertEquals(load, terminalGeneration, 1e-3);
        // proportional to the target: generators with a null target do not participate
        network.getGenerators().forEach(g -> {
            if (targetsBefore.get(g.getId()) == 0) {
                assertEquals(0, g.getTerminal().getP(), DELTA);
            } else {
                assertNotEquals(targetsBefore.get(g.getId()), -g.getTerminal().getP(), 1e-3);
            }
        });
        // targets are the reference of the ACOPF objective and are never touched
        network.getGenerators().forEach(g -> assertEquals(targetsBefore.get(g.getId()), g.getTargetP(), DELTA));
    }

    @Test
    void testFailureWithoutGenerator() {
        Network network = IeeeCdfNetworkFactory.create14();
        network.getGeneratorStream().map(Generator::getId).toList().forEach(id -> network.getGenerator(id).remove());

        PowsyblException e = assertThrows(PowsyblException.class, () -> initialize(network));
        assertTrue(e.getMessage().startsWith("DC load flow initialization"), e.getMessage());
    }

    private static String initialize(Network network) {
        return AcopfInitializer.initialize(network, ReportNode.NO_OP);
    }

    private static String getBusViewBusId(Network network, String busBreakerViewBusId) {
        return network.getBusBreakerView().getBus(busBreakerViewBusId).getConnectedTerminalStream().findFirst().orElseThrow()
                .getBusView().getBus().getId();
    }

    private static List<Bus> getSlackBuses(Network network) {
        return network.getVoltageLevelStream()
                .<SlackTerminal>map(vl -> vl.getExtension(SlackTerminal.class))
                .filter(st -> st != null && st.getTerminal() != null)
                .map(st -> st.getTerminal().getBusView().getBus())
                .toList();
    }
}
