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
import com.powsybl.openreac.parameters.input.algo.OpenReacBusesWithReactiveSlackConfig;

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
        // Export configured buses only if
        if (openReacParameters.getBusesWithReactiveSlackConfig() == OpenReacBusesWithReactiveSlackConfig.SPECIFIED) {
            serializerProvider.defaultSerializeField("busesWithReactiveSlack", openReacParameters.getConfiguredBusesWithReactiveSlacks(), jsonGenerator);
        }
        serializerProvider.defaultSerializeField("objective", openReacParameters.getObjective().name(), jsonGenerator);
        if (openReacParameters.getObjectiveDistance() != null) {
            serializerProvider.defaultSerializeField("objectiveDistance", openReacParameters.getObjectiveDistance(), jsonGenerator);
        }
        serializerProvider.defaultSerializeField("busesWithReactiveSlackConfig", openReacParameters.getBusesWithReactiveSlackConfig().name(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
