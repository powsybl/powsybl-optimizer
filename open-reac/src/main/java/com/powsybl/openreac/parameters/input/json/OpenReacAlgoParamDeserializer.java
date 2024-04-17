/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParam;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParamImpl;

import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenReacAlgoParamDeserializer extends StdDeserializer<OpenReacAlgoParam> {

    public OpenReacAlgoParamDeserializer() {
        super(OpenReacAlgoParam.class);
    }

    @Override
    public OpenReacAlgoParam deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        String name = null;
        String value = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "name" -> {
                    parser.nextToken();
                    name = parser.getText();
                }
                case "value" -> {
                    parser.nextToken();
                    value = parser.getText();
                }
                default -> throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
        }
        return new OpenReacAlgoParamImpl(name, value);
    }
}
