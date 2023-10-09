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
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;

import java.io.IOException;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */

public class VoltageLimitOverrideDeserializer extends StdDeserializer<VoltageLimitOverride> {

    public VoltageLimitOverrideDeserializer() {
        super(VoltageLimitOverride.class);
    }

    @Override
    public VoltageLimitOverride deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        VoltageLimitOverride.OverrideKind lowLimitKind = null;
        VoltageLimitOverride.OverrideKind highLimitKind = null;
        double lowLimitOverride = 0;
        double highLimitOverride = 0;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "lowLimitKind":
                    parser.nextToken();
                    lowLimitKind = parser.readValueAs(VoltageLimitOverride.OverrideKind.class);
                    break;
                case "highLimitKind":
                    parser.nextToken();
                    highLimitKind = parser.readValueAs(VoltageLimitOverride.OverrideKind.class);
                    break;
                case "lowLimitOverride":
                    parser.nextToken();
                    lowLimitOverride = parser.readValueAs(Double.class);
                    break;
                case "highLimitOverride":
                    parser.nextToken();
                    highLimitOverride = parser.readValueAs(Double.class);
                    break;
                default:
                    throw new IllegalStateException("Unexpected field: " + parser.getCurrentName());
            }
        }
        return new VoltageLimitOverride(lowLimitKind, highLimitKind,
                lowLimitOverride, highLimitOverride);
    }
}
