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
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;

import java.io.IOException;

/**
 * @author Hugo Marcellin {@literal <hugo.marcelin at rte-france.com>}
 */

public class VoltageLimitOverrideDeserializer extends StdDeserializer<VoltageLimitOverride> {

    public VoltageLimitOverrideDeserializer() {
        super(VoltageLimitOverride.class);
    }

    @Override
    public VoltageLimitOverride deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        String voltageLevelId = null;
        VoltageLimitOverride.VoltageLimitType type = null;
        Boolean isRelative = null;
        double overrideValue = Double.NaN;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "voltageLevelId" -> {
                    parser.nextToken();
                    voltageLevelId = parser.readValueAs(String.class);
                }
                case "voltageLimitType" -> {
                    parser.nextToken();
                    type = parser.readValueAs(VoltageLimitOverride.VoltageLimitType.class);
                }
                case "isRelative" -> {
                    parser.nextToken();
                    isRelative = parser.readValueAs(Boolean.class);
                }
                case "value" -> {
                    parser.nextToken();
                    overrideValue = parser.readValueAs(Double.class);
                }
                default -> throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
        }
        if (isRelative == null) {
            throw new InvalidParametersException("A relative or absolute voltage limit override must be specified.");
        }
        return new VoltageLimitOverride(voltageLevelId, type, isRelative, overrideValue);
    }
}
