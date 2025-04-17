/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.optimization;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.VoltageControlNetworkFactory;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * In the optimization of ACOPF, the transformer tap ratios are continuous.
 * This can lead to optimized values that differ from those actually available in the discrete settings of the equipment.
 *
 * To correct this, the tap ratios can be rounded after an initial ACOPF optimization
 * and a second optimization can be performed to adjust the calculated voltage plan to these new tap ratios.
 *
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
 */
class OpenReac2ConsecutiveOptimizationsTest extends AbstractOpenReacRunnerTest {

    @Test
    void test2ConsecutiveOptimizationWithTransformerTapRounding() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT1"));
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG); // allows to get indicators of the optimization

        // run only one optimization
        OpenReacResult result = runOpenReac(network, "optimization/2-optimizations/option-not-activated/", parameters, true);
        assertFalse(Boolean.parseBoolean(result.getIndicators().get("optimization_after_rounding")));

        // verify only 1 optimization was conducted by comparing the numbers of iterations
        int totalNumberOfIterations = Integer.parseInt(result.getIndicators().get("nb_iter_total"));
        int numberOfIterationsOfLastAcopfRun = Integer.parseInt(result.getIndicators().get("nb_iter_last"));
        assertEquals(numberOfIterationsOfLastAcopfRun, totalNumberOfIterations);

        // allows for 2 consecutive optimizations
        parameters.setOptimizationAfterRounding(true);
        result = runOpenReac(network, "optimization/2-optimizations/option-activated/", parameters, true);
        assertTrue(Boolean.parseBoolean(result.getIndicators().get("optimization_after_rounding")));

        // verify the rtc has been optimized successfully
        assertEquals(OpenReacStatus.OK, result.getStatus());
        assertEquals(1, Integer.parseInt(result.getIndicators().get("nb_transformers_with_variable_ratio")));

        // verify that 2 optimizations were conducted by comparing the numbers of iterations
        totalNumberOfIterations = Integer.parseInt(result.getIndicators().get("nb_iter_total"));
        numberOfIterationsOfLastAcopfRun = Integer.parseInt(result.getIndicators().get("nb_iter_last"));
        assertTrue(numberOfIterationsOfLastAcopfRun < totalNumberOfIterations);
    }

    @Test
    void test2ConsecutiveOptimizationsOnlyIfAtLeastOneTransformerIsOptimized() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG); // allows to get indicators of the optimization

        // even if the option is activated, 2 optimizations are performed only if at least one transformer is optimized
        parameters.setOptimizationAfterRounding(true);
        OpenReacResult result = runOpenReac(network, "optimization/2-optimizations/no-tap-optimized/", parameters, true);
        assertTrue(Boolean.parseBoolean(result.getIndicators().get("optimization_after_rounding")));

        // verify only 1 optimization was conducted by comparing the numbers of iterations
        int totalNumberOfIterations = Integer.parseInt(result.getIndicators().get("nb_iter_total"));
        int numberOfIterationsOfLastAcopfRun = Integer.parseInt(result.getIndicators().get("nb_iter_last"));
        assertEquals(numberOfIterationsOfLastAcopfRun, totalNumberOfIterations);
    }

    @Test
    void test2ConsecutiveOptimizationOnlyIfFirstConverged() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWith2T2wt();
        // add a load with incoherent q0 to ensure divergence of optimization
        network.getVoltageLevel("VL_3").newLoad()
                .setId("LOAD_4")
                .setBus("BUS_3")
                .setP0(11.2)
                .setQ0(10000)
                .add();

        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(List.of("T2wT1"));
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED); // no reactive slack available
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG); // allows to get indicators of the optimization

        // even if the option is activated, 2 optimizations are performed only if the first one converged
        parameters.setOptimizationAfterRounding(true);
        OpenReacResult result = runOpenReac(network, "optimization/2-optimizations/no-convergence-1st-optim/", parameters, true);
        assertTrue(Boolean.parseBoolean(result.getIndicators().get("optimization_after_rounding")));

        // verify only 1 optimization was conducted by comparing the numbers of iterations
        int totalNumberOfIterations = Integer.parseInt(result.getIndicators().get("nb_iter_total"));
        int numberOfIterationsOfLastAcopfRun = Integer.parseInt(result.getIndicators().get("nb_iter_last"));
        assertEquals(numberOfIterationsOfLastAcopfRun, totalNumberOfIterations);
    }

}
