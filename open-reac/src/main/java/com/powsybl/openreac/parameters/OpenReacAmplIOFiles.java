/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters;

import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.Reports;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector;
import com.powsybl.openreac.parameters.input.*;
import com.powsybl.openreac.parameters.input.algo.AlgorithmInput;
import com.powsybl.openreac.parameters.output.FixedParallelTransformersOutput;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;
import com.powsybl.openreac.parameters.output.VoltageProfileOutput;
import com.powsybl.openreac.parameters.output.network.NetworkModifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenReacAmplIOFiles will interface all inputs and outputs needed for OpenReac to the abstracted Ampl Executor.
 * <p>
 * The user of OpenReac should not see this class directly. One should use {@link OpenReacParameters} for inputs
 * and {@link OpenReacResult} for outputs.
 * However, when adding new inputs (outputs) to OpenReac, one must add {@link AmplInputFile} ({@link AmplOutputFile})
 * here through {@link OpenReacAmplIOFiles#getInputParameters} ({@link OpenReacAmplIOFiles#getOutputParameters})
 *
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class OpenReacAmplIOFiles implements AmplParameters {

    private final ConstantQGenerators constantQGenerators;
    private final VariableShuntCompensators variableShuntCompensators;
    private final VariableTwoWindingsTransformers variableTwoWindingsTransformers;
    private final AlgorithmInput algorithmParams;
    private final ReactiveSlackOutput reactiveSlackOutput;
    private final VoltageLevelLimitsOverrideInput voltageLimitsOverride;
    private final ConfiguredBusesWithReactiveSlack configuredReactiveSlackBuses;
    private final NetworkModifications networkModifications;
    private final VoltageProfileOutput voltageProfileOutput;
    private final boolean debug;
    private final String debugDir;
    private final AmplExportConfig amplExportConfig;
    private final ParallelTwoWindingsTransformersBundles parallelTwoWindingsTransformersBundles;
    private final FixedParallelTransformersOutput fixedParallelTransformersOutput;

    public OpenReacAmplIOFiles(OpenReacParameters params, AmplExportConfig amplExportConfig, Network network, boolean debug, ReportNode reportNode) {

        //inputs
        this.constantQGenerators = new ConstantQGenerators(params.getConstantQGenerators());
        this.variableShuntCompensators = new VariableShuntCompensators(params.getVariableShuntCompensators());
        this.variableTwoWindingsTransformers = new VariableTwoWindingsTransformers(params.getVariableTwoWindingsTransformers());
        this.algorithmParams = new AlgorithmInput(params.getAllAlgorithmParams());
        this.voltageLimitsOverride = new VoltageLevelLimitsOverrideInput(params.getSpecificVoltageLimits(), network, reportNode);
        this.configuredReactiveSlackBuses = new ConfiguredBusesWithReactiveSlack(params.getConfiguredReactiveSlackBuses());
        this.amplExportConfig = amplExportConfig;

        //outputs
        this.reactiveSlackOutput = new ReactiveSlackOutput();
        this.networkModifications = new NetworkModifications(network, params.getShuntCompensatorActivationAlertThreshold());
        this.voltageProfileOutput = new VoltageProfileOutput();

        this.debug = debug;
        this.debugDir = params.getDebugDir();

        // Parallel transformer bundles are detected topologically here; every orientable bundle is
        // sent to AMPL as a membership + orientation relation (every member, no classification). The
        // numeric qualification (tie / fix / release) and all bounds are derived in the AMPL model
        // from its own data, so the classification and the constraints can never disagree. Bundles
        // whose member orientation cannot be established (degenerate nominal-voltage pair in a cycle)
        // are not sent and are reported as released. Transformers that AMPL ends up fixing
        // (POINT/EMPTY bundles) are read back from fixedParallelTransformersOutput. The whole grouping
        // can be opted out through OpenReacParameters, in which case the detection is skipped and the
        // membership file is written header-only, a no-op for the AMPL model.
        ParallelTwoWindingsTransformersDetector.DetectionResult parallelDetection = params.isParallelTransformersGrouping()
                ? ParallelTwoWindingsTransformersDetector.detect(network)
                : new ParallelTwoWindingsTransformersDetector.DetectionResult(List.of(), List.of());
        this.parallelTwoWindingsTransformersBundles = new ParallelTwoWindingsTransformersBundles(parallelDetection.bundles());
        this.fixedParallelTransformersOutput = new FixedParallelTransformersOutput();
        Set<String> variableTransformerIds = new HashSet<>(params.getVariableTwoWindingsTransformers());
        Reports.reportParallelTwoWindingsTransformers(reportNode, parallelDetection.bundles(), variableTransformerIds);
        Reports.reportUndecidedOrientationParallelBundles(reportNode, parallelDetection.undecidedBundles());

        Reports.reportConstantQGeneratorsSize(reportNode, params.getConstantQGenerators().size());
        Reports.reportVariableTwoWindingsTransformersSize(reportNode, params.getVariableTwoWindingsTransformers().size());
        Reports.reportVariableShuntCompensatorsSize(reportNode, params.getVariableShuntCompensators().size());
    }

    public ReactiveSlackOutput getReactiveSlackOutput() {
        return reactiveSlackOutput;
    }

    public NetworkModifications getNetworkModifications() {
        return networkModifications;
    }

    public VoltageProfileOutput getVoltageProfileOutput() {
        return voltageProfileOutput;
    }

    public FixedParallelTransformersOutput getFixedParallelTransformersOutput() {
        return fixedParallelTransformersOutput;
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(constantQGenerators, variableShuntCompensators, variableTwoWindingsTransformers,
                algorithmParams, voltageLimitsOverride, configuredReactiveSlackBuses,
                parallelTwoWindingsTransformersBundles);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean isConvergenceOk) {
        if (isConvergenceOk) {
            List<AmplOutputFile> networkModificationsOutputFiles = networkModifications.getOutputFiles();
            List<AmplOutputFile> list = new ArrayList<>(networkModificationsOutputFiles.size() + 3);
            list.addAll(networkModificationsOutputFiles);
            list.add(reactiveSlackOutput);
            list.add(voltageProfileOutput);
            list.add(fixedParallelTransformersOutput);
            return list;
        }
        return List.of();
    }

    /**
     * Will check that every output file parsing went well.
     *
     * @return <code>true</code> if ALL ouput file parsing didn't throw any IOExceptions
     */
    public boolean checkErrors() {
        return !reactiveSlackOutput.isErrorState();
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public AmplExportConfig getAmplExportConfig() {
        return amplExportConfig;
    }

    @Override
    public String getDebugDir() {
        return debugDir;
    }
}
