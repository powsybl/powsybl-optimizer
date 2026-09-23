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
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.ReferenceState;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;

import static java.util.Map.entry;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */

public class OpenReacParametersDeserializer extends StdDeserializer<OpenReacParameters> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacParametersDeserializer.class);

    private interface IOBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }

    private static BiConsumer<JsonParser, OpenReacParameters> safeRead(IOBiConsumer<JsonParser, OpenReacParameters> consumer) {
        return (parser, parameters) -> {
            try {
                parser.nextToken();
                consumer.accept(parser, parameters);
            } catch (IOException e) {
                throw new PowsyblException(e);
            }
        };
    }

    private static final String CLASS_NAME = "OpenReacParameters";

    // Up to version 1.3, a null penalty meant "use the default of the objective type". Objective types are gone,
    // so a null value now simply keeps the default of the parameter.
    private static BiConsumer<JsonParser, OpenReacParameters> safeReadNullableDouble(ObjDoubleConsumer<OpenReacParameters> setter) {
        return safeRead((parser, parameters) -> {
            if (parser.currentToken() != JsonToken.VALUE_NULL) {
                setter.accept(parameters, parser.getValueAsDouble());
            }
        });
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
                LOGGER.warn("Objective types have been removed, field 'objective' ({}) is ignored: set the penalty weights explicitly instead.", parser.getText())
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
                parameters.setDebugDir(parser.getText())
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
            entry("penaltyInvestReaPos", safeRead((parser, parameters) ->
                parameters.setPenaltyInvestReaPos(parser.readValueAs(Double.class))
            )),
            entry("penaltyInvestReaNeg", safeRead((parser, parameters) ->
                parameters.setPenaltyInvestReaNeg(parser.readValueAs(Double.class))
            )),
            entry("penaltyActivePower", safeReadNullableDouble(OpenReacParameters::setPenaltyActivePower)),
            entry("penaltyUnitsReactive", safeRead((parser, parameters) ->
                parameters.setPenaltyUnitsReactive(parser.readValueAs(Double.class))
            )),
            entry("penaltyTransfoRatio", safeRead((parser, parameters) ->
                parameters.setPenaltyTransfoRatio(parser.readValueAs(Double.class))
            )),
            entry("penaltyVoltageTargetRatio", safeReadNullableDouble(OpenReacParameters::setPenaltyVoltageTargetRatio)),
            entry("penaltyVoltageTargetData", safeReadNullableDouble(OpenReacParameters::setPenaltyVoltageTargetData)),
            entry("optimizationAfterRounding", safeRead((parser, parameters) ->
                parameters.setOptimizationAfterRounding(parser.getValueAsBoolean())
            )),
            entry("parallelTransformersGrouping", safeRead((parser, parameters) ->
                parameters.setParallelTransformersGrouping(parser.getValueAsBoolean())
            )),
            entry("referenceState", safeRead((parser, parameters) ->
                parameters.setReferenceState(ReferenceState.valueOf(parser.getText()))
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
        String version = OpenReacParametersSerializer.VERSION;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();

            // Capture the version inline so it can be used for version-gated field checks
            if ("version".equals(fieldName)) {
                parser.nextToken();
                version = parser.getValueAsString();
                continue;
            }

            // Version-gated fields, by the version that introduced (or removed) them
            switch (fieldName) {
                case "penaltyInvestReaPos", "penaltyInvestReaNeg", "penaltyActivePower",
                     "penaltyUnitsReactive", "penaltyTransfoRatio",
                     "penaltyVoltageTargetRatio", "penaltyVoltageTargetData" ->
                    JsonUtil.assertGreaterOrEqualThanReferenceVersion(CLASS_NAME, fieldName, version, "1.1");
                case "parallelTransformersGrouping" ->
                    JsonUtil.assertGreaterOrEqualThanReferenceVersion(CLASS_NAME, fieldName, version, "1.2");
                case "referenceState" ->
                    JsonUtil.assertGreaterOrEqualThanReferenceVersion(CLASS_NAME, fieldName, version, "1.3");
                case "objective" ->
                    JsonUtil.assertLessThanReferenceVersion(CLASS_NAME, fieldName, version, "1.4");
                default -> { /* no version gate */ }
            }

            BiConsumer<JsonParser, OpenReacParameters> consumer = FIELD_PROCESSORS.get(fieldName);
            if (consumer == null) {
                throw new IllegalStateException("Unexpected field: " + fieldName);
            }
            try {
                consumer.accept(parser, parameters);
            } catch (PowsyblException powsyblException) {
                if (powsyblException.getCause() instanceof IOException ioException) {
                    throw ioException;
                } else {
                    throw powsyblException;
                }
            }
        }
        return parameters;
    }
}
