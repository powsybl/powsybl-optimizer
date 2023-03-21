package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;

import java.util.List;

/**
 * List of shunts that can be modified by OpenReac
 * timestep num bus id
 */
public class FixedReactanceGeneratorInput extends AbstractReactiveElementInput {
    public static final String PARAM_GENERATOR_FILE_NAME = "param_generators_reactive.txt";

    public FixedReactanceGeneratorInput(List<String> elementIds) {
        super(elementIds);
    }

    @Override
    public String getFileName() {
        return PARAM_GENERATOR_FILE_NAME;
    }

    @Override
    AmplSubset getElementAmplSubset() {
        return AmplSubset.GENERATOR;
    }
}
