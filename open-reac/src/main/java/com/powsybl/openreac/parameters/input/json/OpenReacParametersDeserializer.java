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
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Map.entry;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */

public class OpenReacParametersDeserializer extends StdDeserializer<OpenReacParameters> {

    private interface IOBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }

    private static BiConsumer<JsonParser, OpenReacParameters> safeRead(IOBiConsumer<JsonParser, OpenReacParameters> consumer) {
        return (parser, parameters) -> {
            try {
                parser.nextToken();
                consumer.accept(parser, parameters);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Map<String, BiConsumer<JsonParser, OpenReacParameters>> FIELD_PROCESSORS = Map.ofEntries(
            entry("version", (parser, parameters) -> { }),
            entry("specificVoltageLimits", safeRead((parser, parameters) ->
                parameters.addSpecificVoltageLimits(parser.readValueAs(new TypeReference<List<VoltageLimitOverride>>() { }))
            )),
            entry("variableShuntCompensators", safeRead((parser, parameters) ->
                parameters.addVariableShuntCompensators(parser.readValueAs(new TypeReference<List<String>>() { }))
            )),
            entry("constantQGenerators", safeRead((parser, parameters) ->
                parameters.addConstantQGenerators(parser.readValueAs(new TypeReference<List<String>>() { }))
            )),
            entry("variableTwoWindingsTransformers", safeRead((parser, parameters) ->
                parameters.addVariableTwoWindingsTransformers(parser.readValueAs(new TypeReference<List<String>>() { }))
            )),
            entry("configuredReactiveSlackBuses", safeRead((parser, parameters) ->
                parameters.addConfiguredReactiveSlackBuses(parser.readValueAs(new TypeReference<List<String>>() { }))
            )),
            entry("objective", safeRead((parser, parameters) ->
                parameters.setObjective(OpenReacOptimisationObjective.valueOf(parser.getText()))
            )),
            entry("objectiveDistance", safeRead((parser, parameters) ->
                parameters.setObjectiveDistance(parser.getValueAsDouble())
            )),
            entry("logLevelAmpl", safeRead((parser, parameters) ->
                parameters.setLogLevelAmpl(OpenReacAmplLogLevel.valueOf(parser.getText()))
            )),
            entry("logLevelSolver", safeRead((parser, parameters) ->
                parameters.setLogLevelSolver(OpenReacSolverLogLevel.valueOf(parser.getText()))
            )),
            entry("debugDir", safeRead((parser, parameters) ->
                parameters.setDebugDir(parser.readValueAs(new TypeReference<String>() { }))
            )),
            entry("minPlausibleLowVoltageLimit", safeRead((parser, parameters) ->
                parameters.setMinPlausibleLowVoltageLimit(parser.readValueAs(Double.class))
            )),
            entry("maxPlausibleHighVoltageLimit", safeRead((parser, parameters) ->
                parameters.setMaxPlausibleHighVoltageLimit(parser.readValueAs(Double.class))
            )),
            entry("reactiveSlackBusesMode", safeRead((parser, parameters) ->
                parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.valueOf(parser.getText()))
            )),
            entry("activePowerVariationRate", safeRead((parser, parameters) ->
                parameters.setActivePowerVariationRate(parser.getValueAsDouble())
            )),
            entry("minPlausibleActivePowerThreshold", safeRead((parser, parameters) ->
                parameters.setMinPlausibleActivePowerThreshold(parser.getValueAsDouble())
            )),
            entry("lowImpedanceThreshold", safeRead((parser, parameters) ->
                parameters.setLowImpedanceThreshold(parser.getValueAsDouble())
            )),
            entry("minNominalVoltageIgnoredBus", safeRead((parser, parameters) ->
                parameters.setMinNominalVoltageIgnoredBus(parser.getValueAsDouble())
            )),
            entry("minNominalVoltageIgnoredVoltageBounds", safeRead((parser, parameters) ->
                parameters.setMinNominalVoltageIgnoredVoltageBounds(parser.getValueAsDouble())
            )),
            entry("maxPlausiblePowerLimit", safeRead((parser, parameters) ->
                parameters.setPQMax(parser.getValueAsDouble())
            )),
            entry("lowActivePowerDefaultLimit", safeRead((parser, parameters) ->
                parameters.setLowActivePowerDefaultLimit(parser.getValueAsDouble())
            )),
            entry("highActivePowerDefaultLimit", safeRead((parser, parameters) ->
                parameters.setHighActivePowerDefaultLimit(parser.getValueAsDouble())
            )),
            entry("defaultQmaxPmaxRatio", safeRead((parser, parameters) ->
                parameters.setDefaultQmaxPmaxRatio(parser.getValueAsDouble())
            )),
            entry("defaultMinimalQPRange", safeRead((parser, parameters) ->
                parameters.setDefaultMinimalQPRange(parser.getValueAsDouble())
            )),
            entry("defaultVariableScalingFactor", safeRead((parser, parameters) ->
                parameters.setDefaultVariableScalingFactor(parser.readValueAs(Double.class))
            )),
            entry("defaultConstraintScalingFactor", safeRead((parser, parameters) ->
                parameters.setDefaultConstraintScalingFactor(parser.readValueAs(Double.class))
            )),
            entry("reactiveSlackVariableScalingFactor", safeRead((parser, parameters) ->
                parameters.setReactiveSlackVariableScalingFactor(parser.readValueAs(Double.class))
            )),
            entry("twoWindingTransformerRatioVariableScalingFactor", safeRead((parser, parameters) ->
                parameters.setTwoWindingTransformerRatioVariableScalingFactor(parser.readValueAs(Double.class))
            )),
            entry("shuntVariableScalingFactor", safeRead((parser, parameters) ->
                parameters.setShuntVariableScalingFactor(parser.readValueAs(Double.class))
            )),
            entry("optimizationAfterRounding", safeRead((parser, parameters) ->
                parameters.setOptimizationAfterRounding(parser.getValueAsBoolean())
            ))
    );

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
            BiConsumer<JsonParser, OpenReacParameters> consumer = FIELD_PROCESSORS.get(parser.getCurrentName());
            if (consumer == null) {
                throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
            consumer.accept(parser, parameters);
        }
        return parameters;
    }

}
