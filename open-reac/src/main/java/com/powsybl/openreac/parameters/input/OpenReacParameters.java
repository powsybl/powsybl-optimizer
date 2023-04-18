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
import com.powsybl.openreac.parameters.input.algo.ObjectiveDistance;

import java.util.*;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacParameters {

    private final Map<String, VoltageLimitOverride> specificVoltageLimits = new HashMap<>();
    private final List<String> variableShuntCompensators = new ArrayList<>();
    private final List<String> constantQGenerators = new ArrayList<>();
    private final List<String> variableTwoWindingsTransformers = new ArrayList<>();
    private final List<OpenReacAlgoParam> genericParamsList = new ArrayList<>();
    private OpenReacOptimisationObjective objective = OpenReacOptimisationObjective.MIN_GENERATION;
    private Optional<ObjectiveDistance> objectiveDistance = Optional.empty();

    public OpenReacParameters() {
    }

    /**
     * Override some voltage level limits in the network. This will NOT modify the network object.
     * param specificVoltageLimits map containing keys : VoltageLevelId, and values are the low and high delta limits (kV).
     */
    public OpenReacParameters addSpecificVoltageLimits(Map<String, VoltageLimitOverride> specificVoltageLimits) {
        this.specificVoltageLimits.putAll(specificVoltageLimits);
        return this;
    }

    /**
     * A list of shunt compensators which susceptance should be considered as variable by the optimizer.
     * The optimizer computes a continuous value that is rounded when results are integrated in the network.
     */
    public OpenReacParameters addVariableShuntCompensators(List<String> shuntsIds) {
        this.variableShuntCompensators.addAll(shuntsIds);
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
     * The definition of the objective function for the optimization.
     */
    public OpenReacOptimisationObjective getObjective() {
        return this.objective;
    }

    /**
     * The definition of the objective function for the optimization.
     */
    public OpenReacParameters setObjective(OpenReacOptimisationObjective obj) {
        Objects.requireNonNull(obj);
        this.objective = obj;
        return this;
    }

    /**
     * A 0% objective means the model will target lower voltage limit.
     * <p>
     * A 100% objective means the model will target upper voltage limit.
     * @param objectiveDistance is in %
     * @see ObjectiveDistance is a parameter used for {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT}.
     */
    public OpenReacParameters setObjectiveDistance(double objectiveDistance) {
        this.objectiveDistance = Optional.of(new ObjectiveDistance(objectiveDistance / 100));
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
        if (objective != null) {
            allAlgoParams.add(objective);
        }
        objectiveDistance.ifPresent(allAlgoParams::add);
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
                throw new InvalidParametersException("Shunt " + shuntId + " not found in the network.");
            }
        }
        for (String genId : getConstantQGenerators()) {
            if (network.getGenerator(genId) == null) {
                throw new InvalidParametersException("Generator " + genId + " not found in the network.");
            }
        }
        for (String transformerId : getVariableTwoWindingsTransformers()) {
            if (network.getTwoWindingsTransformer(transformerId) == null) {
                throw new InvalidParametersException("Two windings transfromer " + transformerId + " not found in the network.");
            }
        }
        for (String voltageLevelId : getSpecificVoltageLimits().keySet()) {
            if (network.getVoltageLevel(voltageLevelId) == null) {
                throw new InvalidParametersException("Voltage level " + voltageLevelId + " not found in the network.");
            } else if (network.getVoltageLevel(voltageLevelId).getNominalV()
                    + getSpecificVoltageLimits().get(voltageLevelId).getDeltaLowVoltageLimit()
                    < 0) {
                throw new InvalidParametersException("Voltage level " + voltageLevelId + " override leads to negative lower voltage level.");
            }
        }

        if (objective.equals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT) && objectiveDistance.isEmpty()) {
            throw new InvalidParametersException("In using " + OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT +
                    " as objective, a distance in percent between low and high voltage limits is expected.");
        }

    }
}
