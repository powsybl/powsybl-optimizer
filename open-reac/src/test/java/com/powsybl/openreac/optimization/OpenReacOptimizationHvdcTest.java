/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.HvdcNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test OpenReac optimization with HVDC lines.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
public class OpenReacOptimizationHvdcTest extends AbstractOpenReacRunnerTest {

    @Test
    void testActivePowerSetpointLowerThanPmax() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();
        // put max P < active set point to fail checks before optimization
        network.getVscConverterStation("cs2").getHvdcLine().setMaxP(1);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testLccExcludedFromOptimization() throws IOException {
        Network network = HvdcNetworkFactory.createLcc();
        OpenReacParameters parameters = new OpenReacParameters();
        network.getLccConverterStation("cs2")
                .getHvdcLine()
                // put active set point > PQmax to remove LCC converter station from optimization
                .setActivePowerSetpoint(parameters.getPQMax() + 1)
                // modify max P to avoid check failure before optimization
                .setMaxP(Double.MAX_VALUE);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_lcc_converter_in_data_file")));
        // verify no lcc is considered in optimization
        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_lcc_converter_up_and_running")));
    }

    @Test
    void testVscExcludedFromOptimization() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();
        OpenReacParameters parameters = new OpenReacParameters();
        network.getVscConverterStation("cs2")
                .getHvdcLine()
                // put active set point > PQmax to remove VSC converter station from optimization
                .setActivePowerSetpoint(parameters.getPQMax() + 1)
                // modify max P to avoid check failure before optimization
                .setMaxP(parameters.getPQMax() + 2);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_in_data_file")));
        // verify no vsc is considered in optimization
        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_up_and_running")));
    }

    @Test
    void testVscExcludedFromOptimization2() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();
        OpenReacParameters parameters = new OpenReacParameters();
        network.getVscConverterStation("cs2")
                .getHvdcLine()
                // put max P > PQmax to remove vsc from optimization
                .setMaxP(parameters.getPQMax() + 1);
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);

        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(2, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_in_data_file")));
        // verify no vsc is considered in optimization
        assertEquals(0, Integer.parseInt(result.getIndicators().get("nb_vsc_converter_up_and_running")));
    }

    @Test
    void testVscActiveSetPointSignInOptimization() throws IOException {
        Network network = HvdcNetworkFactory.createVsc();
        // verify active power equilibrium if cs2 is rectifier
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());

        // verify there is no equilibrium if cs2 is inverter
        network.getHvdcLine("hvdc23").setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        result = runOpenReac(network, "optimization/indicators/lcc-test", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }

    @Test
    void testLccActiveSetPointSignInOptimization() throws IOException {
        Network network = HvdcNetworkFactory.createLcc();
        // verify active power equilibrium if cs2 is rectifier
        OpenReacResult result = runOpenReac(network, "optimization/indicators/lcc-test", true);
        assertEquals(OpenReacStatus.OK, result.getStatus());

        // verify there is no equilibrium if cs2 is inverter
        network.getHvdcLine("hvdc23").setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        result = runOpenReac(network, "optimization/indicators/lcc-test", true);
        assertEquals(OpenReacStatus.NOT_OK, result.getStatus());
    }
}
