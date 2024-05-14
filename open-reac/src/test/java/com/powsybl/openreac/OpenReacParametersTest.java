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
 * @author Nicolas PIERRE {@literal <nicolas.pierre at artelys.com>}
 * @author Pierre ARVY {@literal <pierre.arvy at artelys.com>}
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
    void testActivePowerVariationRateIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setActivePowerVariationRate(0); // min value
        assertEquals(0., parameters.getActivePowerVariationRate());
        parameters.setActivePowerVariationRate(0.445556);
        assertEquals(0.445556, parameters.getActivePowerVariationRate());

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setActivePowerVariationRate(-1.2));
        assertEquals("Active power variation rate must be defined and between 0 and 1 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setActivePowerVariationRate(42));
        assertEquals("Active power variation rate must be defined and between 0 and 1 to be consistent.", e2.getMessage());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setActivePowerVariationRate(Double.NaN));
        assertEquals("Active power variation rate must be defined and between 0 and 1 to be consistent.", e3.getMessage());
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testZeroPowerThresholdIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setMinPlausibleActivePowerThreshold(0);
        assertEquals(0., parameters.getMinPlausibleActivePowerThreshold()); // min value
        parameters.setMinPlausibleActivePowerThreshold(2.365);
        assertEquals(2.365, parameters.getMinPlausibleActivePowerThreshold());

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinPlausibleActivePowerThreshold(-1.2));
        assertEquals("Zero power threshold must be defined and >= 0 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinPlausibleActivePowerThreshold(Double.NaN));
        assertEquals("Zero power threshold must be defined and >= 0 to be consistent.", e2.getMessage());
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testZeroImpedanceThresholdIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setLowImpedanceThreshold(0);
        assertEquals(0., parameters.getLowImpedanceThreshold()); // min value
        parameters.setLowImpedanceThreshold(1e-5);
        assertEquals(1e-5, parameters.getLowImpedanceThreshold());

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setLowImpedanceThreshold(-1.2));
        assertEquals("Zero impedance threshold must be defined and >= 0 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setLowImpedanceThreshold(Double.NaN));
        assertEquals("Zero impedance threshold must be defined and >= 0 to be consistent.", e2.getMessage());
    }

    @Test
    void testNominalThresholdsIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setMinNominalVoltageIgnoredBus(0); // min value
        assertEquals(0, parameters.getMinNominalVoltageIgnoredBus());
        parameters.setMinNominalVoltageIgnoredBus(45);
        assertEquals(45, parameters.getMinNominalVoltageIgnoredBus());
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinNominalVoltageIgnoredBus(-1.2));
        assertEquals("Nominal threshold for ignored buses must be defined and >= 0 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinNominalVoltageIgnoredBus(Double.NaN));
        assertEquals("Nominal threshold for ignored buses must be defined and >= 0 to be consistent.", e2.getMessage());

        parameters.setMinNominalVoltageIgnoredVoltageBounds(200);
        assertEquals(200, parameters.getMinNominalVoltageIgnoredVoltageBounds());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinNominalVoltageIgnoredVoltageBounds(-1.2));
        assertEquals("Nominal threshold for ignored voltage bounds must be defined and >= 0 to be consistent", e3.getMessage());
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> parameters.setMinNominalVoltageIgnoredVoltageBounds(Double.NaN));
        assertEquals("Nominal threshold for ignored voltage bounds must be defined and >= 0 to be consistent", e4.getMessage());

        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testPMinMaxIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setPQMax(5775);
        assertEquals(5775, parameters.getPQMax());
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setPQMax(0)); // min value
        assertEquals("Maximal consistency value for P and Q must be defined and > 0 to be consistent", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setPQMax(-2.1));
        assertEquals("Maximal consistency value for P and Q must be defined and > 0 to be consistent", e2.getMessage());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setPQMax(Double.NaN));
        assertEquals("Maximal consistency value for P and Q must be defined and > 0 to be consistent", e3.getMessage());

        parameters.setLowActivePowerDefaultLimit(1500);
        assertEquals(1500, parameters.getLowActivePowerDefaultLimit());
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> parameters.setLowActivePowerDefaultLimit(-100));
        assertEquals("Default P min value must be defined and >= 0 to be consistent.", e4.getMessage());
        IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> parameters.setLowActivePowerDefaultLimit(Double.NaN));
        assertEquals("Default P min value must be defined and >= 0 to be consistent.", e5.getMessage());

        parameters.setHighActivePowerDefaultLimit(1250);
        assertEquals(1250, parameters.getHighActivePowerDefaultLimit());
        IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class, () -> parameters.setHighActivePowerDefaultLimit(0));
        assertEquals("Default P max value must be defined and > 0 to be consistent.", e6.getMessage());
        IllegalArgumentException e7 = assertThrows(IllegalArgumentException.class, () -> parameters.setHighActivePowerDefaultLimit(-100));
        assertEquals("Default P max value must be defined and > 0 to be consistent.", e7.getMessage());
        IllegalArgumentException e8 = assertThrows(IllegalArgumentException.class, () -> parameters.setHighActivePowerDefaultLimit(Double.NaN));
        assertEquals("Default P max value must be defined and > 0 to be consistent.", e8.getMessage());

        assertFalse(parameters.checkAlgorithmParametersIntegrity()); // case defaultPmin > defaultPmax
        parameters.setHighActivePowerDefaultLimit(10000);
        assertFalse(parameters.checkAlgorithmParametersIntegrity()); // case defaultPmax > pQmax
        parameters.setLowActivePowerDefaultLimit(50).setHighActivePowerDefaultLimit(1000);
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testDefaultQmaxPmaxRatioIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setDefaultQmaxPmaxRatio(0.778);
        assertEquals(0.778, parameters.getDefaultQmaxPmaxRatio());
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultQmaxPmaxRatio(0));
        assertEquals("Default Qmax and Pmax ratio must be defined and > 0 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultQmaxPmaxRatio(-0.3));
        assertEquals("Default Qmax and Pmax ratio must be defined and > 0 to be consistent.", e2.getMessage());
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultQmaxPmaxRatio(Double.NaN));
        assertEquals("Default Qmax and Pmax ratio must be defined and > 0 to be consistent.", e3.getMessage());

        parameters.setDefaultQmaxPmaxRatio(500);
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testDefaultMinimalQPRangeIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setDefaultMinimalQPRange(10);
        assertEquals(10, parameters.getDefaultMinimalQPRange());
        parameters.setDefaultMinimalQPRange(0);
        assertEquals(0, parameters.getDefaultMinimalQPRange());

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultMinimalQPRange(-1.5));
        assertEquals("Default minimal QP range must be defined and >= 0 to be consistent.", e1.getMessage());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> parameters.setDefaultMinimalQPRange(Double.NaN));
        assertEquals("Default minimal QP range must be defined and >= 0 to be consistent.", e2.getMessage());
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
        parameters.setActivePowerVariationRate(0.56);
        parameters.setMinPlausibleActivePowerThreshold(0.5);
        parameters.setLowImpedanceThreshold(1e-5);
        parameters.setMinNominalVoltageIgnoredBus(10);
        parameters.setMinNominalVoltageIgnoredVoltageBounds(5);
        parameters.setPQMax(8555.3);
        parameters.setLowActivePowerDefaultLimit(99.2);
        parameters.setHighActivePowerDefaultLimit(1144);
        parameters.setDefaultQmaxPmaxRatio(0.4);
        parameters.setDefaultMinimalQPRange(1.1);
        parameters.setDefaultVariableScalingFactor(0.5);
        parameters.setDefaultConstraintScalingFactor(0.75);
        parameters.setReactiveSlackVariableScalingFactor(1e-2);
        parameters.setTwoWindingTransformerRatioVariableScalingFactor(0.0001);

        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();
        assertEquals(21, algoParams.size());
        assertEquals("2", algoParams.get(0).getValue());
        assertEquals("0.4", algoParams.get(1).getValue());
        assertEquals("DEBUG", algoParams.get(2).getValue());
        assertEquals("0", algoParams.get(3).getValue());
        assertEquals("0.8", algoParams.get(4).getValue());
        assertEquals("1.2", algoParams.get(5).getValue());
        assertEquals("ALL", algoParams.get(6).getValue());
        assertEquals("0.56", algoParams.get(7).getValue());
        assertEquals("0.5", algoParams.get(8).getValue());
        assertEquals("1.0E-5", algoParams.get(9).getValue());
        assertEquals("10.0", algoParams.get(10).getValue());
        assertEquals("5.0", algoParams.get(11).getValue());
        assertEquals("8555.3", algoParams.get(12).getValue());
        assertEquals("99.2", algoParams.get(13).getValue());
        assertEquals("1144.0", algoParams.get(14).getValue());
        assertEquals("0.4", algoParams.get(15).getValue());
        assertEquals("1.1", algoParams.get(16).getValue());
        assertEquals("0.5", algoParams.get(17).getValue());
        assertEquals("0.75", algoParams.get(18).getValue());
        assertEquals("0.01", algoParams.get(19).getValue());
        assertEquals("1.0E-4", algoParams.get(20).getValue());
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
    void testDefaultParametersValuesIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        assertEquals(OpenReacOptimisationObjective.MIN_GENERATION, parameters.getObjective());
        assertNull(parameters.getObjectiveDistance());
        assertEquals(OpenReacAmplLogLevel.INFO, parameters.getLogLevelAmpl());
        assertEquals(OpenReacSolverLogLevel.EVERYTHING, parameters.getLogLevelSolver());
        assertEquals(0.5, parameters.getMinPlausibleLowVoltageLimit());
        assertEquals(1.5, parameters.getMaxPlausibleHighVoltageLimit());
        assertEquals(ReactiveSlackBusesMode.NO_GENERATION, parameters.getReactiveSlackBusesMode());
        assertEquals(1., parameters.getActivePowerVariationRate());
        assertEquals(0.01, parameters.getMinPlausibleActivePowerThreshold());
        assertEquals(1e-4, parameters.getLowImpedanceThreshold());
        assertEquals(1., parameters.getMinNominalVoltageIgnoredBus());
        assertEquals(0., parameters.getMinNominalVoltageIgnoredVoltageBounds());
        assertEquals(9000., parameters.getPQMax());
        assertEquals(0, parameters.getLowActivePowerDefaultLimit());
        assertEquals(1000., parameters.getHighActivePowerDefaultLimit());
        assertEquals(0.3, parameters.getDefaultQmaxPmaxRatio());
        assertEquals(1., parameters.getDefaultMinimalQPRange());
        // TODO : add cases for scaling values
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testDefaultParametersListsIntegrity() {
        // testing default lists of parameters
        OpenReacParameters parameters = new OpenReacParameters();
        assertEquals(0, parameters.getVariableTwoWindingsTransformers().size(), "VariableTwoWindingsTransformers should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getSpecificVoltageLimits().size(), "SpecificVoltageLimits should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConstantQGenerators().size(), "ConstantQGenerators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getVariableShuntCompensators().size(), "VariableShuntCompensators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConfiguredReactiveSlackBuses().size(), "ConfiguredReactiveSlackBuses should be empty when using default OpenREacParameter constructor.");
        assertEquals(20, parameters.getAllAlgorithmParams().size());
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
