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
import com.powsybl.openreac.parameters.input.*;
import com.powsybl.openreac.parameters.output.IndicatorOutput;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 *
 * OpenReacAmplIOFiles will interface all inputs and outputs needed for OpenReac to the abtracted Ampl Executor.
 * <p>
 * The user of OpenReac should not see this class directly. One should use {@link OpenReacParameters} for inputs
 * and {@link OpenReacResult} for outputs.
 * However, when adding new inputs (outputs) to OpenReac, one must add {@link AmplOutputFile} (@link AmplInputFile)
 * here through {@link OpenReacAmplIOFiles#getInputParameters} ({@link OpenReacAmplIOFiles#getOutputParameters()})
 */
public class OpenReacAmplIOFiles implements AmplParameters {

    private final TargetQGenerators targetQGenerators;
    private final VariableShuntCompensators variableShuntCompensators;
    private final VariableTwoWindingsTransformers variableTwoWindingsTransformers;
    private final AlgorithmInput algorithmParams;
    private final ReactiveSlackOutput reactiveSlackOutput;
    private final IndicatorOutput indicators;

    public OpenReacAmplIOFiles(OpenReacParameters params) {
        this.targetQGenerators = new TargetQGenerators(params.getTargetQGenerators());
        this.variableShuntCompensators = new VariableShuntCompensators(params.getVariableShuntCompensators());
        this.variableTwoWindingsTransformers = new VariableTwoWindingsTransformers(params.getVariableTwoWindingsTransformers());
        this.algorithmParams = new AlgorithmInput(params.getAlgorithmParams());
        this.reactiveSlackOutput = new ReactiveSlackOutput();
        this.indicators = new IndicatorOutput();
    }

    public List<ReactiveSlackOutput.ReactiveSlack> getReactiveInvestments() {
        return reactiveSlackOutput.getSlacks();
    }

    public Map<String, String> getIndicators() {
        return indicators.getIndicators();
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(targetQGenerators, variableShuntCompensators, variableTwoWindingsTransformers, algorithmParams);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters() {
        return List.of(reactiveSlackOutput, indicators);
    }

    /**
     * Will check that every output file parsing went well.
     * @return <code>true</code> if ALL ouput file parsing didn't throw any IOExceptions
     */
    public boolean checkErrors() {
        return reactiveInvestmentOutput.isErrorState() || indicators.isErrorState();
    }
}
