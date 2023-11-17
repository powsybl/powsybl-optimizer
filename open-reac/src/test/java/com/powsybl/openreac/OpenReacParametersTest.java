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
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
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
        assertThrows(NullPointerException.class, () -> parameters.setObjective(null), "Can't unset objective function.");

        // Objective distance for BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT objective
        parameters.setObjectiveDistance(0.); // min value
        assertEquals(0., parameters.getObjectiveDistance());
        parameters.setObjectiveDistance(1.); // max value
        assertEquals(1., parameters.getObjectiveDistance());
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(-2.), "Objective distance must be > 0");
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(1.2), "Objective distance must be < 1");
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(Double.NaN), "Objective distance must be defined.");

        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testMinMaxVoltageLimitIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        // Consistency of min plausible low voltage limit (>= 0)
        parameters.setMinPlausibleLowVoltageLimit(0.);
        assertEquals(0., parameters.getMinPlausibleLowVoltageLimit()); // min value
        parameters.setMinPlausibleLowVoltageLimit(0.8211);
        assertEquals(0.8211, parameters.getMinPlausibleLowVoltageLimit());
        assertThrows(InvalidParametersException.class, () -> parameters.setMinPlausibleLowVoltageLimit(-0.25), "minPlausibleLowVoltageLimit must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setMinPlausibleLowVoltageLimit(Double.NaN), "minPlausibleLowVoltageLimit must be defined.");

        // Consistency of max plausible high voltage limit (> 0)
        parameters.setMaxPlausibleHighVoltageLimit(0.75);
        assertEquals(0.75, parameters.getMaxPlausibleHighVoltageLimit());
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(-0.15), "maxPlausibleHighVoltageLimit must be > 0.");
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(0), "maxPlausibleHighVoltageLimit must be > 0");
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(Double.NaN), "maxPlausibleHighVoltageLimit must be defined.");

        // Check min < max
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
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
    void testDefault() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setDefaultMinimalQPRange(10.);
        assertEquals(10., parameters.getDefaultMinimalQPRange());
        parameters.setDefaultMinimalQPRange(0.);
        assertEquals(0., parameters.getDefaultMinimalQPRange());

        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultMinimalQPRange(-1.5));
        assertThrows(InvalidParametersException.class, () -> parameters.setDefaultMinimalQPRange(Double.NaN));
    }

    @Test
    void testDefaultValuesIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();
        assertEquals(OpenReacOptimisationObjective.MIN_GENERATION, parameters.getObjective());
        assertEquals(0.5, parameters.getObjectiveDistance());
        assertEquals(0.5, parameters.getMinPlausibleLowVoltageLimit());
        assertEquals(1.5, parameters.getMaxPlausibleHighVoltageLimit());
        assertEquals(1., parameters.getAlphaCoefficient());
        assertEquals(0.01, parameters.getZeroPowerThreshold());
        assertEquals(1e-4, parameters.getZeroImpedanceThreshold());
        assertEquals(1., parameters.getNominalThresholdIgnoredBuses());
        assertEquals(0., parameters.getNominalThresholdIgnoredVoltageBounds());
        assertEquals(9000., parameters.getPQMax());
        assertEquals(0, parameters.getDefaultPMin());
        assertEquals(1000., parameters.getDefaultPMax());
        assertEquals(0.3, parameters.getDefaultQmaxPmaxRatio());
        assertEquals(1., parameters.getDefaultMinimalQPRange());
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testParametersIntegrityChecks() {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        String wrongId = "An id not in 118 cdf network.";
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals(0, parameters.getVariableTwoWindingsTransformers().size(), "VariableTwoWindingsTransformers should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getSpecificVoltageLimits().size(), "SpecificVoltageLimits should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConstantQGenerators().size(), "ConstantQGenerators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getVariableShuntCompensators().size(), "VariableShuntCompensators should be empty when using default OpenReacParameter constructor.");
        assertEquals(14, parameters.getAllAlgorithmParams().size());

        // adding an objective, to have a valid OpenReacParameter object
        parameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);
        // testing TwoWindingsTransformer
        parameters.addVariableTwoWindingsTransformers(network.getTwoWindingsTransformerStream().map(TwoWindingsTransformer::getId).collect(Collectors.toList()));
        OpenReacParameters lambdaParams = parameters; // for the lambdas to compile
        assertDoesNotThrow(() -> lambdaParams.checkIntegrity(network), "Adding TwoWindingsTransformer network IDs should not throw.");
        parameters.addVariableTwoWindingsTransformers(List.of(wrongId));
        assertNull(network.getTwoWindingsTransformer(wrongId), "Please change wrong ID so it does not match any element in the network.");
        assertThrows(InvalidParametersException.class, () -> lambdaParams.checkIntegrity(network), "An ID TwoWindingsTransformer not present in the network should throw to the user.");

        // Reseting parameters
        parameters = new OpenReacParameters();
        parameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);

        // testing ShuntCompensator
        parameters.addVariableShuntCompensators(network.getShuntCompensatorStream().map(ShuntCompensator::getId).collect(Collectors.toList()));
        OpenReacParameters lambdaParamsShunts = parameters; // for the lambdas to compile
        assertDoesNotThrow(() -> lambdaParamsShunts.checkIntegrity(network), "Adding ShuntCompensator network IDs should not throw.");
        parameters.addVariableShuntCompensators(List.of(wrongId));
        assertNull(network.getShuntCompensator(wrongId), "Please change wrong ID so it does not match any element in the network.");
        assertThrows(InvalidParametersException.class, () -> lambdaParamsShunts.checkIntegrity(network), "An ShuntCompensator ID not present in the network should throw to the user.");

        // Reseting parameters
        parameters = new OpenReacParameters();
        parameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);

        // testing Generator
        parameters.addConstantQGenerators(network.getGeneratorStream().map(Generator::getId).collect(Collectors.toList()));
        OpenReacParameters lambdaParamsGenerators = parameters; // for the lambdas to compile
        assertDoesNotThrow(() -> lambdaParamsGenerators.checkIntegrity(network), "Adding Generator network IDs should not throw.");
        parameters.addConstantQGenerators(List.of(wrongId));
        assertNull(network.getGenerator(wrongId), "Please change wrong ID so it does not match any element in the network.");
        assertThrows(InvalidParametersException.class, () -> lambdaParamsGenerators.checkIntegrity(network), "An Generator ID not present in the network should throw to the user.");
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
