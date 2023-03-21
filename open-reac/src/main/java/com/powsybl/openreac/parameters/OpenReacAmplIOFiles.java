package com.powsybl.openreac.parameters;

import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.openreac.parameters.input.*;
import com.powsybl.openreac.parameters.output.IndicatorOutput;
import com.powsybl.openreac.parameters.output.OpenReacResults;
import com.powsybl.openreac.parameters.output.ReactiveInvestmentOutput;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OpenReacAmplIOFiles will interface all inputs and outputs needed for OpenReac to the abtracted Ampl Executor.
 * <p>
 * The user of OpenReac should not see this class directly. One should use {@link OpenReacParameters} for inputs
 * and {@link OpenReacResults} for outputs.
 * However, when adding new inputs (outputs) to OpenReac, one must add {@link AmplOutputFile} (@link AmplInputFile)
 * here through {@link OpenReacAmplIOFiles#getInputParameters} ({@link OpenReacAmplIOFiles#getOutputParameters()})
 */
public class OpenReacAmplIOFiles implements AmplParameters {

    private final FixedReactanceGeneratorInput fixedReactiveGeneratorInput;
    private final VariableReactanceShuntsInput variableReactanceShuntsInput;
    private final VariableRatioInput variableRatioInput;
    private final AlgorithmInput algorithmParams;
    private final ReactiveInvestmentOutput reactiveInvestmentOutput;
    private final IndicatorOutput indicators;

    public OpenReacAmplIOFiles(OpenReacParameters params) {
        this.fixedReactiveGeneratorInput = new FixedReactanceGeneratorInput(params.getFixedGenerators());
        this.variableReactanceShuntsInput = new VariableReactanceShuntsInput(params.getModifiableShunts());
        this.variableRatioInput = new VariableRatioInput(params.getModifiableTransformers());
        this.algorithmParams = new AlgorithmInput(params.getAlgorithmParams());
        this.reactiveInvestmentOutput = new ReactiveInvestmentOutput();
        this.indicators = new IndicatorOutput();
    }

    public List<ReactiveInvestmentOutput.ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestmentOutput.getInvestments();
    }

    public Map<String, String> getIndicators() {
        return indicators.getIndicators();
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(fixedReactiveGeneratorInput, variableReactanceShuntsInput, variableRatioInput, algorithmParams);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters() {
        return List.of(reactiveInvestmentOutput, indicators);
    }

}
