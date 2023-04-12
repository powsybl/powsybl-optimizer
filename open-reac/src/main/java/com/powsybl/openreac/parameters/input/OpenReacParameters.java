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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacParameters.class);

    private final Map<String, VoltageLimitOverride> specificVoltageLimits;
    private final List<String> variableShuntCompensators;
    private final List<String> constantQGenerators;
    private final List<String> variableTwoWindingsTransformers;
    private final List<OpenReacAlgoParam> genericParamsList;
    private OpenReacOptimisationObjective objective;
    private Optional<OptimisationVoltageRatio> objVoltageRatio;

    public OpenReacParameters() {
        this.variableShuntCompensators = new ArrayList<>();
        this.constantQGenerators = new ArrayList<>();
        this.variableTwoWindingsTransformers = new ArrayList<>();
        this.specificVoltageLimits = new HashMap<>();
        this.genericParamsList = new ArrayList<>();
        this.objective = null;
        this.objVoltageRatio = Optional.empty();
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
     *
     * @param specificVoltageLimits map containing keys : VoltageLevelId, and VoltageLimitOverride with absolute values.
     */
    public OpenReacParameters addSpecificVoltageLimits(Map<String, VoltageLimitOverride> specificVoltageLimits) {
        this.specificVoltageLimits.putAll(specificVoltageLimits);
        return this;
    }

    /**
     * A list of generators that are not controlling voltage during the optimization.
     * The reactive power produced by the generator is constant and equals `targetQ`.
     */
    public OpenReacParameters addConstantQGenerators(List<String> generatorsIds) {
        this.constantQGenerators.addAll(generatorsIds);
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
        this.genericParamsList.addAll(params);
        return this;
    }

    /**
     * @see OptimisationVoltageRatio
     */
    public OpenReacParameters setRatioVoltageObjective(double ratio) {
        this.objVoltageRatio = Optional.of(new OptimisationVoltageRatio(ratio));
        return this;
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    public Map<String, VoltageLimitOverride> getSpecificVoltageLimits() {
        return specificVoltageLimits;
    }

    public List<String> getConstantQGenerators() {
        return constantQGenerators;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
    }

    public List<OpenReacAlgoParam> getAllAlgorithmParams() {
        ArrayList<OpenReacAlgoParam> allAlgoParams = new ArrayList<>(genericParamsList.size() + 2);
        allAlgoParams.addAll(genericParamsList);
        if(objective != null){
            allAlgoParams.add(objective);
        }
        objVoltageRatio.ifPresent(allAlgoParams::add);
        return allAlgoParams;
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
        if(objective == null){
           throw new InvalidParametersException("You must define a valid optimization objective. Use OpenReacParameters.setObjective");
        }
        if (objective.equals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE) && objVoltageRatio.isEmpty()) {
            throw new InvalidParametersException("Using " + OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE +
                    " as objective, you must set the ratio with OpenReacParameters.setRatioVoltageObjective");
        }

    }

    public OpenReacOptimisationObjective getObjective() {
        return this.objective;
    }

    public OpenReacParameters setObjective(OpenReacOptimisationObjective obj) {
        Objects.requireNonNull(obj);
        this.objective = obj;
        return this;
    }
}
