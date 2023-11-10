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
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
 */
public class OpenReacParametersTest {

    @Test
    public void testObjectiveIntegrityChecks() {
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
    public void testMinMaxVoltageLimitIntegrityChecks() {
        OpenReacParameters parameters = new OpenReacParameters();
        assertNull(parameters.getMinVoltageLimitConsistency());
        assertNull(parameters.getMaxVoltageLimitConsistency());

        // Consistency of min voltage limit (>= 0)
        assertThrows(InvalidParametersException.class, () -> parameters.setMinVoltageLimitConsistency(-0.25));
        parameters.setMinVoltageLimitConsistency(0.8);
        assertEquals(0.8, parameters.getMinVoltageLimitConsistency());

        // Consistency of high voltage limit (> 0)
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxVoltageLimitConsistency(-0.15));
        assertThrows(InvalidParametersException.class, () -> parameters.setMaxVoltageLimitConsistency(0));
        parameters.setMaxVoltageLimitConsistency(0.75);
        assertEquals(0.75, parameters.getMaxVoltageLimitConsistency());

        // Check min < max
        assertFalse(parameters.checkAlgorithmParametersIntegrity());
        parameters.setMaxVoltageLimitConsistency(1.2);
        assertTrue(parameters.checkAlgorithmParametersIntegrity());
    }

    @Test
    void testAlgorithmParams() {
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addAlgorithmParam("myParam", "myValue");
        parameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
        parameters.setObjectiveDistance(0.4);
        parameters.setMinVoltageLimitConsistency(0.8);
        parameters.setMaxVoltageLimitConsistency(1.2);
        List<OpenReacAlgoParam> algoParams = parameters.getAllAlgorithmParams();

        assertEquals(5, algoParams.size());
        assertEquals("myParam", algoParams.get(0).getName());
        assertEquals("myValue", algoParams.get(0).getValue());
        assertEquals("objective_choice", algoParams.get(1).getName());
        assertEquals("2", algoParams.get(1).getValue());
        assertEquals("ratio_voltage_target", algoParams.get(2).getName());
        assertEquals("0.004", algoParams.get(2).getValue());
        assertEquals("consistent_min_voltage", algoParams.get(3).getName());
        assertEquals("0.8", algoParams.get(3).getValue());
        assertEquals("consistent_max_voltage", algoParams.get(4).getName());
        assertEquals("1.2", algoParams.get(4).getValue());
    }

    @Test
    public void testParametersIntegrityChecks() {
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
