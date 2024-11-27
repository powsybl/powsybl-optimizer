/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.optimization;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Nicolas PIERRE {@literal <nicolas.pierre at artelys.com>}
 */
public class OpenReacParametrizationTest extends AbstractOpenReacRunnerTest {

    @Test
    void testDefaultParamAlgoExport() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/openreac-input-algo-parameters/default.txt");
        }
    }

    @Test
    void testModifiedParamAlgoExport() throws IOException {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters()
                .setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE)
                .setObjectiveDistance(69)
                .setLogLevelAmpl(OpenReacAmplLogLevel.WARNING)
                .setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS)
                .setMinPlausibleLowVoltageLimit(0.7888)
                .setMaxPlausibleHighVoltageLimit(1.3455)
                .setReactiveSlackBusesMode(ReactiveSlackBusesMode.NO_GENERATION)
                .setActivePowerVariationRate(0.88)
                .setMinPlausibleActivePowerThreshold(0.45)
                .setLowImpedanceThreshold(1e-5)
                .setMinNominalVoltageIgnoredBus(2.)
                .setMinNominalVoltageIgnoredVoltageBounds(0.75)
                .setPQMax(3987.76)
                .setLowActivePowerDefaultLimit(12.32)
                .setHighActivePowerDefaultLimit(1452.66)
                .setDefaultQmaxPmaxRatio(0.24)
                .setDefaultMinimalQPRange(2.)
                .setDefaultVariableScalingFactor(1.1222)
                .setDefaultConstraintScalingFactor(0.7889)
                .setReactiveSlackVariableScalingFactor(0.2)
                .setTwoWindingTransformerRatioVariableScalingFactor(0.0045)
                .setShuntVariableScalingFactor(0.101);

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of("empty_case/reactiveopf_results_indic.txt"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager);
            Path execFolder = getAmplExecPath();
            assertEqualsToRef(execFolder.resolve("param_algo.txt"), "/openreac-input-algo-parameters/modified_param_algo.txt");
        }
    }

}
