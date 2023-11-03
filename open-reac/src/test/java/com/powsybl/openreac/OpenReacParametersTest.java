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
    void testObjectiveIntegrityChecks() {
        Network network = IeeeCdfNetworkFactory.create118();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals(parameters.getObjective(), OpenReacOptimisationObjective.MIN_GENERATION);
        assertThrows(NullPointerException.class, () -> parameters.setObjective(null), "We can't unset objective function.");
        parameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Default configuration with only objective should be ok.");
        parameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT);
        assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network), "BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT without ratio voltage set should throw");
        parameters.setObjectiveDistance(1);
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Default configuration with BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT and ratio voltage set should not throw");
    }

    @Test
    void testAmplLogLevelIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelAmpl(null), "Can't set null ampl log level.");

        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG);
        assertEquals("DEBUG", parameters.getLogLevelAmpl().toParam().getValue());
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.INFO);
        assertEquals("INFO", parameters.getLogLevelAmpl().toParam().getValue());
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.WARNING);
        assertEquals("WARNING", parameters.getLogLevelAmpl().toParam().getValue());
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.ERROR);
        assertEquals("ERROR", parameters.getLogLevelAmpl().toParam().getValue());
    }

    @Test
    void testSolverLogLevelIntegrity() {
        OpenReacParameters parameters = new OpenReacParameters();

        assertThrows(NullPointerException.class, () -> parameters.setLogLevelSolver(null), "Can't set null solver log level.");
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        assertEquals("0", parameters.getLogLevelSolver().toParam().getValue());
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.ONLY_RESULTS);
        assertEquals("1", parameters.getLogLevelSolver().toParam().getValue());
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.EVERYTHING);
        assertEquals("2", parameters.getLogLevelSolver().toParam().getValue());
    }

    @Test
    void testAlgorithmParams() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addAlgorithmParam("param", "value");
        parameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
        parameters.setObjectiveDistance(0.4);
        parameters.setLogLevelAmpl(OpenReacAmplLogLevel.DEBUG);
        parameters.setLogLevelSolver(OpenReacSolverLogLevel.NOTHING);
        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();

        assertEquals(5, algoParams.size());
        assertEquals("value", algoParams.get(0).getValue());
        assertEquals("2", algoParams.get(1).getValue());
        assertEquals("0.004", algoParams.get(2).getValue());
        assertEquals("DEBUG", algoParams.get(3).getValue());
        assertEquals("0", algoParams.get(4).getValue());

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
        assertEquals(1, parameters.getAllAlgorithmParams().size());

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
