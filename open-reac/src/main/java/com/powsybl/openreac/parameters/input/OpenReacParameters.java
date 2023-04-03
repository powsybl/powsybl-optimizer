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
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacParameters {

    /**
     * This map allows to change the limits of some voltage levels.
     * <ul>
     *     <li>
     *         Key: VoltageLevel ID in the network
     *     </li>
     *     <li>
     *         Value: Pair for new voltage level limits
     *         <ul>
     *            <li>Left: lower relative delta</li>
     *            <li>Right: upper relatibe delta</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private final Map<String, Pair<Double, Double>> specificVoltageLimits;
    /**
     * List of network's shunts ID
     */
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

    public OpenReacParameters addVariableShuntCompensators(List<String> shuntsIds) {
        this.variableShuntCompensators.addAll(shuntsIds);
        return this;
    }

    /**
     * Override voltage level limits in the network. This will NOT modify the network object.
     * @param specificVoltageLimits map containing keys : VoltageLevelId, and values are the new lower and upper limits
     */
    public OpenReacParameters addSpecificVoltageLimit(Map<String,Pair<Double,Double>> specificVoltageLimits) {
        this.specificVoltageLimits.putAll(specificVoltageLimits);
        return this;
    }

    /**
     * Fix the reactance of the given generators during the OpenReac solve.
     * The reactance is constant to the reactance stored in the network.
     */
    public OpenReacParameters addConstantQGerenartors(List<String> generatorsIds) {
        this.constantQGerenartors.addAll(generatorsIds);
        return this;
    }

    /**
     * Tells OpenReac that it can modify the ratio of the given transformers.
     */
    public OpenReacParameters addVariableTwoWindingsTransformers(List<String> transformerIds) {
        this.variableTwoWindingsTransformers.addAll(transformerIds);
        return this;
    }

    public OpenReacParameters addAlgorithmParam(List<OpenReacAlgoParam> params) {
        this.algoParamsList.addAll(params);
        return this;
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
    }
}
