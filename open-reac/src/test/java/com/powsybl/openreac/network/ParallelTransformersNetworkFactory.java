/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.*;

/**
 * Networks created to test the parallel two-winding transformer detector
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class ParallelTransformersNetworkFactory {

    private ParallelTransformersNetworkFactory() {
        // utility class
    }

    /**
     * Two transformers in parallel between buses B1 (225 kV) and B2 (90 kV),
     * both with overlapping rho ranges -> one LARGE bundle of size 2
     */
    public static Network createSimpleParallel() {
        Network network = Network.create("simple-parallel", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        return network;
    }

    /**
     * Three transformers between the same pair of buses -> one LARGE bundle of size 3
     */
    public static Network createThreeParallel() {
        Network network = Network.create("three-parallel", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T3", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        return network;
    }

    /**
     * Triangle A-B-C inside one substation. T1 (A 225 kV - C 90 kV) and T2 (B 225 kV - C 90 kV)
     * are both 225/90 transformers sharing bus C; the cycle is closed by a 225 kV line A-B.
     * The complex (chordless-cycle) detection must bundle T1 and T2, while the line — a different
     * nominal voltage pair, and not a ratio tap changer — is filtered out.
     */
    public static Network createTriangleCycle() {
        Network network = Network.create("triangle-cycle", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL_A", 225.0, "A");
        addBus(s, "VL_B", 225.0, "B");
        addBus(s, "VL_C", 90.0, "C");
        addRtcTransformer(s, "T1", "VL_A", "A", "VL_C", "C", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL_B", "B", "VL_C", "C", 225.0, 90.0, 0.95, 1.05);
        addLine(network, "L_AB", "VL_A", "A", "VL_B", "B");
        return network;
    }

    /**
     * Square A-B-C-D-A, with A and C at 225 kV and B and D at 90 kV
     * Each edge is a transformer 225/90 kV. No chord between A-C or B-D
     * -> one LARGE bundle of size 4
     */
    public static Network createSquareCycle() {
        Network network = Network.create("square-cycle", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL_HT_A", 225.0, "A");
        addBus(s, "VL_HT_C", 225.0, "C");
        addBus(s, "VL_BT_B", 90.0, "B");
        addBus(s, "VL_BT_D", 90.0, "D");
        addRtcTransformer(s, "T_AB", "VL_HT_A", "A", "VL_BT_B", "B", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T_BC", "VL_BT_B", "B", "VL_HT_C", "C", 90.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T_CD", "VL_HT_C", "C", "VL_BT_D", "D", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T_DA", "VL_BT_D", "D", "VL_HT_A", "A", 90.0, 225.0, 0.95, 1.05);
        return network;
    }

    /**
     * Two transformers between B1-B2 (bundle 1) and two transformers between B2-B3 (bundle 2)
     * Pure topological detection: bundles are NOT merged because they don't share
     * a transformer — only a bus
     */
    public static Network createTwoSeparateBundles() {
        Network network = Network.create("two-separate-bundles", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        addBus(s, "VL3", 20.0, "B3");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T3", "VL2", "B2", "VL3", "B3", 90.0, 20.0, 0.95, 1.05);
        addRtcTransformer(s, "T4", "VL2", "B2", "VL3", "B3", 90.0, 20.0, 0.95, 1.05);
        return network;
    }

    /**
     * Two transformers between same bus pair, but only one has a ratio tap changer
     * -> no bundle (RTC filtering brings the count below 2)
     */
    public static Network createOneRtcOneFixed() {
        Network network = Network.create("one-rtc-one-fixed", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addFixedTransformer(s, "T2", "VL1", "B1", "VL2", "B2", 225.0, 90.0);
        return network;
    }

    /**
     * Two physically parallel transformers between B1 (225 kV) and B2 (90 kV), but T2 is
     * declared with swapped terminals (90 kV side first). Legal in IIDM; the detector must
     * flag T2 as reversed relative to T1
     */
    public static Network createAntiParallel() {
        Network network = Network.create("anti-parallel", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 90.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL2", "B2", "VL1", "B1", 90.0, 225.0, 0.95, 1.05);
        return network;
    }

    /**
     * Two coupling transformers (both sides at 225 kV) between the same pair of buses,
     * T2 declared with swapped terminals. Nominal voltage cannot orient a degenerate
     * pair; the shared bus pair can
     */
    public static Network createEqualVoltageAntiParallel() {
        Network network = Network.create("equal-voltage-anti-parallel", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 225.0, "B2");
        addRtcTransformer(s, "T1", "VL1", "B1", "VL2", "B2", 225.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL2", "B2", "VL1", "B1", 225.0, 225.0, 0.95, 1.05);
        return network;
    }

    /**
     * Mixed-orientation triangle mirroring a real autotransformer group: T1 declared
     * HV(400)->LV(225) between A and B; T2 and T3 declared LV(225)->HV(400) between B and
     * another 400 kV bus C; loop closed by a 400 kV line A-C. The detector must bundle the
     * three and flag T2 and T3 as reversed relative to T1 (the reference)
     */
    public static Network createAntiParallelTriangle() {
        Network network = Network.create("anti-parallel-triangle", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL_A", 400.0, "A");
        addBus(s, "VL_B", 225.0, "B");
        addBus(s, "VL_C", 400.0, "C");
        addRtcTransformer(s, "T1", "VL_A", "A", "VL_B", "B", 400.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T2", "VL_B", "B", "VL_C", "C", 225.0, 400.0, 0.95, 1.05);
        addRtcTransformer(s, "T3", "VL_B", "B", "VL_C", "C", 225.0, 400.0, 0.95, 1.05);
        addLine(network, "L_AC", "VL_A", "A", "VL_C", "C");
        return network;
    }

    /**
     * Square A-B-C-D-A of coupling transformers, every voltage level at 225 kV.
     * Degenerate nominal voltage pair and no shared bus pair: the orientation of the
     * members is undecidable -> the bundle must be relaxed and reported
     */
    public static Network createEqualVoltageSquareCycle() {
        Network network = Network.create("equal-voltage-square-cycle", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL_A", 225.0, "A");
        addBus(s, "VL_B", 225.0, "B");
        addBus(s, "VL_C", 225.0, "C");
        addBus(s, "VL_D", 225.0, "D");
        addRtcTransformer(s, "T_AB", "VL_A", "A", "VL_B", "B", 225.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T_BC", "VL_B", "B", "VL_C", "C", 225.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T_CD", "VL_C", "C", "VL_D", "D", 225.0, 225.0, 0.95, 1.05);
        addRtcTransformer(s, "T_DA", "VL_D", "D", "VL_A", "A", 225.0, 225.0, 0.95, 1.05);
        return network;
    }

    /**
     * No transformer at all -> empty result
     */
    public static Network createNoTransformer() {
        Network network = Network.create("no-transformer", "test");
        Substation s = network.newSubstation().setId("S").add();
        addBus(s, "VL1", 225.0, "B1");
        addBus(s, "VL2", 90.0, "B2");
        return network;
    }

    // --- Helpers ---

    private static void addBus(Substation s, String vlId, double nominalV, String busId) {
        VoltageLevel vl = s.newVoltageLevel()
                .setId(vlId)
                .setNominalV(nominalV)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl.getBusBreakerView().newBus().setId(busId).add();
    }

    private static void addLine(Network network, String id,
                                String vl1, String bus1, String vl2, String bus2) {
        network.newLine()
                .setId(id)
                .setVoltageLevel1(vl1).setBus1(bus1).setConnectableBus1(bus1)
                .setVoltageLevel2(vl2).setBus2(bus2).setConnectableBus2(bus2)
                .setR(0.1).setX(1.0).setG1(0).setB1(0).setG2(0).setB2(0)
                .add();
    }

    private static void addRtcTransformer(Substation s, String id,
                                          String vl1, String bus1, String vl2, String bus2,
                                          double ratedU1, double ratedU2,
                                          double rhoMin, double rhoMax) {
        TwoWindingsTransformer twt = s.newTwoWindingsTransformer()
                .setId(id)
                .setVoltageLevel1(vl1).setBus1(bus1).setConnectableBus1(bus1)
                .setVoltageLevel2(vl2).setBus2(bus2).setConnectableBus2(bus2)
                .setRatedU1(ratedU1).setRatedU2(ratedU2)
                .setR(0.1).setX(1.0).setG(0).setB(0)
                .add();

        twt.newRatioTapChanger()
                .beginStep().setRho(rhoMin).setR(0).setX(0).setG(0).setB(0).endStep()
                .beginStep().setRho((rhoMin + rhoMax) / 2.0).setR(0).setX(0).setG(0).setB(0).endStep()
                .beginStep().setRho(rhoMax).setR(0).setX(0).setG(0).setB(0).endStep()
                .setTapPosition(1)
                .setLoadTapChangingCapabilities(true)
                .add();
    }

    private static void addFixedTransformer(Substation s, String id,
                                            String vl1, String bus1, String vl2, String bus2,
                                            double ratedU1, double ratedU2) {
        s.newTwoWindingsTransformer()
                .setId(id)
                .setVoltageLevel1(vl1).setBus1(bus1).setConnectableBus1(bus1)
                .setVoltageLevel2(vl2).setBus2(bus2).setConnectableBus2(bus2)
                .setRatedU1(ratedU1).setRatedU2(ratedU2)
                .setR(0.1).setX(1.0).setG(0).setB(0)
                .add();
    }
}
