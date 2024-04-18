/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;

import java.io.IOException;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */

public class OpenReacParametersDeserializer extends StdDeserializer<OpenReacParameters> {

    public OpenReacParametersDeserializer() {
        super(OpenReacParameters.class);
    }

    @Override
    public OpenReacParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new OpenReacParameters());
    }

    @Override
    public OpenReacParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, OpenReacParameters parameters) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "version" -> {
                    // nothing to do
                }
                case "specificVoltageLimits" -> {
                    parser.nextToken();
                    parameters.addSpecificVoltageLimits(parser.readValueAs(new TypeReference<List<VoltageLimitOverride>>() { }));
                }
                case "variableShuntCompensators" -> {
                    parser.nextToken();
                    parameters.addVariableShuntCompensators(parser.readValueAs(new TypeReference<List<String>>() { }));
                }
                case "constantQGenerators" -> {
                    parser.nextToken();
                    parameters.addConstantQGenerators(parser.readValueAs(new TypeReference<List<String>>() { }));
                }
                case "variableTwoWindingsTransformers" -> {
                    parser.nextToken();
                    parameters.addVariableTwoWindingsTransformers(parser.readValueAs(new TypeReference<List<String>>() { }));
                }
                case "configuredReactiveSlackBuses" -> {
                    parser.nextToken();
                    parameters.addConfiguredReactiveSlackBuses(parser.readValueAs(new TypeReference<List<String>>() { }));
                }
                case "objective" -> {
                    parser.nextToken();
                    parameters.setObjective(OpenReacOptimisationObjective.valueOf(parser.getText()));
                }
                case "objectiveDistance" -> {
                    parser.nextToken();
                    parameters.setObjectiveDistance(parser.getValueAsDouble());
                }
                case "logLevelAmpl" -> {
                    parser.nextToken();
                    parameters.setLogLevelAmpl(OpenReacAmplLogLevel.valueOf(parser.getText()));
                }
                case "logLevelSolver" -> {
                    parser.nextToken();
                    parameters.setLogLevelSolver(OpenReacSolverLogLevel.valueOf(parser.getText()));
                }
                case "minPlausibleLowVoltageLimit" -> {
                    parser.nextToken();
                    parameters.setMinPlausibleLowVoltageLimit(parser.readValueAs(Double.class));
                }
                case "maxPlausibleHighVoltageLimit" -> {
                    parser.nextToken();
                    parameters.setMaxPlausibleHighVoltageLimit(parser.readValueAs(Double.class));
                }
                case "reactiveSlackBusesMode" -> {
                    parser.nextToken();
                    parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.valueOf(parser.getText()));
                }
                case "activePowerVariationRate" -> {
                    parser.nextToken();
                    parameters.setActivePowerVariationRate(parser.getValueAsDouble());
                }
                case "minPlausibleActivePowerThreshold" -> {
                    parser.nextToken();
                    parameters.setMinPlausibleActivePowerThreshold(parser.getValueAsDouble());
                }
                case "lowImpedanceThreshold" -> {
                    parser.nextToken();
                    parameters.setLowImpedanceThreshold(parser.getValueAsDouble());
                }
                case "minNominalVoltageIgnoredBus" -> {
                    parser.nextToken();
                    parameters.setMinNominalVoltageIgnoredBus(parser.getValueAsDouble());
                }
                case "minNominalVoltageIgnoredVoltageBounds" -> {
                    parser.nextToken();
                    parameters.setMinNominalVoltageIgnoredVoltageBounds(parser.getValueAsDouble());
                }
                case "maxPlausiblePowerLimit" -> {
                    parser.nextToken();
                    parameters.setPQMax(parser.getValueAsDouble());
                }
                case "lowActivePowerDefaultLimit" -> {
                    parser.nextToken();
                    parameters.setLowActivePowerDefaultLimit(parser.getValueAsDouble());
                }
                case "highActivePowerDefaultLimit" -> {
                    parser.nextToken();
                    parameters.setHighActivePowerDefaultLimit(parser.getValueAsDouble());
                }
                case "defaultQmaxPmaxRatio" -> {
                    parser.nextToken();
                    parameters.setDefaultQmaxPmaxRatio(parser.getValueAsDouble());
                }
                case "defaultMinimalQPRange" -> {
                    parser.nextToken();
                    parameters.setDefaultMinimalQPRange(parser.getValueAsDouble());
                }
                case "defaultVariableScalingFactor" -> {
                    parser.nextToken();
                    parameters.setDefaultVariableScalingFactor(parser.readValueAs(Double.class));
                }
                case "defaultConstraintScalingFactor" -> {
                    parser.nextToken();
                    parameters.setDefaultConstraintScalingFactor(parser.readValueAs(Double.class));
                }
                case "reactiveSlackVariableScalingFactor" -> {
                    parser.nextToken();
                    parameters.setReactiveSlackVariableScalingFactor(parser.readValueAs(Double.class));
                }
                case "twoWindingTransformerRatioVariableScalingFactor" -> {
                    parser.nextToken();
                    parameters.setTwoWindingTransformerRatioVariableScalingFactor(parser.readValueAs(Double.class));
                }
                default -> throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

}
