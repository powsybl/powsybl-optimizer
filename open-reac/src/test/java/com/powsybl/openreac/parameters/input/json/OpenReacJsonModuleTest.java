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
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
class OpenReacJsonModuleTest {

    @Test
    void testOpenReacParametersLists() throws IOException {
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
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED);
        parameters.addConfiguredReactiveSlackBuses(List.of("bus1", "bus2"));
        parameters.setDebugDir("/tmp/debugDir");

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        ComparisonUtils.assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream("/parametersLists.json")), json);

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
        assertEquals(ReactiveSlackBusesMode.CONFIGURED, parameters2.getReactiveSlackBusesMode());
        assertEquals(List.of("bus1", "bus2"), parameters2.getConfiguredReactiveSlackBuses());
    }

    @Test
    void testOpenReacParametersThresholds() throws IOException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new OpenReactJsonModule());
        OpenReacParameters parameters = new OpenReacParameters();

        // modify open reac parameters
        parameters.setObjectiveDistance(5);
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.WARNING);
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        parameters.setMinPlausibleLowVoltageLimit(0.755);
        parameters.setMaxPlausibleHighVoltageLimit(1.236);
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.ALL);
        parameters.setActivePowerVariationRate(0.56);
        parameters.setMinPlausibleActivePowerThreshold(0.5);
        parameters.setLowImpedanceThreshold(1e-5);
        parameters.setMinNominalVoltageIgnoredBus(10.);
        parameters.setMinNominalVoltageIgnoredVoltageBounds(5.);
        parameters.setPQMax(8555.3);
        parameters.setLowActivePowerDefaultLimit(99.2);
        parameters.setHighActivePowerDefaultLimit(1144.);
        parameters.setDefaultQmaxPmaxRatio(0.4);
        parameters.setDefaultMinimalQPRange(1.1);
        parameters.setDefaultVariableScalingFactor(0.756);
        parameters.setDefaultConstraintScalingFactor(0.888);
        parameters.setReactiveSlackVariableScalingFactor(1e-2);
        parameters.setTwoWindingTransformerRatioVariableScalingFactor(0.005);
        parameters.setOptimizationAfterRounding(true);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        ComparisonUtils.assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream("/parametersThresholds.json")), json);
        OpenReacParameters parameters2 = objectMapper.readValue(json, OpenReacParameters.class);

        assertEquals(5, parameters2.getObjectiveDistance());
        assertEquals(OpenReacAmplLogLevel.WARNING, parameters2.getLogLevelAmpl());
        assertEquals(OpenReacSolverLogLevel.NOTHING, parameters2.getLogLevelSolver());
        assertEquals(0.755, parameters2.getMinPlausibleLowVoltageLimit());
        assertEquals(1.236, parameters2.getMaxPlausibleHighVoltageLimit());
        assertEquals(ReactiveSlackBusesMode.ALL, parameters2.getReactiveSlackBusesMode());
        assertEquals(OpenReacOptimisationObjective.MIN_GENERATION, parameters2.getObjective());
        assertEquals(0.56, parameters2.getActivePowerVariationRate());
        assertEquals(0.5, parameters2.getMinPlausibleActivePowerThreshold());
        assertEquals(1e-5, parameters2.getLowImpedanceThreshold());
        assertEquals(10., parameters2.getMinNominalVoltageIgnoredBus());
        assertEquals(5., parameters2.getMinNominalVoltageIgnoredVoltageBounds());
        assertEquals(8555.3, parameters2.getPQMax());
        assertEquals(99.2, parameters2.getLowActivePowerDefaultLimit());
        assertEquals(1144., parameters2.getHighActivePowerDefaultLimit());
        assertEquals(0.4, parameters2.getDefaultQmaxPmaxRatio());
        assertEquals(1.1, parameters2.getDefaultMinimalQPRange());
        assertEquals(0.756, parameters2.getDefaultVariableScalingFactor());
        assertEquals(0.888, parameters2.getDefaultConstraintScalingFactor());
        assertEquals(1e-2, parameters2.getReactiveSlackVariableScalingFactor());
        assertEquals(0.005, parameters2.getTwoWindingTransformerRatioVariableScalingFactor());
        assertTrue(parameters2.isOptimizationAfterRounding());
    }

    @Test
    void testOpenReacParametersObjectivePenalties() throws IOException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new OpenReactJsonModule());
        OpenReacParameters parameters = new OpenReacParameters();

        // Explicitly override all three objective penalties with non-default values
        parameters.setPenaltyInvestReaPos(5.5);
        parameters.setPenaltyInvestReaNeg(7.25);
        parameters.setPenaltyActivePower(0.42);
        parameters.setPenaltyUnitsReactive(0.2);
        parameters.setPenaltyTransfoRatio(0.3);
        parameters.setPenaltyVoltageTargetRatio(0.8);
        parameters.setPenaltyVoltageTargetData(0.9);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        ComparisonUtils.assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream("/parametersObjectivePenalties.json")), json);

        OpenReacParameters parameters2 = objectMapper.readValue(json, OpenReacParameters.class);

        assertEquals(5.5, parameters2.getPenaltyInvestReaPos());
        assertEquals(7.25, parameters2.getPenaltyInvestReaNeg());
        assertEquals(0.42, parameters2.getPenaltyActivePower());
        assertEquals(0.2, parameters2.getPenaltyUnitsReactive());
        assertEquals(0.3, parameters2.getPenaltyTransfoRatio());
        assertEquals(0.8, parameters2.getPenaltyVoltageTargetRatio());
        assertEquals(0.9, parameters2.getPenaltyVoltageTargetData());

        // Round-trip null restoration: setting penaltyActivePower to null should survive JSON serialization
        parameters2.setPenaltyActivePower(null);
        parameters2.setPenaltyVoltageTargetRatio(null);
        parameters2.setPenaltyVoltageTargetData(null);
        String json2 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters2);
        OpenReacParameters parameters3 = objectMapper.readValue(json2, OpenReacParameters.class);
        assertNull(parameters3.getPenaltyActivePower());
        assertNull(parameters3.getPenaltyVoltageTargetRatio());
        assertNull(parameters3.getPenaltyVoltageTargetData());
        assertEquals(5.5, parameters3.getPenaltyInvestReaPos());
        assertEquals(7.25, parameters3.getPenaltyInvestReaNeg());
    }

    @Test
    void testOpenReacParametersBackwardCompatibilityV1dot0() throws IOException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new OpenReactJsonModule());

        // Read a v1.0 file, which pre-dates the three penalty fields introduced in v1.1.
        // This must succeed, and the new fields must take their default values.
        OpenReacParameters parameters = objectMapper.readValue(
                Objects.requireNonNull(getClass().getResourceAsStream("/parametersV1dot0.json")),
                OpenReacParameters.class);

        // The three v1.1 fields must fall back to their defaults when reading a v1.0 file
        assertEquals(10, parameters.getPenaltyInvestReaPos());
        assertEquals(10, parameters.getPenaltyInvestReaNeg());
        assertNull(parameters.getPenaltyActivePower());
        assertEquals(0.1, parameters.getPenaltyUnitsReactive());
        assertEquals(0.1, parameters.getPenaltyTransfoRatio());
        assertNull(parameters.getPenaltyVoltageTargetRatio());
        assertNull(parameters.getPenaltyVoltageTargetData());

        // Spot-check a few pre-existing fields to confirm the rest of the deserialization still works
        assertEquals(OpenReacOptimisationObjective.MIN_GENERATION, parameters.getObjective());
        assertEquals(ReactiveSlackBusesMode.CONFIGURED, parameters.getReactiveSlackBusesMode());
        assertEquals(List.of("g1", "g2"), parameters.getConstantQGenerators());
        assertEquals(0.1, parameters.getShuntVariableScalingFactor());
    }
}
