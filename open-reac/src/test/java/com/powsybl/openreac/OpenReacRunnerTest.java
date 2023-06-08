/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
 */
class OpenReacRunnerTest {

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void test() throws IOException {
        Network network = IeeeCdfNetworkFactory.create118();

        OpenReacParameters parameters = new OpenReacParameters()
                .setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT)
                .setObjectiveDistance(70)
                .addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream().limit(1).map(TwoWindingsTransformer::getId).collect(Collectors.toList()))
                .addConstantQGenerators(network.getGeneratorStream().limit(1).map(Generator::getId).collect(Collectors.toList()))
                .addVariableShuntCompensators(network.getShuntCompensatorStream().limit(1).map(ShuntCompensator::getId).collect(Collectors.toList()));

        OpenReacConfig config = new OpenReacConfig(false);

        Path tmpDir = fileSystem.getPath("/work");

        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(List.of("reactiveopf_results_generators.csv",
                                                                                         "reactiveopf_results_indic.txt",
                                                                                         "reactiveopf_results_rtc.csv",
                                                                                         "reactiveopf_results_shunts.csv",
                                                                                         "reactiveopf_results_static_var_compensators.csv",
                                                                                         "reactiveopf_results_vsc_converter_stations.csv"));
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir), localCommandExecutor, ForkJoinPool.commonPool())) {
            OpenReacResult openReacResult = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters, config, computationManager);

            assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
            assertEquals(54, openReacResult.getGeneratorModifications().size());
            assertEquals(78, openReacResult.getIndicators().size());
            assertTrue(openReacResult.getReactiveSlacks().isEmpty());
        }
    }

}
