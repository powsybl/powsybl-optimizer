/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.regulation.RegulationMode;

/**
 * @author Anne Tilloy {@literal <anne.tilloy@rte-france.com>}
 */
public class VoltageControlNetworkFactory extends AbstractLoadFlowNetworkFactory {

    /**
     *   g1     g2      g3
     *   |      |       |
     *  b1      b2      b3
     *  |       |       |
     *  8 tr1   8 tr2   8 tr3
     *  |       |       |
     *  +------ b4 -----+
     *          |
     *          l4
     *
     * Creates a network where a generator regulates the voltage of a remote bus.
     *
     * @return the created network
     */
    public static Network createWithGeneratorRemoteControl() {

        Network network = Network.create("generator-remote-control-test", "code");

        Substation s = network.newSubstation()
                .setId("s")
                .add();
        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("vl1")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b1 = vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("vl2")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b2 = vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        VoltageLevel vl3 = s.newVoltageLevel()
                .setId("vl3")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b3 = vl3.getBusBreakerView().newBus()
                .setId("b3")
                .add();
        VoltageLevel vl4 = s.newVoltageLevel()
                .setId("vl4")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b4 = vl4.getBusBreakerView().newBus()
                .setId("b4")
                .add();
        Load l4 = vl4.newLoad()
                .setId("l4")
                .setBus("b4")
                .setConnectableBus("b4")
                .setP0(299.6)
                .setQ0(200)
                .add();
        b1.getVoltageLevel()
                .newGenerator()
                .setId("g1")
                .setBus("b1")
                .setConnectableBus("b1")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        b2.getVoltageLevel()
                .newGenerator()
                .setId("g2")
                .setBus("b2")
                .setConnectableBus("b2")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        b3.getVoltageLevel()
                .newGenerator()
                .setId("g3")
                .setBus("b3")
                .setConnectableBus("b3")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr1")
                .setBus1(b1.getId())
                .setConnectableBus1(b1.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr2")
                .setBus1(b2.getId())
                .setConnectableBus1(b2.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.2)
                .setRatedU2(398)
                .setR(1)
                .setX(36)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr3")
                .setBus1(b3.getId())
                .setConnectableBus1(b3.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(21.3)
                .setRatedU2(397)
                .setR(2)
                .setX(50)
                .add();

        return network;
    }

    /**
     * Creates a network with identical transformers.
     *
     * @return the created network
     */
    public static Network createWithIdenticalTransformers() {

        Network network = Network.create("generator-remote-control-test", "code");

        Substation s = network.newSubstation()
                .setId("s")
                .add();
        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("vl1")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b1 = vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("vl2")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b2 = vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        VoltageLevel vl3 = s.newVoltageLevel()
                .setId("vl3")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b3 = vl3.getBusBreakerView().newBus()
                .setId("b3")
                .add();
        VoltageLevel vl4 = s.newVoltageLevel()
                .setId("vl4")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b4 = vl4.getBusBreakerView().newBus()
                .setId("b4")
                .add();
        Load l4 = vl4.newLoad()
                .setId("l4")
                .setBus("b4")
                .setConnectableBus("b4")
                .setP0(299.6)
                .setQ0(200)
                .add();
        b1.getVoltageLevel()
                .newGenerator()
                .setId("g1")
                .setBus("b1")
                .setConnectableBus("b1")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        b2.getVoltageLevel()
                .newGenerator()
                .setId("g2")
                .setBus("b2")
                .setConnectableBus("b2")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        b3.getVoltageLevel()
                .newGenerator()
                .setId("g3")
                .setBus("b3")
                .setConnectableBus("b3")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(413.4)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .add()
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr1")
                .setBus1(b1.getId())
                .setConnectableBus1(b1.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr2")
                .setBus1(b2.getId())
                .setConnectableBus1(b2.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr3")
                .setBus1(b3.getId())
                .setConnectableBus1(b3.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();

        return network;
    }

    /**
     * Creates the base network (without transformers) shared by the transformer test networks.
     *
     * @param id the network id
     * @return the created network
     */
    public static Network createTransformerBaseNetwork(String id) {

        Network network = Network.create(id, "test");

        Substation substation1 = network.newSubstation()
                .setId("SUBSTATION1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = substation1.newVoltageLevel()
                .setId("VL_1")
                .setNominalV(132.0)
                .setLowVoltageLimit(118.8)
                .setHighVoltageLimit(145.2)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("BUS_1")
                .add();
        vl1.newGenerator()
                .setId("GEN_1")
                .setBus("BUS_1")
                .setMinP(0.0)
                .setMaxP(140)
                .setTargetP(25)
                .setLocalTargetV(135)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withRegulating(true)
                        .add()
                .add();

        Substation substation = network.newSubstation()
                .setId("SUBSTATION")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl2 = substation.newVoltageLevel()
                .setId("VL_2")
                .setNominalV(132.0)
                .setLowVoltageLimit(118.8)
                .setHighVoltageLimit(145.2)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl2.getBusBreakerView().newBus()
                .setId("BUS_2")
                .add();
        vl2.newLoad()
                .setId("LOAD_2")
                .setBus("BUS_2")
                .setP0(11.2)
                .setQ0(7.5)
                .add();

        VoltageLevel vl3 = substation.newVoltageLevel()
                .setId("VL_3")
                .setNominalV(33.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(100)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl3.getBusBreakerView().newBus()
                .setId("BUS_3")
                .add();
        vl3.newLoad()
                .setId("LOAD_3")
                .setBus("BUS_3")
                .setQ0(0)
                .setP0(5)
                .add();

        network.newLine()
                .setId("LINE_12")
                .setBus1("BUS_1")
                .setBus2("BUS_2")
                .setR(1.05)
                .setX(10.0)
                .setG1(0.0000005)
                .add();

        return network;
    }

    /**
     * A very small network to test with a T2wt.
     *
     *     G1        LD2      LD3
     *     |    L12   |        |
     *     |  ------- |        |
     *     B1         B2      B3
     *                  \    /
     *                   T2WT
     *
     * Creates a network with a two-winding transformer with a ratio tap changer.
     *
     * @return the created network
     */
    public static Network createNetworkWithT2wt() {

        Network network = VoltageControlNetworkFactory.createTransformerBaseNetwork("two-winding-transformer-control");

        TwoWindingsTransformer t2wt = network.getSubstation("SUBSTATION").newTwoWindingsTransformer()
                .setId("T2wT")
                .setRatedU1(132.0)
                .setRatedU2(33.0)
                .setR(17.0)
                .setX(10.0)
                .setG(0.00573921028466483)
                .setB(0.000573921028466483)
                .setBus1("BUS_2")
                .setBus2("BUS_3")
                .add();

        t2wt.newRatioTapChanger()
                .beginStep()
                .setRho(0.9)
                .setR(0.1089)
                .setX(0.01089)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.0)
                .setR(0.121)
                .setX(0.0121)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.05)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.1)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(33.0)
                        .withRegulating(false)
                        .withTerminal(network.getLoad("LOAD_3").getTerminal())
                        .add()
                .add();

        return network;
    }

    /**
     * Creates a variant of the network with a two-winding transformer.
     *
     * @return the created network
     */
    public static Network createNetworkWithT2wt2() {

        Network network = VoltageControlNetworkFactory.createTransformerBaseNetwork("two-winding-transformer-control");

        TwoWindingsTransformer t2wt = network.getSubstation("SUBSTATION").newTwoWindingsTransformer()
                .setId("T2wT")
                .setRatedU1(132.0)
                .setRatedU2(33.0)
                .setR(17.0)
                .setX(10.0)
                .setG(0.00573921028466483)
                .setB(0.000573921028466483)
                .setBus1("BUS_2")
                .setBus2("BUS_3")
                .add();

        t2wt.newRatioTapChanger()
                .beginStep()
                .setRho(1.75)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.7)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.5)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.2)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.1)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.05)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.0)
                .setR(0.121)
                .setX(0.0121)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(0.9)
                .setR(0.1089)
                .setX(0.01089)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(33.0)
                        .withRegulating(false)
                        .withTerminal(network.getLoad("LOAD_3").getTerminal())
                        .add()
                .add();

        return network;
    }

    /**
     * A very small network to test with two T2wt.
     *
     *     G1        LD2        LD3
     *     |    L12   |  T2WT2   |
     *     |  ------- | /     \  |
     *     B1         B2       B3
     *                 \      /
     *                  T2WT1
     *
     * Creates a network with two two-winding transformers.
     *
     * @return the created network
     */
    public static Network createNetworkWith2T2wt() {

        Network network = VoltageControlNetworkFactory.createTransformerBaseNetwork("two-winding-transformer-control");

        TwoWindingsTransformer t2wt1 = network.getSubstation("SUBSTATION").newTwoWindingsTransformer()
                .setId("T2wT1")
                .setRatedU1(132.0)
                .setRatedU2(33.0)
                .setR(17.0)
                .setX(10.0)
                .setG(0.00573921028466483)
                .setB(0.000573921028466483)
                .setBus1("BUS_2")
                .setBus2("BUS_3")
                .add();

        t2wt1.newRatioTapChanger()
                .beginStep()
                .setRho(0.9)
                .setR(0.1089)
                .setX(0.01089)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.0)
                .setR(0.121)
                .setX(0.0121)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.05)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.1)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(33.0)
                        .withRegulating(false)
                        .withTerminal(network.getLoad("LOAD_3").getTerminal())
                        .add()
                .add();

        TwoWindingsTransformer t2wt2 = network.getSubstation("SUBSTATION").newTwoWindingsTransformer()
                .setId("T2wT2")
                .setRatedU1(132.0)
                .setRatedU2(33.0)
                .setR(17.0)
                .setX(10.0)
                .setG(0.00573921028466483)
                .setB(0.000573921028466483)
                .setBus1("BUS_2")
                .setBus2("BUS_3")
                .add();

        t2wt2.newRatioTapChanger()
                .beginStep()
                .setRho(0.9)
                .setR(0.1089)
                .setX(0.01089)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.0)
                .setR(0.121)
                .setX(0.0121)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.05)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                .setRho(1.1)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(33.0)
                        .withRegulating(false)
                        .withTerminal(network.getLoad("LOAD_3").getTerminal())
                        .add()
                .add();

        return network;
    }

    /**
     * A very small network to test with a T3wt.
     *
     *     G1        LD2        LD3
     *     |    L12   |          |
     *     |  ------- |          |
     *     B1         B2         B3
     *                  \        /
     *                leg1     leg2
     *                   \      /
     *                     T3WT
     *                      |
     *                     leg3
     *                      |
     *                      B4
     *                      |
     *                     LD4
     *
     * Creates a network with a three-winding transformer.
     *
     * @return the created network
     */
    public static Network createNetworkWithT3wt() {

        Network network = VoltageControlNetworkFactory.createTransformerBaseNetwork("two-winding-transformer-control");

        VoltageLevel vl4 = network.getSubstation("SUBSTATION").newVoltageLevel()
                .setId("VL_4")
                .setNominalV(10.0)
                .setLowVoltageLimit(5.0)
                .setHighVoltageLimit(15.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl4.getBusBreakerView().newBus()
                .setId("BUS_4")
                .add();

        vl4.newLoad()
                .setId("LOAD_4")
                .setBus("BUS_4")
                .setQ0(0)
                .setP0(5)
                .add();

        ThreeWindingsTransformer t3wt = network.getSubstation("SUBSTATION").newThreeWindingsTransformer()
                .setId("T3wT")
                .setRatedU0(200.0)
                .newLeg1()
                .setR(2.0)
                .setX(10.0)
                .setRatedU(130.0)
                .setConnectableBus("BUS_2")
                .setBus("BUS_2")
                .add()
                .newLeg2()
                .setR(2.0)
                .setX(10.0)
                .setRatedU(30.0)
                .setConnectableBus("BUS_3")
                .setBus("BUS_3")
                .add()
                .newLeg3()
                .setR(2.0)
                .setX(10.0)
                .setRatedU(10.0)
                .setConnectableBus("BUS_4")
                .setBus("BUS_4")
                .add()
                .add();

        t3wt.getLeg2().newRatioTapChanger()
                .beginStep()
                .setRho(0.9)
                .setR(0.1089)
                .setX(0.01089)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.0)
                .setR(0.121)
                .setX(0.0121)
                .setG(0.8264462809917356)
                .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                .setRho(1.1)
                .setR(0.1331)
                .setX(0.01331)
                .setG(0.9090909090909092)
                .setB(0.09090909090909092)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(33.0)
                        .withRegulating(false)
                        .withTerminal(network.getLoad("LOAD_3").getTerminal())
                        .add()
                .add();

        return network;
    }

    /**
     *
     *     G1        LD2           LD3
     *     |    L12   |   T2WT2    |
     *     |  ------- |  /     \   |
     *     B1         B2        B3
     *                  \      /
     *                    T2WT
     *
     * Creates a network where several transformers share the same remote voltage control.
     *
     * @return the created network
     */
    public static Network createWithTransformerSharedRemoteControl() {
        Network network = createNetworkWithT2wt();

        TwoWindingsTransformer t2wt2 = network.getSubstation("SUBSTATION")
                .newTwoWindingsTransformer()
                    .setId("T2wT2")
                    .setRatedU1(132.0)
                    .setRatedU2(33.0)
                    .setR(17.0)
                    .setX(10.0)
                    .setG(0.00573921028466483)
                    .setB(0.000573921028466483)
                    .setBus1("BUS_2")
                    .setBus2("BUS_3")
                .add();

        t2wt2.newRatioTapChanger()
                .beginStep()
                    .setRho(0.9)
                    .setR(0.1089)
                    .setX(0.01089)
                    .setG(0.8264462809917356)
                    .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                    .setRho(1.0)
                    .setR(0.121)
                    .setX(0.0121)
                    .setG(0.8264462809917356)
                    .setB(0.08264462809917356)
                .endStep()
                .beginStep()
                    .setRho(1.05)
                    .setR(0.1331)
                    .setX(0.01331)
                    .setG(0.9090909090909092)
                    .setB(0.09090909090909092)
                .endStep()
                .beginStep()
                    .setRho(1.1)
                    .setR(0.1331)
                    .setX(0.01331)
                    .setG(0.9090909090909092)
                    .setB(0.09090909090909092)
                .endStep()
                .setTapPosition(0)
                .setLoadTapChangingCapabilities(true)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(34.0)
                        .withRegulating(true)
                        .withTerminal(t2wt2.getTerminal2())
                        .withTargetDeadband(0)
                        .add()
                .add();

        TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer("T2wT");
        t2wt.getRatioTapChanger()
                .setTapPosition(0)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(34.0)
                        .withTargetDeadband(0)
                        .withRegulating(true)
                        .withTerminal(t2wt.getTerminal2())
                        .build();

        return network;
    }

    /**
     *   g1     SHUNT2  SHUNT3
     *   |      |       |
     *  b1      b2      b3
     *  |       |       |
     *  8 tr1   8 tr2   8 tr3
     *  |       |       |
     *  +------ b4 -----+
     *          |
     *          l4
     *
     * Creates a network where several shunts share the same remote voltage control.
     *
     * @return the created network
     */
    public static Network createWithShuntSharedRemoteControl() {

        Network network = Network.create("shunt-remote-control-test", "code");

        Substation s = network.newSubstation()
                .setId("s")
                .add();
        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("vl1")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b1 = vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("vl2")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b2 = vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        VoltageLevel vl3 = s.newVoltageLevel()
                .setId("vl3")
                .setNominalV(20)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b3 = vl3.getBusBreakerView().newBus()
                .setId("b3")
                .add();
        VoltageLevel vl4 = s.newVoltageLevel()
                .setId("vl4")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b4 = vl4.getBusBreakerView().newBus()
                .setId("b4")
                .add();
        Load l4 = vl4.newLoad()
                .setId("l4")
                .setBus("b4")
                .setConnectableBus("b4")
                .setP0(299.6)
                .setQ0(0)
                .add();
        b1.getVoltageLevel()
                .newGenerator()
                .setId("g1")
                .setBus("b1")
                .setConnectableBus("b1")
                .setEnergySource(EnergySource.THERMAL)
                .setMinP(0)
                .setMaxP(200)
                .setTargetP(100)
                .setLocalTargetV(21)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withRegulating(true)
                        .add()
                .add();
        b2.getVoltageLevel().newShuntCompensator()
                .setId("SHUNT2")
                .setBus("b2")
                .setConnectableBus("b2")
                .setSectionCount(0)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(400)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .withTargetDeadband(5.0)
                        .add()
                .newLinearModel()
                .setMaximumSectionCount(50)
                .setBPerSection(-1E-2)
                .setGPerSection(0.0)
                .add()
                .add();
        b3.getVoltageLevel().newShuntCompensator()
                .setId("SHUNT3")
                .setBus("b3")
                .setConnectableBus("b3")
                .setSectionCount(0)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(400)
                        .withRegulating(true)
                        .withTerminal(l4.getTerminal())
                        .withTargetDeadband(5.0)
                        .add()
                .newLinearModel()
                .setMaximumSectionCount(50)
                .setBPerSection(-1E-2)
                .setGPerSection(0.0)
                .add()
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr1")
                .setBus1(b1.getId())
                .setConnectableBus1(b1.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr2")
                .setBus1(b2.getId())
                .setConnectableBus1(b2.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();
        s.newTwoWindingsTransformer()
                .setId("tr3")
                .setBus1(b3.getId())
                .setConnectableBus1(b3.getId())
                .setBus2(b4.getId())
                .setConnectableBus2(b4.getId())
                .setRatedU1(20.5)
                .setRatedU2(399)
                .setR(1)
                .setX(30)
                .add();

        return network;
    }

    /**
     * SVC test case.
     *
     * g1        ld1
     * |          |
     * b1---------b2
     *      l1    |
     *           svc1
     *
     * Creates a network with a static var compensator.
     *
     * @return the created network
     */
    public static Network createWithStaticVarCompensator() {
        Network network = Network.create("svc", "test");
        Substation s1 = network.newSubstation()
                .setId("S1")
                .add();
        Substation s2 = network.newSubstation()
                .setId("S2")
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("vl1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        vl1.newGenerator()
                .setId("g1")
                .setConnectableBus("b1")
                .setBus("b1")
                .setTargetP(101.3664)
                .setLocalTargetV(390)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withRegulating(true)
                        .add()
                .setMinP(0)
                .setMaxP(150)
                .add();
        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("vl2")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        vl2.newLoad()
                .setId("ld1")
                .setConnectableBus("b2")
                .setBus("b2")
                .setP0(101)
                .setQ0(150)
                .add();
        vl2.newStaticVarCompensator()
                .setId("svc1")
                .setConnectableBus("b2")
                .setBus("b2")
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withRegulating(false)
                        .add()
                .setBmin(-0.008)
                .setBmax(0.008)
                .add();
        network.newLine()
                .setId("l1")
                .setBus1("b1")
                .setBus2("b2")
                .setR(1)
                .setX(3)
                .add();
        return network;
    }

    /**
     * l1 - b1 ================ b2 - g2
     *      ||                  |
     * g3 - b3 ---------------- b4 - g4 (regulating b1)
     *      |
     *      l3
     *
     * Creates a network with a simple remote voltage control.
     *
     * @return the created network
     */
    public static Network createWithSimpleRemoteControl() {
        Network network = Network.create("remoteControl", "code");
        Bus b1 = createBus(network, "b1");
        Bus b2 = createBus(network, "b2");
        Bus b3 = createBus(network, "b3");
        Bus b4 = createBus(network, "b4");
        createGenerator(b2, "g2", 2, 1);
        createGenerator(b3, "g3", 2, 1);
        createGenerator(b4, "g4", 2, 1);
        createLoad(b3, "l3", 5, 1);
        createLoad(b1, "l1", 1, 1);
        createLine(network, b1, b2, "l12", 0.0);
        createLine(network, b2, b4, "l24", 0.01);
        createLine(network, b4, b3, "l43", 0.01);
        createLine(network, b3, b1, "l31", 0.0);
        Generator g4Gen = network.getGenerator("g4");
        g4Gen.newVoltageRegulation()
                .withMode(RegulationMode.VOLTAGE)
                .withTargetValue(g4Gen.getRegulatingTargetV())
                .withRegulating(g4Gen.isRegulating())
                .withTerminal(network.getLoad("l1").getTerminal())
                .build(); // remote control.
        return network;
    }

    /**
     *     b2 - g2          l4                g5
     *     |                 |                |
     *     b1 ------------- b4 ------------- b5 - l5
     *     |
     *     b3 - g3
     *
     * Creates a network with two voltage controls.
     *
     * @return the created network
     */
    public static Network createWithTwoVoltageControls() {
        Network network = Network.create("twoVoltageControls", "code");
        Bus b1 = createBus(network, "b1");
        Bus b2 = createBus(network, "b2");
        Bus b3 = createBus(network, "b3");
        Bus b4 = createBus(network, "b4");
        Bus b5 = createBus(network, "b5");
        createGenerator(b2, "g2", 2, 1);
        createGenerator(b3, "g3", 2, 1);
        createGenerator(b5, "g5", 2, 1);
        createLoad(b4, "l4", 5, 1);
        createLoad(b5, "l5", 1, 1);
        createLine(network, b1, b2, "l12", 0.01);
        createLine(network, b1, b3, "l13", 0.01);
        createLine(network, b1, b4, "l14", 0.01);
        createLine(network, b4, b5, "l45", 0.01);
        Generator g2Gen = network.getGenerator("g2");
        g2Gen.newVoltageRegulation()
                .withMode(RegulationMode.VOLTAGE)
                .withTargetValue(g2Gen.getRegulatingTargetV())
                .withRegulating(g2Gen.isRegulating())
                .withTerminal(network.getLine("l12").getTerminal1())
                .build(); // remote control (g2 -> b1).
        Generator g3Gen = network.getGenerator("g3");
        g3Gen.newVoltageRegulation()
                .withMode(RegulationMode.VOLTAGE)
                .withTargetValue(g3Gen.getRegulatingTargetV())
                .withRegulating(g3Gen.isRegulating())
                .withTerminal(network.getLine("l13").getTerminal1())
                .build(); // remote control (g3 -> b1).
        return network;
    }

    /**
     *     b2 - g1          l1 + g3
     *     |                 |
     *     b1 ------------- b3
     *     |
     *     shunt1
     *
     * Creates a network with both a shunt and a generator voltage control.
     *
     * @return the created network
     */
    public static Network createWithShuntAndGeneratorVoltageControl() {
        Network network = Network.create("shuntAndGeneratorVoltageControl", "code");
        Bus b1 = createBus(network, "b1");
        Bus b2 = createBus(network, "b2");
        Bus b3 = createBus(network, "b3");
        createLine(network, b1, b2, "l12", 0.01);
        createLine(network, b1, b3, "l13", 0.01);
        createGenerator(b2, "g1", 2, 1);
        createLoad(b1, "l1", 2.5, 1);
        createGenerator(b3, "g3", 1, 1);
        b3.getVoltageLevel().newShuntCompensator() // hidden voltage control
                .setId("SHUNT")
                .setBus("b3")
                .setConnectableBus("b3")
                .setSectionCount(0)
                .newVoltageRegulation()
                        .withMode(RegulationMode.VOLTAGE)
                        .withTargetValue(1)
                        .withRegulating(true)
                        .withTargetDeadband(0.1)
                        .add()
                .newLinearModel()
                .setMaximumSectionCount(50)
                .setBPerSection(1E-3)
                .setGPerSection(0.0)
                .add()
                .add();
        Generator g1Gen = network.getGenerator("g1");
        g1Gen.newVoltageRegulation()
                .withMode(RegulationMode.VOLTAGE)
                .withTargetValue(g1Gen.getRegulatingTargetV())
                .withRegulating(g1Gen.isRegulating())
                .withTerminal(network.getLine("l12").getTerminal1())
                .build(); // remote control (g2 -> b1).
        return network;
    }
}
