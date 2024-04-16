/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParam;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
 * @author Pierre ARVY <pierre.arvy at artelys.com>
 */
public class OpenReacParametersTest {

    @Test
    void testObjectiveIntegrity() {
        // Objective choice
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT);
        assertEquals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, parameters.getObjective());
        parameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
        assertEquals(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE, parameters.getObjective());
        assertThrows(NullPointerException.class, () -> parameters.setObjective(null));

        // Objective distance for BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT objective
        parameters.setObjectiveDistance(0); // min value
        assertEquals(0, parameters.getObjectiveDistance());
        parameters.setObjectiveDistance(100); // max value
        assertEquals(100, parameters.getObjectiveDistance());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(-0.15));
        assertEquals("Objective distance must be defined and >= 0 and <= 100 to be consistent", e.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(100.02));
        assertEquals("Objective distance must be defined and >= 0 and <= 100 to be consistent", e2.getMessage());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(Double.NaN));
        assertEquals("Objective distance must be defined and >= 0 and <= 100 to be consistent", e3.getMessage());

        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testAmplLogLevelIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals("INFO", parameters.getLogLevelAmpl().toParam().getValue()); // default value
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG);
        assertEquals("DEBUG", parameters.getLogLevelAmpl().toParam().getValue());
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.WARNING);
        assertEquals("WARNING", parameters.getLogLevelAmpl().toParam().getValue());
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.ERROR);
        assertEquals("ERROR", parameters.getLogLevelAmpl().toParam().getValue());

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelAmpl(null));
    }

    @Test
    void testSolverLogLevelIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals("2", parameters.getLogLevelSolver().toParam().getValue()); // default value
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        assertEquals("0", parameters.getLogLevelSolver().toParam().getValue());
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS);
        assertEquals("1", parameters.getLogLevelSolver().toParam().getValue());

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelSolver(null));
    }

    @Test
    void testMinMaxVoltageLimitIntegrity() {
        Network network = IeeeCdfNetworkFactory.create14();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();

        // Consistency of min plausible low voltage limit (>= 0)
        assertEquals(0.5, parameters.getMinPlausibleLowVoltageLimit()); // default value
        parameters.setMinPlausibleLowVoltageLimit(0.8);
        assertEquals(0.8, parameters.getMinPlausibleLowVoltageLimit());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parameters.setMinPlausibleLowVoltageLimit(-0.25));
        assertEquals("Min plausible low voltage limit must be >= 0 and defined to be consistent.", e.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinPlausibleLowVoltageLimit(Double.NaN));
        assertEquals("Min plausible low voltage limit must be >= 0 and defined to be consistent.", e2.getMessage());

        // Consistency of max plausible high voltage limit (> 0)
        assertEquals(1.5, parameters.getMaxPlausibleHighVoltageLimit()); // default value
        parameters.setMaxPlausibleHighVoltageLimit(0.75);
        assertEquals(0.75, parameters.getMaxPlausibleHighVoltageLimit());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(-0.15));
        assertEquals("Max plausible high voltage limit must be > 0 and defined to be consistent.", e3.getMessage());
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(0));
        assertEquals("Max plausible high voltage limit must be > 0 and defined to be consistent.", e4.getMessage());
        IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(Double.NaN));
        assertEquals("Max plausible high voltage limit must be > 0 and defined to be consistent.", e5.getMessage());

        // Check min < max
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
        InvalidParametersException e6 = assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network));
        assertEquals("At least one algorithm parameter is inconsistent.", e6.getMessage());
        parameters.setMaxPlausibleHighVoltageLimit(1.2);
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testVariablesScalingFactorsIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        // Consistency of default scaling factor
        assertEquals(1, parameters.getDefaultVariableScalingFactor()); // default value
        parameters.setDefaultVariableScalingFactor(0.8);
        assertEquals(0.8, parameters.getDefaultVariableScalingFactor());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultVariableScalingFactor(-0.25));
        assertEquals("Default scaling factor for variables must be > 0 and defined to be consistent.", e.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultVariableScalingFactor(0));
        assertEquals("Default scaling factor for variables must be > 0 and defined to be consistent.", e2.getMessage());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultVariableScalingFactor(Double.NaN));
        assertEquals("Default scaling factor for variables must be > 0 and defined to be consistent.", e3.getMessage());

        // Consistency of reactive slack variables scaling factor
        assertEquals(1e-1, parameters.getReactiveSlackVariableScalingFactor()); // default value
        parameters.setReactiveSlackVariableScalingFactor(0.058);
        assertEquals(0.058, parameters.getReactiveSlackVariableScalingFactor());
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> parameters.setReactiveSlackVariableScalingFactor(-0.25));
        assertEquals("Scaling factor for reactive slack variables must be > 0 and defined to be consistent.", e4.getMessage());
        IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> parameters.setReactiveSlackVariableScalingFactor(0));
        assertEquals("Scaling factor for reactive slack variables must be > 0 and defined to be consistent.", e5.getMessage());
        IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class, () -> parameters.setReactiveSlackVariableScalingFactor(Double.NaN));
        assertEquals("Scaling factor for reactive slack variables must be > 0 and defined to be consistent.", e6.getMessage());

        // Consistency of t2wt ratio variables scaling factor
        assertEquals(1e-3, parameters.getTwoWindingTransformerRatioVariableScalingFactor()); // default value
        parameters.setTwoWindingTransformerRatioVariableScalingFactor(0.007);
        assertEquals(0.007, parameters.getTwoWindingTransformerRatioVariableScalingFactor());
        IllegalArgumentException e7 = assertThrows(IllegalArgumentException.class, () -> parameters.setTwoWindingTransformerRatioVariableScalingFactor(-0.25));
        assertEquals("Scaling factor for transformer ratio variables must be > 0 and defined to be consistent.", e7.getMessage());
        IllegalArgumentException e8 = assertThrows(IllegalArgumentException.class, () -> parameters.setTwoWindingTransformerRatioVariableScalingFactor(0));
        assertEquals("Scaling factor for transformer ratio variables must be > 0 and defined to be consistent.", e8.getMessage());
        IllegalArgumentException e9 = assertThrows(IllegalArgumentException.class, () -> parameters.setTwoWindingTransformerRatioVariableScalingFactor(Double.NaN));
        assertEquals("Scaling factor for transformer ratio variables must be > 0 and defined to be consistent.", e9.getMessage());
    }

    @Test
    void testConstraintsScalingFactorsIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        // Consistency of default constraints scaling factor
        assertEquals(1, parameters.getDefaultConstraintScalingFactor()); // default value
        parameters.setDefaultConstraintScalingFactor(0);
        assertEquals(0, parameters.getDefaultConstraintScalingFactor());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultConstraintScalingFactor(-0.25));
        assertEquals("Default scaling factor for constraints must be >= 0 and defined to be consistent.", e.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultConstraintScalingFactor(Double.NaN));
        assertEquals("Default scaling factor for constraints must be >= 0 and defined to be consistent.", e2.getMessage());
    }

    @Test
    void testAlgorithmParams() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
        parameters.setObjectiveDistance(40);
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG);
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        parameters.setMinPlausibleLowVoltageLimit(0.8);
        parameters.setMaxPlausibleHighVoltageLimit(1.2);
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.ALL);
        parameters.setDefaultVariableScalingFactor(0.5);
        parameters.setDefaultConstraintScalingFactor(0.75);
        parameters.setReactiveSlackVariableScalingFactor(1e-2);
        parameters.setTwoWindingTransformerRatioVariableScalingFactor(0.0001);

        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();
        assertEquals(11, algoParams.size());
        assertEquals("2", algoParams.get(0).getValue());
        assertEquals("0.4", algoParams.get(1).getValue());
        assertEquals("DEBUG", algoParams.get(2).getValue());
        assertEquals("0", algoParams.get(3).getValue());
        assertEquals("0.8", algoParams.get(4).getValue());
        assertEquals("1.2", algoParams.get(5).getValue());
        assertEquals("ALL", algoParams.get(6).getValue());
        assertEquals("0.5", algoParams.get(7).getValue());
        assertEquals("0.75", algoParams.get(8).getValue());
        assertEquals("0.01", algoParams.get(9).getValue());
        assertEquals("1.0E-4", algoParams.get(10).getValue());
    }

    @Test
    void testBusesWithReactiveSlackConfigIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals(ReactiveSlackBusesMode.NO_GENERATION, parameters.getReactiveSlackBusesMode()); // default value
        assertThrows(NullPointerException.class, () -> parameters.setReactiveSlackBusesMode(null));
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.CONFIGURED);
        assertEquals("CONFIGURED", parameters.getReactiveSlackBusesMode().toParam().getValue());
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.NO_GENERATION);
        assertEquals("NO_GENERATION", parameters.getReactiveSlackBusesMode().toParam().getValue());
        parameters.setReactiveSlackBusesMode(ReactiveSlackBusesMode.ALL);
        assertEquals("ALL", parameters.getReactiveSlackBusesMode().toParam().getValue());
    }

    @Test
    void testDefaultListsOfParametersIntegrity() {
        // testing default lists of parameters
        OpenReacParameters parameters = new OpenReacParameters();
        assertEquals(0, parameters.getVariableTwoWindingsTransformers().size(), "VariableTwoWindingsTransformers should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getSpecificVoltageLimits().size(), "SpecificVoltageLimits should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConstantQGenerators().size(), "ConstantQGenerators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getVariableShuntCompensators().size(), "VariableShuntCompensators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConfiguredReactiveSlackBuses().size(), "ConfiguredReactiveSlackBuses should be empty when using default OpenREacParameter constructor.");
        assertEquals(10, parameters.getAllAlgorithmParams().size());
    }

    @Test
    void testListsOfParametersIntegrity() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        String wrongId = "An id not in 57 cdf network.";

        testTwoWindingsTransformersParametersIntegrity(network, wrongId);
        testShuntCompensatorParametersIntegrity(network, wrongId);
        testConstantQGeneratorsParametersIntegrity(network, wrongId);
        testBusesWithReactiveSlacksParametersIntegrity(network, wrongId);
    }

    void testTwoWindingsTransformersParametersIntegrity(Network network, String wrongId) {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream().map(TwoWindingsTransformer::getId).collect(Collectors.toList()));
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Adding TwoWindingsTransformer network IDs should not throw.");
        parameters.addVariableTwoWindingsTransformers(List.of(wrongId));
        assertNull(network.getTwoWindingsTransformer(wrongId), "Please change wrong ID so it does not match any element in the network.");
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network));
        assertEquals("Two windings transformer " + wrongId + " not found in the network.", e.getMessage());
    }

    void testShuntCompensatorParametersIntegrity(Network network, String wrongId) {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableShuntCompensators(network.getShuntCompensatorStream().map(ShuntCompensator::getId).collect(Collectors.toList()));
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Adding ShuntCompensator network IDs should not throw.");
        parameters.addVariableShuntCompensators(List.of(wrongId));
        assertNull(network.getShuntCompensator(wrongId), "Please change wrong ID so it does not match any element in the network.");
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network));
        assertEquals("Shunt " + wrongId + " not found in the network.", e.getMessage());
    }

    void testConstantQGeneratorsParametersIntegrity(Network network, String wrongId) {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConstantQGenerators(network.getGeneratorStream().map(Generator::getId).collect(Collectors.toList()));
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Adding Generator network IDs should not throw.");
        parameters.addConstantQGenerators(List.of(wrongId));
        assertNull(network.getGenerator(wrongId), "Please change wrong ID so it does not match any element in the network.");
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network));
        assertEquals("Generator " + wrongId + " not found in the network.", e.getMessage());
    }

    void testBusesWithReactiveSlacksParametersIntegrity(Network network, String wrongId) {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addConfiguredReactiveSlackBuses(network.getBusView().getBusStream().map(Bus::getId).collect(Collectors.toList()));
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Adding Buses network IDs should not throw.");
        parameters.addConfiguredReactiveSlackBuses(List.of(wrongId));
        assertNull(network.getBusView().getBus(wrongId), "Please change wrong ID so it does not match any any element in the network.");
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network));
        assertEquals("Bus " + wrongId + " not found in the network.", e.getMessage());
    }

    void setDefaultVoltageLimits(Network network) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(0.8 * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(1.2 * vl.getNominalV());
            }
        }
    }

}
