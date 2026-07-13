/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.VoltageRegulationAdder;

/**
 * Minimal test networks with a single battery.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class BatteryNetworkFactory {

    private BatteryNetworkFactory() {
    }

    public static Network create() {
        Network network = Network.create("battery-test", "test");
        Substation s = network.newSubstation()
                .setId("S")
                .add();
        VoltageLevel vl = s.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl.getBusBreakerView().newBus()
                .setId("BUS1")
                .add();
        vl.newBattery()
                .setId("BATTERY")
                .setBus("BUS1")
                .setConnectableBus("BUS1")
                .setMinP(-100.0)
                .setMaxP(100.0)
                .setTargetP(10.0)
                .setTargetQ(0.0)
                .add();
        return network;
    }

    /**
     * Same network, with voltage regulation enabled on the battery (local regulation).
     */
    public static Network createWithVoltageRegulationOn() {
        Network network = create();
        Battery battery = network.getBattery("BATTERY");
        battery.newExtension(VoltageRegulationAdder.class)
                .withVoltageRegulatorOn(true)
                .withTargetV(400.0)
                .withRegulatingTerminal(battery.getTerminal())
                .add();
        return network;
    }
}
