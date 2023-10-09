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
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;

import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */

public class VoltageLimitOverrideSerializer extends StdSerializer<VoltageLimitOverride> {

    public VoltageLimitOverrideSerializer() {
        super(VoltageLimitOverride.class);
    }

    @Override
    public void serialize(VoltageLimitOverride voltageLimitOverride, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("lowLimitKind", voltageLimitOverride.getLowLimitKind());
        jsonGenerator.writeObjectField("highLimitKind", voltageLimitOverride.getHighLimitKind());
        jsonGenerator.writeNumberField("lowLimitOverride", voltageLimitOverride.getLowLimitOverride());
        jsonGenerator.writeNumberField("highLimitOverride", voltageLimitOverride.getHighLimitOverride());
        jsonGenerator.writeEndObject();
    }
}
