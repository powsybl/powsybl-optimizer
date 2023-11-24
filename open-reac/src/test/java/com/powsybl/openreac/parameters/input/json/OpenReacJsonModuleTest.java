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
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class OpenReacJsonModuleTest {

    @Test
    void test() throws IOException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new OpenReactJsonModule());
        OpenReacParameters parameters = new OpenReacParameters();

        // List of voltage limit overrides
        List<VoltageLimitOverride> vloList1 = new ArrayList<>();
        vloList1.add(new VoltageLimitOverride("foo", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -1));
        vloList1.add(new VoltageLimitOverride("foo", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 2));
        vloList1.add(new VoltageLimitOverride("bar", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 20));
        vloList1.add(new VoltageLimitOverride("bar", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 26));

        parameters.addSpecificVoltageLimits(vloList1);
        parameters.addConstantQGenerators(List.of("g1", "g2"));
        parameters.addVariableTwoWindingsTransformers(List.of("tr1"));
        parameters.addVariableShuntCompensators(List.of("sc1", "sc2"));
        parameters.setObjectiveDistance(5);
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.WARNING);
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        parameters.setMinPlausibleLowVoltageLimit(0.755);
        parameters.setMaxPlausibleHighVoltageLimit(1.236);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        ComparisonUtils.compareTxt(Objects.requireNonNull(getClass().getResourceAsStream("/parameters.json")), json);

        OpenReacParameters parameters2 = objectMapper.readValue(json, OpenReacParameters.class);
        // List of voltage limit overrides
        List<VoltageLimitOverride> vloList2 = new ArrayList<>();
        vloList2.add(new VoltageLimitOverride("foo", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -1));
        vloList2.add(new VoltageLimitOverride("foo", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 2));
        vloList2.add(new VoltageLimitOverride("bar", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 20));
        vloList2.add(new VoltageLimitOverride("bar", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 26));

        assertEquals(vloList2, parameters2.getSpecificVoltageLimits());
        assertEquals(List.of("g1", "g2"), parameters2.getConstantQGenerators());
        assertEquals(List.of("tr1"), parameters2.getVariableTwoWindingsTransformers());
        assertEquals(5, parameters2.getObjectiveDistance());
        assertEquals(OpenReacAmplLogLevel.WARNING, parameters2.getLogLevelAmpl());
        assertEquals(OpenReacSolverLogLevel.NOTHING, parameters2.getLogLevelSolver());
        assertEquals(0.755, parameters2.getMinPlausibleLowVoltageLimit());
        assertEquals(1.236, parameters2.getMaxPlausibleHighVoltageLimit());
    }
}
