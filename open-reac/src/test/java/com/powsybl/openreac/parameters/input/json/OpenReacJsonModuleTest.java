/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class OpenReacJsonModuleTest {

    @Test
    void test() throws IOException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new OpenReactJsonModule());
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addSpecificVoltageLimits(Map.of("foo", new VoltageLimitOverride(VoltageLimitOverride.OverrideKind.RELATIVE, VoltageLimitOverride.OverrideKind.RELATIVE, -1, 2),
                "bar", new VoltageLimitOverride(VoltageLimitOverride.OverrideKind.ABSOLUTE, VoltageLimitOverride.OverrideKind.ABSOLUTE, 20, 26)));
        parameters.addConstantQGenerators(List.of("g1", "g2"));
        parameters.addVariableTwoWindingsTransformers(List.of("tr1"));
        parameters.addVariableShuntCompensators(List.of("sc1", "sc2"));
        parameters.addAlgorithmParam("p1", "v1");
        parameters.addAlgorithmParam("p2", "v2");
        parameters.setObjectiveDistance(5);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        ComparisonUtils.compareTxt(Objects.requireNonNull(getClass().getResourceAsStream("/parameters.json")), json);
        OpenReacParameters parameters2 = objectMapper.readValue(json, OpenReacParameters.class);
        assertEquals(Map.of("foo", new VoltageLimitOverride(VoltageLimitOverride.OverrideKind.RELATIVE, VoltageLimitOverride.OverrideKind.RELATIVE, -1, 2),
                "bar", new VoltageLimitOverride(VoltageLimitOverride.OverrideKind.ABSOLUTE, VoltageLimitOverride.OverrideKind.ABSOLUTE, 20, 26)), parameters2.getSpecificVoltageLimits());
        assertEquals(List.of("g1", "g2"), parameters2.getConstantQGenerators());
        assertEquals(List.of("tr1"), parameters2.getVariableTwoWindingsTransformers());
        assertEquals(2, parameters2.getAlgorithmParams().size());
        assertEquals("p1", parameters2.getAlgorithmParams().get(0).getName());
        assertEquals("v1", parameters2.getAlgorithmParams().get(0).getValue());
        assertEquals("p2", parameters2.getAlgorithmParams().get(1).getName());
        assertEquals("v2", parameters2.getAlgorithmParams().get(1).getValue());
        assertEquals(5, parameters2.getObjectiveDistance());
    }
}
