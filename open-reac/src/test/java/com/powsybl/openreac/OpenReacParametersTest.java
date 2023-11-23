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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
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
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(-2.), "Objective distance must be >= 0");
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(1.02), "Objective distance must be <= 1");
        assertThrows(IllegalArgumentException.class, () -> parameters.setObjectiveDistance(Double.NaN), "Objective distance must be defined.");

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

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelAmpl(null), "Can't set null ampl log level.");
    }

    @Test
    void testSolverLogLevelIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals("2", parameters.getLogLevelSolver().toParam().getValue()); // default value
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        assertEquals("0", parameters.getLogLevelSolver().toParam().getValue());
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS);
        assertEquals("1", parameters.getLogLevelSolver().toParam().getValue());

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelSolver(null), "Can't set null solver log level.");
    }

    @Test
    void testMinMaxVoltageLimitIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        // Consistency of min plausible low voltage limit (>= 0)
        assertEquals(0.5, parameters.getMinPlausibleLowVoltageLimit()); // default value
        parameters.setMinPlausibleLowVoltageLimit(0.8);
        assertEquals(0.8, parameters.getMinPlausibleLowVoltageLimit());
        assertThrows(InvalidParametersException.class, () -> parameters.setMinPlausibleLowVoltageLimit(-0.25));
        assertThrows(InvalidParametersException.class, () -> parameters.setMinPlausibleLowVoltageLimit(Double.NaN));

        // Consistency of max plausible high voltage limit (> 0)
        assertEquals(1.5, parameters.getMaxPlausibleHighVoltageLimit()); // default value
        parameters.setMaxPlausibleHighVoltageLimit(0.75);
        assertEquals(0.75, parameters.getMaxPlausibleHighVoltageLimit());
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(-0.15));
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(0));
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxPlausibleHighVoltageLimit(Double.NaN));

        // Check min < max
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
        parameters.setMaxPlausibleHighVoltageLimit(1.2);
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testAlgorithmParams() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
        parameters.setObjectiveDistance(0.4);
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG);
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        parameters.setMinPlausibleLowVoltageLimit(0.8);
        parameters.setMaxPlausibleHighVoltageLimit(1.2);
        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();

        assertEquals(6, algoParams.size());
        assertEquals("2", algoParams.get(0).getValue());
        assertEquals("0.4", algoParams.get(1).getValue());
        assertEquals("DEBUG", algoParams.get(2).getValue());
        assertEquals("0", algoParams.get(3).getValue());
        assertEquals("0.8", algoParams.get(4).getValue());
        assertEquals("1.2", algoParams.get(5).getValue());
    }

    @Test
    public void testParametersIntegrity() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        String wrongId = "An id not in 118 cdf network.";
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals(0, parameters.getVariableTwoWindingsTransformers().size(), "VariableTwoWindingsTransformers should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getSpecificVoltageLimits().size(), "SpecificVoltageLimits should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConstantQGenerators().size(), "ConstantQGenerators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getVariableShuntCompensators().size(), "VariableShuntCompensators should be empty when using default OpenReacParameter constructor.");
        assertEquals(6, parameters.getAllAlgorithmParams().size());

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
