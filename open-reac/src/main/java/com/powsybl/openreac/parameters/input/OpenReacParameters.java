/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParam;
import com.powsybl.openreac.parameters.input.algo.OpenReacAlgoParamImpl;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import org.jgrapht.alg.util.Pair;
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

    private static final String OBJECTIVE_DISTANCE_KEY = "ratio_voltage_target";

    private final List<Pair<String, VoltageLimitOverride>> specificVoltageLimits = new ArrayList<>();
    private final List<String> variableShuntCompensators = new ArrayList<>();
    private final List<String> constantQGenerators = new ArrayList<>();
    private final List<String> variableTwoWindingsTransformers = new ArrayList<>();
    private final List<OpenReacAlgoParam> algorithmParams = new ArrayList<>();
    private OpenReacOptimisationObjective objective = OpenReacOptimisationObjective.MIN_GENERATION;

    /*
     * Must be used with {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT}
     * to define the voltage between low and high voltage limits, which OpenReac should converge to.
     * Zero percent means that it should converge to low voltage limits. 100 percents means that it should
     * converge to high voltage limits.
     */
    private Double objectiveDistance;

    /**
     * Override some voltage level limits in the network. This will NOT modify the network object.
     * <p>
     * The override is ignored if one or both of the voltage limit are NaN.
     * @param specificVoltageLimits keys: a VoltageLevel ID, values: low and high delta limits (kV).
     */
    public OpenReacParameters addSpecificVoltageLimits(List<Pair<String, VoltageLimitOverride>> specificVoltageLimits) {
        this.specificVoltageLimits.addAll(Objects.requireNonNull(specificVoltageLimits));
        return this;
    }

    /**
     * A list of shunt compensators, which susceptance will be considered as variable by the optimizer.
     * The optimizer computes a continuous value that is rounded when results are stored in {@link com.powsybl.openreac.parameters.output.OpenReacResult}.
     */
    public OpenReacParameters addVariableShuntCompensators(List<String> shuntsIds) {
        this.variableShuntCompensators.addAll(shuntsIds);
        return this;
    }

    /**
     * The reactive power produced by every generator in the list will be constant and equal to `targetQ`.
     */
    public OpenReacParameters addConstantQGenerators(List<String> generatorsIds) {
        this.constantQGenerators.addAll(generatorsIds);
        return this;
    }

    /**
     * A list of two windings transformers, which ratio will be considered as variable by the optimizer.
     */
    public OpenReacParameters addVariableTwoWindingsTransformers(List<String> transformerIds) {
        this.variableTwoWindingsTransformers.addAll(transformerIds);
        return this;
    }

    /**
     * Add a parameter to the optimization engine
     */
    public OpenReacParameters addAlgorithmParam(List<OpenReacAlgoParam> algorithmParams) {
        this.algorithmParams.addAll(algorithmParams);
        return this;
    }

    /**
     * Add a parameter to the optimization engine
     */
    public OpenReacParameters addAlgorithmParam(String name, String value) {
        algorithmParams.add(new OpenReacAlgoParamImpl(name, value));
        return this;
    }

    public List<OpenReacAlgoParam> getAlgorithmParams() {
        return algorithmParams;
    }

    /**
     * The definition of the objective function for the optimization.
     */
    public OpenReacOptimisationObjective getObjective() {
        return objective;
    }

    /**
     * The definition of the objective function for the optimization.
     */
    public OpenReacParameters setObjective(OpenReacOptimisationObjective objective) {
        this.objective = Objects.requireNonNull(objective);
        return this;
    }

    public Double getObjectiveDistance() {
        return objectiveDistance;
    }

    /**
     * A 0% objective means the model will target lower voltage limit.
     * <p>
     * A 100% objective means the model will target upper voltage limit.
     * @param objectiveDistance is in %
     */
    public OpenReacParameters setObjectiveDistance(double objectiveDistance) {
        this.objectiveDistance = objectiveDistance;
        return this;
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    public List<Pair<String, VoltageLimitOverride>> getSpecificVoltageLimits() {
        return specificVoltageLimits;
    }

    public List<String> getConstantQGenerators() {
        return constantQGenerators;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
    }

    public List<OpenReacAlgoParam> getAllAlgorithmParams() {
        ArrayList<OpenReacAlgoParam> allAlgoParams = new ArrayList<>(algorithmParams.size() + 2);
        allAlgoParams.addAll(algorithmParams);
        if (objective != null) {
            allAlgoParams.add(objective.toParam());
        }
        if (objectiveDistance != null) {
            allAlgoParams.add(new OpenReacAlgoParamImpl(OBJECTIVE_DISTANCE_KEY, Double.toString(objectiveDistance / 100)));
        }
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

        // Check integrity of voltage overrides
        boolean integrityVoltageLimitOverrides = checkVoltageLimitOverrides(network);
        if (!integrityVoltageLimitOverrides) {
            throw new InvalidParametersException("At least one voltage limit override is inconsistent.");
        }

        // Check integrity of low/high voltage limits, taking into account voltage limit overrides
        boolean integrityVoltageLevelLimits = checkVoltageLevelLimits(network);
        if (!integrityVoltageLevelLimits) {
            throw new InvalidParametersException("At least one voltage level has an undefined or incorrect voltage limit.");
        }

        if (objective.equals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT) && objectiveDistance == null) {
            throw new InvalidParametersException("In using " + OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT +
                    " as objective, a distance in percent between low and high voltage limits is expected.");
        }

    }

    boolean checkVoltageLevelLimits(Network network) {
        boolean integrityVoltageLevelLimits = true;

        for (VoltageLevel vl : network.getVoltageLevels()) {
            double lowLimit = vl.getLowVoltageLimit();
            double highLimit = vl.getHighVoltageLimit();

            // If low voltage limit is undefined...
            if (Double.isNaN(lowLimit)) {

                // find associated override if it exists
                boolean hasOverride = false;
                VoltageLimitOverride associatedOverride = null;
                for (Pair<String, VoltageLimitOverride> pair : getSpecificVoltageLimits()) {
                    if (pair.getFirst().equals(vl.getId()) && pair.getSecond().getSide() == VoltageLimitOverride.OverrideSide.LOW) {
                        hasOverride = true;
                        associatedOverride = pair.getSecond();
                        break;
                    }
                }

                // ... verify if there is an override for the undefined limit
                if (!hasOverride) {
                    LOGGER.warn("Voltage level '" + vl.getId() + "' has no low voltage limit defined. " +
                            "Please add one or use a voltage limit override.");
                    integrityVoltageLevelLimits = false;

                // ... verify override is not relative
                } else if (associatedOverride.isRelative()) {
                    LOGGER.warn("Relative voltage override impossible on undefined low voltage limit for voltage level '"
                            + vl.getId() + "'.");
                    integrityVoltageLevelLimits = false;
                }
            }

            // If high voltage limit is undefined...
            if (Double.isNaN(highLimit)) {

                // find associated override if it exists
                boolean hasOverride = false;
                VoltageLimitOverride associatedOverride = null;
                for (Pair<String, VoltageLimitOverride> pair : getSpecificVoltageLimits()) {
                    if (pair.getFirst().equals(vl.getId()) && pair.getSecond().getSide() == VoltageLimitOverride.OverrideSide.HIGH) {
                        hasOverride = true;
                        associatedOverride = pair.getSecond();
                        break;
                    }
                }

                // ... verify if there is an override for the undefined limit
                if (!hasOverride) {
                    LOGGER.warn("Voltage level '" + vl.getId() + "' has no high voltage limit defined. " +
                            "Please add one or use a voltage limit override.");
                    integrityVoltageLevelLimits = false;

                // ... verify override is not relative
                } else if (associatedOverride.isRelative()) {
                    LOGGER.warn("Relative voltage override impossible on undefined high voltage limit for voltage level '"
                            + vl.getId() + "'.");
                    integrityVoltageLevelLimits = false;
                }
            }
        }

        return integrityVoltageLevelLimits;
    }

    /**
     * @param network the network on which are applied voltage limit overrides.
     * @return true if the integrity of voltage limit overrides is verifies, false otherwise.
     */
    boolean checkVoltageLimitOverrides(Network network) {
        // Check integrity of voltage overrides
        boolean integrityVoltageLimitOverrides = true;
        for (Pair<String, VoltageLimitOverride> pair : getSpecificVoltageLimits()) {
            String voltageLevelId = pair.getFirst();
            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);

            // Check existence of voltage level on which is applied voltage limit override
            if (voltageLevel == null) {
                LOGGER.warn("Voltage level " + voltageLevelId + " not found in the network.");
                integrityVoltageLimitOverrides = false;

            } else {
                VoltageLimitOverride override = pair.getSecond();

                // if the override is relative...
                if (override.isRelative()) {

                    // ... and on low limit ...
                    if (override.getSide() == VoltageLimitOverride.OverrideSide.LOW) {

                        // ... verify low voltage limit is defined
                        if (Double.isNaN(voltageLevel.getLowVoltageLimit())) {
                            LOGGER.warn("Voltage level '" + voltageLevelId + "' has undefined low voltage limit, " +
                                    "relative voltage limit override impossible.");
                            integrityVoltageLimitOverrides = false;
                        }

                        // ... verify low voltage limit override does not lead to negative limit value
                        if (override.getLimitOverride() + voltageLevel.getLowVoltageLimit() < 0) {
                            LOGGER.warn("Voltage level " + voltageLevelId + " low relative override leads to negative low voltage limit.");
                            integrityVoltageLimitOverrides = false;
                        }
                    // ... and on high limit ...
                    } else {

                        // ... verify high voltage limit is defined
                        if (Double.isNaN(voltageLevel.getHighVoltageLimit())) {
                            LOGGER.warn("Voltage level '" + voltageLevelId + "' has undefined high voltage limit, " +
                                    "relative voltage limit override impossible.");
                            integrityVoltageLimitOverrides = false;
                        }
                    }

                }
            }

        }

        return integrityVoltageLimitOverrides;
    }
}
