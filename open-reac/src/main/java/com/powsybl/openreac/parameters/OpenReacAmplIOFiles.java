/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters;

import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.parameters.input.*;
import com.powsybl.openreac.parameters.input.algo.AlgorithmInput;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;

import java.util.Collection;
import java.util.List;

/**
 * OpenReacAmplIOFiles will interface all inputs and outputs needed for OpenReac to the abtracted Ampl Executor.
 * <p>
 * The user of OpenReac should not see this class directly. One should use {@link OpenReacParameters} for inputs
 * and {@link OpenReacResult} for outputs.
 * However, when adding new inputs (outputs) to OpenReac, one must add {@link AmplOutputFile} (@link AmplInputFile)
 * here through {@link OpenReacAmplIOFiles#getInputParameters} ({@link OpenReacAmplIOFiles#getOutputParameters})
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacAmplIOFiles implements AmplParameters {

    private final ConstantQGenerators constantQGenerators;
    private final VariableShuntCompensators variableShuntCompensators;
    private final VariableTwoWindingsTransformers variableTwoWindingsTransformers;
    private final AlgorithmInput algorithmParams;
    private final ReactiveSlackOutput reactiveSlackOutput;
    private final VoltageLevelLimitsOverrideInput voltageLimitsOverride;

    public OpenReacAmplIOFiles(OpenReacParameters params, Network network) {
        //inputs
        this.constantQGenerators = new ConstantQGenerators(params.getConstantQGenerators());
        this.variableShuntCompensators = new VariableShuntCompensators(params.getVariableShuntCompensators());
        this.variableTwoWindingsTransformers = new VariableTwoWindingsTransformers(params.getVariableTwoWindingsTransformers());
        this.algorithmParams = new AlgorithmInput(params.getAlgorithmParams());
        this.voltageLimitsOverride = new VoltageLevelLimitsOverrideInput(params.getSpecificVoltageDelta(), network);

        //outputs
        this.reactiveSlackOutput = new ReactiveSlackOutput();
    }

    public List<ReactiveSlackOutput.ReactiveSlack> getReactiveInvestments() {
        return reactiveSlackOutput.getSlacks();
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(constantQGenerators, variableShuntCompensators, variableTwoWindingsTransformers,
                algorithmParams, voltageLimitsOverride);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean isConvergenceOk) {
        if (isConvergenceOk) {
            return List.of(reactiveSlackOutput);
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
}
