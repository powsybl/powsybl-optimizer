/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParam;

/**
 * @author Hugo Marcellin {@literal <hugo.marcelin at rte-france.com>}
 */

public class OpenReactJsonModule extends SimpleModule {

    public OpenReactJsonModule() {
        addSerializer(OpenReacParameters.class, new OpenReacParametersSerializer());
        addSerializer(VoltageLimitOverride.class, new VoltageLimitOverrideSerializer());
        addSerializer(OpenReacAlgoParam.class, new OpenReacAlgoParamSerializer());
        addDeserializer(OpenReacParameters.class, new OpenReacParametersDeserializer());
        addDeserializer(VoltageLimitOverride.class, new VoltageLimitOverrideDeserializer());
        addDeserializer(OpenReacAlgoParam.class, new OpenReacAlgoParamDeserializer());
    }
}
