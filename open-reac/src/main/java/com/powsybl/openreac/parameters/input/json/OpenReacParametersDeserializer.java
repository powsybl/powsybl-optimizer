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
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;

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
                case "version":
                    break;
                case "specificVoltageLimits":
                    parser.nextToken();
                    parameters.addSpecificVoltageLimits(parser.readValueAs(new TypeReference<List<VoltageLimitOverride>>() { }));
                    break;
                case "variableShuntCompensators":
                    parser.nextToken();
                    parameters.addVariableShuntCompensators(parser.readValueAs(new TypeReference<List<String>>() { }));
                    break;
                case "constantQGenerators":
                    parser.nextToken();
                    parameters.addConstantQGenerators(parser.readValueAs(new TypeReference<List<String>>() { }));
                    break;
                case "variableTwoWindingsTransformers":
                    parser.nextToken();
                    parameters.addVariableTwoWindingsTransformers(parser.readValueAs(new TypeReference<List<String>>() { }));
                    break;
                case "objective":
                    parser.nextToken();
                    parameters.setObjective(OpenReacOptimisationObjective.valueOf(parser.getText()));
                    break;
                case "objectiveDistance":
                    parser.nextToken();
                    parameters.setObjectiveDistance(parser.getValueAsDouble());
                    break;
                case "minPlausibleLowVoltageLimit":
                    parser.nextToken();
                    parameters.setMinPlausibleLowVoltageLimit(parser.getValueAsDouble());
                    break;
                case "maxPlausibleHighVoltageLimit":
                    parser.nextToken();
                    parameters.setMaxPlausibleHighVoltageLimit(parser.getValueAsDouble());
                    break;
                case "alphaCoefficient":
                    parser.nextToken();
                    parameters.setAlphaCoefficient(parser.getValueAsDouble());
                    break;
                case "zeroPowerThreshold":
                    parser.nextToken();
                    parameters.setZeroPowerThreshold(parser.getValueAsDouble());
                    break;
                case "zeroImpedanceThreshold":
                    parser.nextToken();
                    parameters.setZeroImpedanceThreshold(parser.getValueAsDouble());
                    break;
                case "nominalThresholdIgnoredBuses":
                    parser.nextToken();
                    parameters.setNominalThresholdIgnoredBuses(parser.getValueAsDouble());
                    break;
                case "nominalThresholdIgnoredVoltageBounds":
                    parser.nextToken();
                    parameters.setNominalThresholdIgnoredVoltageBounds(parser.getValueAsDouble());
                    break;
                case "pQmax":
                    parser.nextToken();
                    parameters.setPQMax(parser.getValueAsDouble());
                    break;
                case "defaultPMin":
                    parser.nextToken();
                    parameters.setDefaultPMin(parser.getValueAsDouble());
                    break;
                case "defaultPMax":
                    parser.nextToken();
                    parameters.setDefaultPMax(parser.getValueAsDouble());
                    break;
                case "defaultQmaxPmaxRatio":
                    parser.nextToken();
                    parameters.setDefaultQmaxPmaxRatio(parser.getValueAsDouble());
                    break;
                case "defaultMinimalQPRange":
                    parser.nextToken();
                    parameters.setDefaultMinimalQPRange(parser.getValueAsDouble());
                    break;
                default:
                    throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
        }

        return parameters;
    }

}
