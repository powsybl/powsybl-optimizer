/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.optimization;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Nicolas PIERRE {@literal <nicolas.pierre at artelys.com>}
 */
abstract class AbstractOpenReacRunnerTest {
    protected FileSystem fileSystem;
    protected Path tmpDir;

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = createDirectory(fileSystem.getPath("tmp"));
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    protected void assertEqualsToRef(Path p, String refFileName) throws IOException {
        try (InputStream actual = newInputStream(p)) {
            ComparisonUtils.assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(refFileName)), actual);
        }
    }

    protected Path getAmplExecPath() throws IOException {
        Path execFolder;
        try (Stream<Path> walk = walk(tmpDir)) {
            execFolder = walk.limit(2).collect(Collectors.toList()).get(1);
        }
        return execFolder;
    }

    /**
     * Runs OpenReac, apply the results and verify that a load flow converges on these results.
     */
    protected void testAllModifAndLoadFlow(Network network, String subFolder, OpenReacParameters parameters, ReportNode reportNode) throws IOException {
        runAndApplyAllModifications(network, subFolder, parameters, true, reportNode);

        LoadFlowParameters lfParams = new LoadFlowParameters();
        OpenLoadFlowParameters olfParams = new OpenLoadFlowParameters()
                .setSlackDistributionFailureBehavior(OpenLoadFlowParameters.SlackDistributionFailureBehavior.LEAVE_ON_SLACK_BUS);
        lfParams.addExtension(OpenLoadFlowParameters.class, olfParams);
        LoadFlowResult loadFlowResult = LoadFlow.run(network, lfParams);
        assertTrue(loadFlowResult.isFullyConverged());
    }

    /**
     * Runs OpenReac and apply the results on the network.
     * The application of the voltage plan calculated by optimization is optional.
     */
    protected void runAndApplyAllModifications(Network network, String subFolder, OpenReacParameters parameters,
                                               boolean updateNetworkWithVoltages, ReportNode reportNode) throws IOException {
        OpenReacResult openReacResult = runOpenReac(network, subFolder, parameters, false, reportNode);
        assertEquals(OpenReacStatus.OK, openReacResult.getStatus());
        openReacResult.setUpdateNetworkWithVoltages(updateNetworkWithVoltages);
        openReacResult.applyAllModifications(network);
    }

    /**
     * Runs OpenReac and returns associated result.
     */
    protected OpenReacResult runOpenReac(Network network, String subFolder) throws IOException {
        return runOpenReac(network, subFolder, false);
    }

    /**
     * Runs OpenReac and returns associated result.
     */
    protected OpenReacResult runOpenReac(Network network, String subFolder, boolean onlyIndicators) throws IOException {
        return runOpenReac(network, subFolder, new OpenReacParameters(), onlyIndicators);
    }

    /**
     * Runs OpenReac and returns associated result.
     */
    protected OpenReacResult runOpenReac(Network network, String subFolder, OpenReacParameters parameters, boolean onlyIndicators) throws IOException {
        return runOpenReac(network, subFolder, parameters, onlyIndicators, ReportNode.NO_OP);
    }

    /**
     * Runs OpenReac and returns associated result.
     * Note that OpenReac is not really executed by default. If the execution line is not uncommented,
     * the results are retrieved and stored in an {@link OpenReacResult} object.
     */
    protected OpenReacResult runOpenReac(Network network, String subFolder, OpenReacParameters parameters,
                                         boolean onlyIndicators, ReportNode reportNode) throws IOException {
        // set default voltage limits to every voltage levels of the network
        setDefaultVoltageLimits(network);
        List<String> outputFileNames = new ArrayList<>(List.of(subFolder + "/reactiveopf_results_indic.txt"));
        if (!onlyIndicators) {
            outputFileNames.addAll(List.of(
                    subFolder + "/reactiveopf_results_generators.csv",
                    subFolder + "/reactiveopf_results_rtc.csv",
                    subFolder + "/reactiveopf_results_shunts.csv",
                    subFolder + "/reactiveopf_results_static_var_compensators.csv",
                    subFolder + "/reactiveopf_results_vsc_converter_stations.csv",
                    subFolder + "/reactiveopf_results_voltages.csv"
            ));
        }
        LocalCommandExecutor localCommandExecutor = new TestLocalCommandExecutor(outputFileNames);
        // To really run open reac, use the commentede line below. Be sure that open-reac/src/test/resources/com/powsybl/config/test/config.yml contains your ampl path
//        try (ComputationManager computationManager = new LocalComputationManager()) {
        try (ComputationManager computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir),
                localCommandExecutor, ForkJoinPool.commonPool())) {
            return OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), parameters,
                    new OpenReacConfig(true), computationManager, reportNode, null);
        }
    }

    protected void setDefaultVoltageLimits(Network network) {
        setDefaultVoltageLimits(network, 0.5, 1.5);
    }

    /**
     * Add voltage limits to voltage levels with undefined limits.
     * OpenReac needs voltage limits to run optimization.
     */
    protected void setDefaultVoltageLimits(Network network, double thresholdMin, double thresholdMax) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(thresholdMin * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(thresholdMax * vl.getNominalV());
            }
        }
    }
}
