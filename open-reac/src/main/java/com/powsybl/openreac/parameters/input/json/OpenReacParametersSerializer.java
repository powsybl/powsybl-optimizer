/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;

import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenReacParametersSerializer extends StdSerializer<OpenReacParameters> {

    private static final String VERSION = "1.0";

    public OpenReacParametersSerializer() {
        super(OpenReacParameters.class);
    }

    @Override
    public void serialize(OpenReacParameters openReacParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("version", VERSION);

        serializerProvider.defaultSerializeField("specificVoltageLimits", openReacParameters.getSpecificVoltageLimits(), jsonGenerator);
        serializerProvider.defaultSerializeField("constantQGenerators", openReacParameters.getConstantQGenerators(), jsonGenerator);
        serializerProvider.defaultSerializeField("variableTwoWindingsTransformers", openReacParameters.getVariableTwoWindingsTransformers(), jsonGenerator);
        serializerProvider.defaultSerializeField("variableShuntCompensators", openReacParameters.getVariableShuntCompensators(), jsonGenerator);
        if (openReacParameters.getReactiveSlackBusesMode() == ReactiveSlackBusesMode.CONFIGURED) {
            serializerProvider.defaultSerializeField("configuredReactiveSlackBuses", openReacParameters.getConfiguredReactiveSlackBuses(), jsonGenerator);
        }
        serializerProvider.defaultSerializeField("objective", openReacParameters.getObjective().name(), jsonGenerator);
        if (openReacParameters.getObjectiveDistance() != null) {
            serializerProvider.defaultSerializeField("objectiveDistance", openReacParameters.getObjectiveDistance(), jsonGenerator);
        }
        serializerProvider.defaultSerializeField("logLevelAmpl", openReacParameters.getLogLevelAmpl(), jsonGenerator);
        serializerProvider.defaultSerializeField("logLevelSolver", openReacParameters.getLogLevelSolver(), jsonGenerator);
        serializerProvider.defaultSerializeField("minPlausibleLowVoltageLimit", openReacParameters.getMinPlausibleLowVoltageLimit(), jsonGenerator);
        serializerProvider.defaultSerializeField("maxPlausibleHighVoltageLimit", openReacParameters.getMaxPlausibleHighVoltageLimit(), jsonGenerator);
        serializerProvider.defaultSerializeField("reactiveSlackBusesMode", openReacParameters.getReactiveSlackBusesMode().name(), jsonGenerator);
        serializerProvider.defaultSerializeField("defaultVariablesScalingFactor", openReacParameters.getDefaultVariablesScalingFactor(), jsonGenerator);
        serializerProvider.defaultSerializeField("defaultConstraintsScalingFactor", openReacParameters.getDefaultConstraintsScalingFactor(), jsonGenerator);
        serializerProvider.defaultSerializeField("reactiveSlackVariablesScalingFactor", openReacParameters.getReactiveSlackVariablesScalingFactor(), jsonGenerator);
        serializerProvider.defaultSerializeField("twoWindingTransformerRatioVariablesScalingFactor", openReacParameters.getTwoWindingTransformerRatioVariablesScalingFactor(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
