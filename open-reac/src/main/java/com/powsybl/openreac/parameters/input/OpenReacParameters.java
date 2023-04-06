/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParam;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OptimisationVoltageRatio;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacParameters.class);

    private final Map<String, Pair<Double, Double>> specificVoltageLimits;
    private final List<String> variableShuntCompensators;
    private final List<String> constantQGerenartors;
    private final List<String> variableTwoWindingsTransformers;
    private final List<OpenReacAlgoParam> algoParamsList;

    public OpenReacParameters() {
        this.variableShuntCompensators = new ArrayList<>();
        this.constantQGerenartors = new ArrayList<>();
        this.variableTwoWindingsTransformers = new ArrayList<>();
        this.specificVoltageLimits = new HashMap<>();
        this.algoParamsList = new ArrayList<>();
    }

    /**
     * A list of shunt compensators which susceptance should be considered as variable by the optimizer.
     * The otpimizer computes a continuous value that is rounded when results are integrated in the network.
     */
    public OpenReacParameters addVariableShuntCompensators(List<String> shuntsIds) {
        this.variableShuntCompensators.addAll(shuntsIds);
        return this;
    }

    /**
     * Override some voltage level limits in the network. This will NOT modify the network object.
     * @param specificVoltageLimits map containing keys : VoltageLevelId, and values are the low and high delta limits.
     */
    public OpenReacParameters addSpecificVoltageLimit(Map<String, Pair<Double, Double>> specificVoltageLimits) {
        this.specificVoltageLimits.putAll(specificVoltageLimits);
        return this;
    }

    /**
     * A list of generators that are not controlling voltage during the optimization.
     * The reactive power produced by the generator is constant and equals `targetQ`.
     */
    public OpenReacParameters addConstantQGerenartors(List<String> generatorsIds) {
        this.constantQGerenartors.addAll(generatorsIds);
        return this;
    }

    /**
     * A list of two windings transformers which ratio should be considered as variable by the optimizer.
     */
    public OpenReacParameters addVariableTwoWindingsTransformers(List<String> transformerIds) {
        this.variableTwoWindingsTransformers.addAll(transformerIds);
        return this;
    }

    public OpenReacParameters addAlgorithmParam(List<OpenReacAlgoParam> params) {
        this.algoParamsList.addAll(params);
        return this;
    }

    /**
     * Will use {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE} and {@link OptimisationVoltageRatio}.
     * DO NOT CALL THIS MULTIPLE TIMES.
     */
    public OpenReacParameters addRatioVoltageObjective(double ratio) {
        return addAlgorithmParam(List.of(new OptimisationVoltageRatio(ratio), OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE));
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    public Map<String, Pair<Double, Double>> getSpecificVoltageDelta() {
        return specificVoltageLimits;
    }

    public List<String> getConstantQGenerators() {
        return constantQGerenartors;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
    }

    public List<OpenReacAlgoParam> getAlgorithmParams() {
        return algoParamsList;
    }

    /**
     * Do some checks on the parameters given, such as provided IDs must correspond to the given network element
     *
     * @param network Network on which ID are going to be infered
     * @throws InvalidParametersException
     */
    public void checkIntegrity(Network network) throws InvalidParametersException {
        for (String shuntId : getVariableShuntCompensators()) {
            if (network.getShuntCompensator(shuntId) == null) {
                throw new InvalidParametersException(shuntId + " is not a valid Shunt ID in the network: " + network.getNameOrId());
            }
        }
        for (String genId : getConstantQGenerators()) {
            if (network.getGenerator(genId) == null) {
                throw new InvalidParametersException(genId + " is not a valid generator ID in the network: " + network.getNameOrId());
            }
        }
        for (String transformerId : getVariableTwoWindingsTransformers()) {
            if (network.getTwoWindingsTransformer(transformerId) == null) {
                throw new InvalidParametersException(transformerId + " is not a valid transformer ID in the network: " + network.getNameOrId());
            }
        }
        checkDuplicate(true, OpenReacOptimisationObjective.class);
        checkDuplicate(false, OptimisationVoltageRatio.class);
        if (isParameterPresent(OptimisationVoltageRatio.class)) {
            if (getAlgorithmParams().stream().noneMatch(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE::equals)) {
                throw new InvalidParametersException("Parameter 'OptimisationVoltageRatio' must be used with 'BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE'");
            }
        }

    }

    /**
     * @return <code>true</code> if clazz is present in algo parameters.
     */
    private boolean isParameterPresent(Class<? extends OpenReacAlgoParam> clazz) {
        return getAlgorithmParams().stream().anyMatch(clazz::isInstance);
    }

    /**
     * Check that the given parameter is unique in the parameter list.
     *
     * @param shouldThrow when it finds duplicates it will throw an exception if <code>true</code>, else it will only log a warning.
     */
    private void checkDuplicate(boolean shouldThrow, Class<? extends OpenReacAlgoParam> clazz) {
        String duplicateMessage = "Using multiple " + clazz + " parameters. It is undefined ratio will be used by OpenReac.";
        if (getAlgorithmParams().stream().filter(clazz::isInstance).count() > 1) {
            if (shouldThrow) {
                throw new InvalidParametersException(duplicateMessage);
            } else {
                LOGGER.warn(duplicateMessage);
            }
        }
    }
}
