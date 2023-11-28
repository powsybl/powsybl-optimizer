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
    void testAlphaCoefficientIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setAlphaCoefficient(0.); // min value
        assertEquals(0., parameters.getAlphaCoefficient());
        parameters.setAlphaCoefficient(0.445556);
        assertEquals(0.445556, parameters.getAlphaCoefficient());

        assertThrows(InvalidParametersException.class, () -> parameters.setAlphaCoefficient(-1.2), "alphaCoefficient must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setAlphaCoefficient(42.), "alphaCoefficient must be < 1.");
        assertThrows(InvalidParametersException.class, () -> parameters.setAlphaCoefficient(Double.NaN), "alphaCoefficient must be defined.");
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testZeroPowerThresholdIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setZeroPowerThreshold(0.);
        assertEquals(0., parameters.getZeroPowerThreshold()); // min value
        parameters.setZeroPowerThreshold(2.365);
        assertEquals(2.365, parameters.getZeroPowerThreshold());

        assertThrows(InvalidParametersException.class, () -> parameters.setZeroPowerThreshold(-1.2), "zeroPowerThreshold must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setZeroPowerThreshold(Double.NaN), "zeroPowerThreshold must be defined.");
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testZeroImpedanceThresholdIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setZeroImpedanceThreshold(0.);
        assertEquals(0., parameters.getZeroImpedanceThreshold()); // min value
        parameters.setZeroImpedanceThreshold(1e-5);
        assertEquals(1e-5, parameters.getZeroImpedanceThreshold());

        assertThrows(InvalidParametersException.class, () -> parameters.setZeroImpedanceThreshold(-1.2), "zeroImpedanceThreshold must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setZeroImpedanceThreshold(Double.NaN), "zeroImpedanceThreshold must be defined.");
    }

    @Test
    void testNominalThresholdsIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setNominalThresholdIgnoredBuses(0.); // min value
        assertEquals(0., parameters.getNominalThresholdIgnoredBuses());
        parameters.setNominalThresholdIgnoredBuses(45.);
        assertEquals(45., parameters.getNominalThresholdIgnoredBuses());
        assertThrows(InvalidParametersException.class, () -> parameters.setNominalThresholdIgnoredBuses(-1.2), "nominalThresholdIgnoredBuses must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setNominalThresholdIgnoredBuses(Double.NaN), "nominalThresholdIgnoredBuses must be defined.");

        parameters.setNominalThresholdIgnoredVoltageBounds(200.);
        assertEquals(200., parameters.getNominalThresholdIgnoredVoltageBounds());
        assertThrows(InvalidParametersException.class, () -> parameters.setNominalThresholdIgnoredVoltageBounds(-1.2), "nominalThresholdIgnoredVoltageBounds must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setNominalThresholdIgnoredVoltageBounds(Double.NaN), "nominalThresholdIgnoredVoltageBounds must be defined.");

        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testPMinMaxIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setPQMax(5775.);
        assertEquals(5775., parameters.getPQMax());
        assertThrows(InvalidParametersException.class, () -> parameters.setPQMax(0.), "pQmax  must be > 0."); // min value
        assertThrows(InvalidParametersException.class, () -> parameters.setPQMax(-2.1), "pQmax must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setPQMax(Double.NaN));

        parameters.setDefaultPMin(1500.);
        assertEquals(1500., parameters.getDefaultPMin());
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultPMin(-100.), "defaultPmin must be >= 0");
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultPMin(Double.NaN));

        parameters.setDefaultPMax(1250.);
        assertEquals(1250., parameters.getDefaultPMax());
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultPMax(0.), "defaultPmax must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultPMax(-100.), "defaultPmax must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultPMax(Double.NaN));

        assertFalse(parameters.checkAlgorithmParametersIntegrity()); // case defaultPmin > defaultPmax
        parameters.setDefaultPMax(10000.);
        assertFalse(parameters.checkAlgorithmParametersIntegrity()); // case defaultPmax > pQmax
        parameters.setDefaultPMin(50.).setDefaultPMax(1000.);
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testDefaultQmaxPmaxRatioIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setDefaultQmaxPmaxRatio(0.778);
        assertEquals(0.778, parameters.getDefaultQmaxPmaxRatio());
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultQmaxPmaxRatio(0.), "defaultQmaxPmaxRatio must be > 0");
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultQmaxPmaxRatio(-0.3), "defaultQmaxPmaxRatio must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultQmaxPmaxRatio(Double.NaN), "defaultQmaxPmaxRatio must be defined.");

        parameters.setDefaultQmaxPmaxRatio(500.);
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testDefaultMinimalQPRangeIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setDefaultMinimalQPRange(10.);
        assertEquals(10., parameters.getDefaultMinimalQPRange());
        parameters.setDefaultMinimalQPRange(0.);
        assertEquals(0., parameters.getDefaultMinimalQPRange());

        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultMinimalQPRange(-1.5));
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultMinimalQPRange(Double.NaN));
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

        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();
        assertEquals(7, algoParams.size());
        assertEquals("2", algoParams.get(0).getValue());
        assertEquals("0.4", algoParams.get(1).getValue());
        assertEquals("DEBUG", algoParams.get(2).getValue());
        assertEquals("0", algoParams.get(3).getValue());
        assertEquals("0.8", algoParams.get(4).getValue());
        assertEquals("1.2", algoParams.get(5).getValue());
        assertEquals("ALL", algoParams.get(6).getValue());
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
        assertEquals(6, parameters.getAllAlgorithmParams().size());
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
