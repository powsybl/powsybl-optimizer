package com.powsybl.openreac;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class OpenReacParametersTest {
    @Test
    public void testObjectiveIntegrityChecks() {
        Network network = IeeeCdfNetworkFactory.create118();
        OpenReacParameters parameters = new OpenReacParameters();

        assertNull(parameters.getObjective(), "Objective must be explicitly defined by the user.");
        assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network), "No objective defined should throw.");
        assertThrows(NullPointerException.class, () -> parameters.setObjective(null), "We can't unset objective function.");
        parameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Default configuration with only objective should be ok.");
        parameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE);
        assertThrows(InvalidParametersException.class, () -> parameters.checkIntegrity(network), "BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE without ratio voltage set should throw");
        parameters.setRatioVoltageObjective(1);
        assertDoesNotThrow(() -> parameters.checkIntegrity(network), "Default configuration with BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE and ratio voltage set should not throw");
    }

    @Test
    public void testParametersIntegrityChecks() {
        Network network = IeeeCdfNetworkFactory.create118();
        String wrongId = "An id not in 118 cdf network.";
        OpenReacParameters parameters = new OpenReacParameters();

        assertEquals(0, parameters.getVariableTwoWindingsTransformers().size(), "VariableTwoWindingsTransformers should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getSpecificVoltageLimits().size(), "SpecificVoltageLimits should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getConstantQGenerators().size(), "ConstantQGenerators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getVariableShuntCompensators().size(), "VariableShuntCompensators should be empty when using default OpenReacParameter constructor.");
        assertEquals(0, parameters.getAllAlgorithmParams().size(), "AllAlgorithmParams should be empty when using default OpenReacParameter constructor.");

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

}
