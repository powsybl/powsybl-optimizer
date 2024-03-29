/**
 * Copyright (c) 2022,2023,2024 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.divergenceanalyser.network.VoltageControlNetworkFactory;
import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class DivergenceAnalyserRunTest {

    protected FileSystem fileSystem;
    protected Path tmpDir;

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = Files.createDirectory(fileSystem.getPath("tmp"));
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    void runDivergenceAnalyserAndVerifyResults(Network network, DivergenceAnalyserParameters parameters, String subFolder, List<Pair<String, String>> indicators) throws IOException {
        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(
                List.of(subFolder + "/da_penal_indic.txt",
                        subFolder + "/da_network_indic.txt",
                        subFolder + "/da_indic.txt",
                        subFolder + "/da_branch_penal.csv",
                        subFolder + "/da_bus_penal.csv",
                        subFolder + "/da_V_theta.csv"));

        // To really run the divergence analyser, use the commented line below. Be sure that divergence-analyser/src/test/resources/com/powsybl/config/test/config.yml contains your ampl path
        try (ComputationManager computationManager = new LocalComputationManager()) {
//        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
//                localCommandExecutor, ForkJoinPool.commonPool())) {

            // Run DA without penal activated
            DivergenceAnalyserResults results = DivergenceAnalyser.runDivergenceAnalysis(network, network.getVariantManager().getWorkingVariantId(),
                    parameters, new DivergenceAnalyserConfig(true), computationManager);

            // Verify network indicators
            assertEquals(results.getNetworkIndicators(), indicators);
            assertEquals("OK", results.getRunIndicators().get(1).getSecond());
            assertEquals("NO", results.getRunIndicators().get(3).getSecond());
            assertEquals(0, results.getBusPenalization().size());
            assertEquals(0, results.getBranchPenalization().size());
        }
    }

    @Test
    void testNetworkIEEE14() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        List<Pair<String, String>> indicators = List.of(
                Pair.of("num_main_cc", "0"),
                Pair.of("num_buses", "14"),
                Pair.of("num_PV_buses", "5"),
                Pair.of("num_branches", "20"),
                Pair.of("num_rtc", "0"),
                Pair.of("num_pst", "0"),
                Pair.of("num_3wt", "0"));

        // verify results with and without penal
        runDivergenceAnalyserAndVerifyResults(network, parameters, "da-ieee14", indicators);
        parameters.setAllPenalization(true);
        runDivergenceAnalyserAndVerifyResults(network, parameters, "da-ieee14", indicators);
    }

    @Test
    void testSmallNetwork() throws IOException {
        Network network = VoltageControlNetworkFactory.createWithShuntAndGeneratorVoltageControl();
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        List<Pair<String, String>> indicators = List.of(
                Pair.of("num_main_cc", "0"),
                Pair.of("num_buses", "3"),
                Pair.of("num_PV_buses", "2"),
                Pair.of("num_branches", "2"),
                Pair.of("num_rtc", "0"),
                Pair.of("num_pst", "0"),
                Pair.of("num_3wt", "0"));

        // verify results with and without penal
        runDivergenceAnalyserAndVerifyResults(network, parameters, "small-network", indicators);
        parameters.setAllPenalization(true);
        runDivergenceAnalyserAndVerifyResults(network, parameters, "small-network", indicators);
    }

    @Test
    void testSmallNetworkWith3wt() throws IOException {
        Network network = VoltageControlNetworkFactory.createNetworkWithT3wt();
        DivergenceAnalyserParameters parameters = new DivergenceAnalyserParameters();
        List<Pair<String, String>> indicators = List.of(
                Pair.of("num_main_cc", "0"),
                Pair.of("num_buses", "5"),
                Pair.of("num_PV_buses", "1"),
                Pair.of("num_branches", "4"),
                Pair.of("num_rtc", "1"),
                Pair.of("num_pst", "0"),
                Pair.of("num_3wt", "1"));

        // verify results with and without penal
        runDivergenceAnalyserAndVerifyResults(network, parameters, "small-network-3wt", indicators);
        parameters.setAllPenalization(true);
        runDivergenceAnalyserAndVerifyResults(network, parameters, "small-network-3wt", indicators);
    }

}
