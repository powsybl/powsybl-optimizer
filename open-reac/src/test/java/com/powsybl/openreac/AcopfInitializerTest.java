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
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.network.ShuntNetworkFactory;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.ReferenceState;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
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

        String slackBusId = initialize(network, new OpenReacParameters());

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

        String slackBusId = initialize(network, new OpenReacParameters());

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

        initialize(network, new OpenReacParameters());

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
    void testActivePowerLimitsDoNotBoundTheDistribution() {
        Network network = IeeeCdfNetworkFactory.create14();
        // surplus of generation: the distribution lowers the generators
        assertTrue(network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum()
                > network.getLoadStream().mapToDouble(Load::getP0).sum());
        Generator generator = network.getGeneratorStream()
                .filter(g -> g.getTargetP() > 0)
                .min(Comparator.comparingDouble(Generator::getTargetP))
                .orElseThrow();
        generator.setMinP(generator.getTargetP());

        initialize(network, new OpenReacParameters());

        // the generator takes its share of the mismatch below its minimum, as it did with the DCOPF
        assertTrue(-generator.getTerminal().getP() < generator.getMinP() - 1e-3);
    }

    @Test
    void testFailureWithoutGenerator() {
        Network network = IeeeCdfNetworkFactory.create14();
        network.getGeneratorStream().map(Generator::getId).toList().forEach(id -> network.getGenerator(id).remove());

        PowsyblException e = assertThrows(PowsyblException.class, () -> initialize(network, new OpenReacParameters()));
        assertTrue(e.getMessage().startsWith("DC load flow initialization"), e.getMessage());
    }

    @Test
    void testNeutralVoltages() {
        Network network = IeeeCdfNetworkFactory.create14();

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL));

        network.getBusView().getBuses().forEach(b -> assertEquals(b.getVoltageLevel().getNominalV(), b.getV(), DELTA));
        assertTrue(network.getBusView().getBusStream().anyMatch(b -> Math.abs(b.getAngle()) > 1));
    }

    @Test
    void testNeutralTransformers() {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        RatioTapChanger variable = network.getTwoWindingsTransformer("T2wT1").getRatioTapChanger();
        RatioTapChanger fixed = network.getTwoWindingsTransformer("T2wT2").getRatioTapChanger();
        assertEquals(0.9, variable.getCurrentStep().getRho());
        assertEquals(0.9, fixed.getCurrentStep().getRho());
        OpenReacParameters parameters = new OpenReacParameters()
                .setReferenceState(ReferenceState.NEUTRAL)
                .addVariableTwoWindingsTransformers(List.of("T2wT1"));

        initialize(network, parameters);

        assertEquals(1, variable.getCurrentStep().getRho());
        assertEquals(0.9, fixed.getCurrentStep().getRho());
        assertEquals(network.getTwoWindingsTransformer("T2wT1").getTerminal1().getVoltageLevel().getNominalV(),
                network.getTwoWindingsTransformer("T2wT1").getTerminal1().getBusView().getBus().getV(), DELTA);
    }

    @Test
    void testTransformerWithOneSideOpenedIsNotNeutralized() {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer("T2wT1");
        t2wt.getTerminal2().disconnect();

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL).addVariableTwoWindingsTransformers(List.of("T2wT1")));

        // the ratio of a transformer opened on one side is a data of the ACOPF, not a variable
        assertEquals(0.9, t2wt.getRatioTapChanger().getCurrentStep().getRho());
    }

    @Test
    void testZeroImpedanceTransformerIsNotNeutralized() {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        // below the default Znull (1e-4 pu on the 33kV side): a fixed-ratio branch for the AMPL model
        TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer("T2wT1").setR(0).setX(0.001);

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL).addVariableTwoWindingsTransformers(List.of("T2wT1")));

        assertEquals(0.9, t2wt.getRatioTapChanger().getCurrentStep().getRho());
    }

    @Test
    void testTransformerBelowNominalVoltageFilterIsNotNeutralized() {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer("T2wT1");
        // the 33kV side is discarded by the AMPL model: the transformer is not in its main synchronous component
        OpenReacParameters parameters = new OpenReacParameters()
                .setReferenceState(ReferenceState.NEUTRAL)
                .setMinNominalVoltageIgnoredBus(50)
                .addVariableTwoWindingsTransformers(List.of("T2wT1"));

        initialize(network, parameters);

        assertEquals(0.9, t2wt.getRatioTapChanger().getCurrentStep().getRho());
    }

    @Test
    void testNeutralTransformerWithoutNeutralStep() {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer("T2wT1");
        // steps at 0.9, 1.0, 1.05, 1.1 rebuilt at 0.9, 0.98, 1.05, 1.1: the closest to 1 is the second one
        t2wt.getRatioTapChanger().remove();
        t2wt.newRatioTapChanger().setTapPosition(3)
                .beginStep().setRho(0.9).endStep()
                .beginStep().setRho(0.98).endStep()
                .beginStep().setRho(1.05).endStep()
                .beginStep().setRho(1.1).endStep()
                .add();
        assertTrue(t2wt.getRatioTapChanger().getNeutralPosition().isEmpty());

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL).addVariableTwoWindingsTransformers(List.of("T2wT1")));

        assertEquals(1, t2wt.getRatioTapChanger().getTapPosition());
    }

    @Test
    void testNeutralShuntCompensators() {
        Network linear = ShuntNetworkFactory.createWithLinearModel();
        ShuntCompensator linearShunt = linear.getShuntCompensator("SHUNT").setSectionCount(10);
        Network nonLinear = ShuntNetworkFactory.createWithNonLinearModel();
        ShuntCompensator nonLinearShunt = nonLinear.getShuntCompensator("SHUNT").setSectionCount(2);
        OpenReacParameters parameters = new OpenReacParameters()
                .setReferenceState(ReferenceState.NEUTRAL)
                .addVariableShuntCompensators(List.of("SHUNT"));

        initialize(linear, parameters);
        initialize(nonLinear, parameters);

        assertEquals(0, linearShunt.getSectionCount());
        assertEquals(2, nonLinearShunt.getSectionCount());
    }

    @Test
    void testNeutralVscConverterStations() {
        Network network = HvdcNetworkFactory.createVsc();
        VscConverterStation inMainComponent = network.getVscConverterStation("cs2");
        VscConverterStation outOfMainComponent = network.getVscConverterStation("cs3");
        double outOfMainComponentSetpoint = outOfMainComponent.getReactivePowerSetpoint();
        assertEquals(100, inMainComponent.getReactivePowerSetpoint());
        inMainComponent.newMinMaxReactiveLimits().setMinQ(-50).setMaxQ(150).add();
        // the ACOPF optimizes the reactive power of a VSC converter station whether it regulates voltage or not
        inMainComponent.setVoltageRegulatorOn(false);

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL));

        assertEquals(50, inMainComponent.getReactivePowerSetpoint(), DELTA);
        assertEquals(outOfMainComponentSetpoint, outOfMainComponent.getReactivePowerSetpoint());
    }

    @Test
    void testNeutralVscConverterStationWithReactiveCapabilityCurve() {
        Network network = HvdcNetworkFactory.createVsc();
        VscConverterStation regulating = network.getVscConverterStation("cs2");
        // the middle of the envelope of the curve, not of its section at any active power
        regulating.newReactiveCapabilityCurve()
                .beginPoint().setP(-100).setMinQ(-30).setMaxQ(30).endPoint()
                .beginPoint().setP(0).setMinQ(-80).setMaxQ(120).endPoint()
                .beginPoint().setP(100).setMinQ(-30).setMaxQ(30).endPoint()
                .add();

        initialize(network, new OpenReacParameters().setReferenceState(ReferenceState.NEUTRAL));

        assertEquals(20, regulating.getReactivePowerSetpoint(), DELTA);
    }

    private static String initialize(Network network, OpenReacParameters parameters) {
        return AcopfInitializer.initialize(network, parameters, ReportNode.NO_OP);
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
