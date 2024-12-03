/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenReacVoltageInitializationTest {

    final double deltaVoltage = 1e-3;
    OpenReacParameters openReacParameters;
    Network network;
    Bus b1;
    Bus b2;
    Bus b3;

    @BeforeEach
    void setUp() {
        openReacParameters = new OpenReacParameters();
        network = VoltageControlNetworkFactory.createNetworkWithT2wt();
        b1 = network.getBusBreakerView().getBus("BUS_1");
        b2 = network.getBusBreakerView().getBus("BUS_2");
        b3 = network.getBusBreakerView().getBus("BUS_3");
    }

    @Test
    void testDcVoltageInitialization() {
        openReacParameters.setVoltageInitialization(OpenReacParameters.OpenReacVoltageInitialization.DC_VALUES);
        OpenReacRunner.initializeVoltageBeforeOptimization(network, openReacParameters);
        assertEquals(Double.NaN, b1.getV());
        assertEquals(0.822, b1.getAngle(), deltaVoltage);
        assertEquals(Double.NaN, b2.getV());
        assertEquals(0, b2.getAngle(), deltaVoltage);
        assertEquals(Double.NaN, b3.getV());
        assertEquals(-2.923, b3.getAngle(), deltaVoltage);
    }

    @Test
    void testUniformVoltageInitialization() {
        openReacParameters.setVoltageInitialization(OpenReacParameters.OpenReacVoltageInitialization.UNIFORM_VALUES);
        OpenReacRunner.initializeVoltageBeforeOptimization(network, openReacParameters);
        assertEquals(b1.getVoltageLevel().getNominalV(), b1.getV());
        assertEquals(0, b1.getAngle());
        assertEquals(b2.getVoltageLevel().getNominalV(), b2.getV());
        assertEquals(0, b2.getAngle());
        assertEquals(b3.getVoltageLevel().getNominalV(), b3.getV());
        assertEquals(0, b3.getAngle());
    }

    @Test
    void testFullVoltageInitialization() {
        openReacParameters.setVoltageInitialization(OpenReacParameters.OpenReacVoltageInitialization.FULL_VOLTAGE);
        OpenReacRunner.initializeVoltageBeforeOptimization(network, openReacParameters);
        assertEquals(135, b1.getV(), deltaVoltage);
        assertEquals(0.822, b1.getAngle(), deltaVoltage);
        assertEquals(135, b2.getV(), deltaVoltage);
        assertEquals(0, b2.getAngle(), deltaVoltage);
        assertEquals(30.375, b3.getV(), deltaVoltage);
        assertEquals(-2.923, b3.getAngle(), deltaVoltage);
    }
}
