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
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
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

        // modify open reac parameters
        parameters.addSpecificVoltageLimits(vloList1);
        parameters.addConstantQGenerators(List.of("g1", "g2"));
        parameters.addVariableTwoWindingsTransformers(List.of("tr1"));
        parameters.addVariableShuntCompensators(List.of("sc1", "sc2"));
        parameters.setObjectiveDistance(0.6665);
        parameters.setMinPlausibleLowVoltageLimit(0.712);
        parameters.setMaxPlausibleHighVoltageLimit(1.2222);
        parameters.setAlphaCoefficient(0.56);
        parameters.setZeroPowerThreshold(0.5);
        parameters.setZeroImpedanceThreshold(1e-5);
        parameters.setNominalThresholdIgnoredBuses(10.);
        parameters.setNominalThresholdIgnoredVoltageBounds(5.);
        parameters.setPQMax(8555.3);
        parameters.setDefaultPMin(99.2);
        parameters.setDefaultPMax(1144.);
        parameters.setDefaultQmaxPmaxRatio(0.4);
        parameters.setDefaultMinimalQPRange(1.1);

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
        assertEquals(List.of("sc1", "sc2"), parameters2.getVariableShuntCompensators());
        assertEquals(OpenReacOptimisationObjective.MIN_GENERATION, parameters2.getObjective());
        assertEquals(0.6665, parameters2.getObjectiveDistance());
        assertEquals(0.712, parameters2.getMinPlausibleLowVoltageLimit());
        assertEquals(1.2222, parameters2.getMaxPlausibleHighVoltageLimit());
        assertEquals(0.56, parameters2.getAlphaCoefficient());
        assertEquals(0.5, parameters2.getZeroPowerThreshold());
        assertEquals(1e-5, parameters2.getZeroImpedanceThreshold());
        assertEquals(10., parameters2.getNominalThresholdIgnoredBuses());
        assertEquals(5., parameters2.getNominalThresholdIgnoredVoltageBounds());
        assertEquals(8555.3, parameters2.getPQMax());
        assertEquals(99.2, parameters2.getDefaultPMin());
        assertEquals(1144., parameters2.getDefaultPMax());
        assertEquals(0.4, parameters2.getDefaultQmaxPmaxRatio());
        assertEquals(1.1, parameters2.getDefaultMinimalQPRange());
    }
}
