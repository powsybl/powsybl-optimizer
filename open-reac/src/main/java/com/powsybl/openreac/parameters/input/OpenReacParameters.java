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
import com.powsybl.openreac.parameters.input.algo.OpenReacBusesWithReactiveSlackConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class OpenReacParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacParameters.class);

    private static final String OBJECTIVE_DISTANCE_KEY = "ratio_voltage_target";

    private final List<VoltageLimitOverride> specificVoltageLimits = new ArrayList<>();

    private final List<String> variableShuntCompensators = new ArrayList<>();

    private final List<String> constantQGenerators = new ArrayList<>();

    private final List<String> variableTwoWindingsTransformers = new ArrayList<>();

    private List<String> busesWithReactiveSlack = new ArrayList<>();

    private final List<OpenReacAlgoParam> algorithmParams = new ArrayList<>();

    private OpenReacOptimisationObjective objective = OpenReacOptimisationObjective.MIN_GENERATION;

    private Double objectiveDistance;

    private OpenReacBusesWithReactiveSlackConfig busesWithReactiveSlackConfig = OpenReacBusesWithReactiveSlackConfig.NO_GENERATION;

    /**
     * Override some voltage level limits in the network. This will NOT modify the network object.
     * <p>
     * The override is ignored if one or both of the voltage limit are NaN.
     * @param specificVoltageLimits list of voltage limit overrides.
     */
    public OpenReacParameters addSpecificVoltageLimits(List<VoltageLimitOverride> specificVoltageLimits) {
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

    public OpenReacParameters addBusesWithReactiveSlack(List<String> busesIds) {
        this.busesWithReactiveSlack.addAll(busesIds);
        return this;
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

    /**
     * Must be used with {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT}
     * to define the voltage between low and high voltage limits, which OpenReac should converge to.
     * <p>
     * A 0% objective means the model will target lower voltage limit.
     * A 100% objective means the model will target upper voltage limit.
     */
    public Double getObjectiveDistance() {
        return objectiveDistance;
    }

    /**
     * Must be used with {@link OpenReacOptimisationObjective#BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT}
     * <p>
     * A 0% objective means the model will target lower voltage limit.
     * A 100% objective means the model will target upper voltage limit.
     * @param objectiveDistance is in %
     */
    public OpenReacParameters setObjectiveDistance(double objectiveDistance) {
        this.objectiveDistance = objectiveDistance;
        return this;
    }

    public OpenReacBusesWithReactiveSlackConfig getBusesWithReactiveSlackConfig() {
        return busesWithReactiveSlackConfig;
    }

    public OpenReacParameters setBusesWithReactiveSlackConfig(OpenReacBusesWithReactiveSlackConfig busesWithReactiveSlackConfig) {
        this.busesWithReactiveSlackConfig = Objects.requireNonNull(busesWithReactiveSlackConfig);
        return this;
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    public List<VoltageLimitOverride> getSpecificVoltageLimits() {
        return specificVoltageLimits;
    }

    public List<String> getConstantQGenerators() {
        return constantQGenerators;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
    }

    public List<String> getConfiguredBusesWithReactiveSlacks() {
        return busesWithReactiveSlack;
    }

    public List<OpenReacAlgoParam> getAllAlgorithmParams() {
        ArrayList<OpenReacAlgoParam> allAlgoParams = new ArrayList<>(algorithmParams.size() + 3);
        allAlgoParams.addAll(algorithmParams);
        if (objective != null) {
            allAlgoParams.add(objective.toParam());
        }
        if (objectiveDistance != null) {
            allAlgoParams.add(new OpenReacAlgoParamImpl(OBJECTIVE_DISTANCE_KEY, Double.toString(objectiveDistance / 100)));
        }
        if (busesWithReactiveSlackConfig != null) {
            allAlgoParams.add(busesWithReactiveSlackConfig.toParam());
        }
        return allAlgoParams;
    }

    /**
     * Do some checks on the parameters given, such as provided IDs must correspond to the given network element
     *
     * @param network Network on which ID are going to be infered
     * @throws InvalidParametersException if the parameters contain some incoherences.
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
                throw new InvalidParametersException("Two windings transformer " + transformerId + " not found in the network.");
            }
        }
        for (String busId : getConfiguredBusesWithReactiveSlacks()) {
            if (network.getBusView().getBus(busId) == null) {
                throw new InvalidParametersException("Bus " + busId + " not found in the network.");
            }
        }

        // Check integrity of voltage overrides
        boolean integrityVoltageLimitOverrides = checkVoltageLimitOverrides(network);
        if (!integrityVoltageLimitOverrides) {
            throw new InvalidParametersException("At least one voltage limit override is inconsistent.");
        }

        // Check integrity of low/high voltage limits, taking into account voltage limit overrides
        boolean integrityVoltageLevelLimits = checkLowVoltageLevelLimits(network);
        integrityVoltageLevelLimits &= checkHighVoltageLevelLimits(network);
        if (!integrityVoltageLevelLimits) {
            throw new InvalidParametersException("At least one voltage level has an undefined or incorrect voltage limit.");
        }

        if (objective.equals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT) && objectiveDistance == null) {
            throw new InvalidParametersException("In using " + OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT +
                    " as objective, a distance in percent between low and high voltage limits is expected.");
        }

    }

    /**
     * @param network the network on which voltage levels are verified.
     * @return true if the low voltage level limits are correct taking into account low voltage limit overrides,
     * false otherwise.
     */
    boolean checkLowVoltageLevelLimits(Network network) {
        boolean integrityVoltageLevelLimits = true;

        for (VoltageLevel vl : network.getVoltageLevels()) {
            double lowLimit = vl.getLowVoltageLimit();

            if (Double.isNaN(lowLimit)) {
                List<VoltageLimitOverride> overrides = getSpecificVoltageLimits(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT);
                if (overrides.size() != 1) {
                    LOGGER.warn("Voltage level {} has no low voltage limit defined. Please add one or use a voltage limit override.", vl.getId());
                    integrityVoltageLevelLimits = false;
                } else if (overrides.get(0).isRelative()) { // we have one and just one
                    LOGGER.warn("Relative voltage override impossible on undefined low voltage limit for voltage level {}.", vl.getId());
                    integrityVoltageLevelLimits = false;
                }
            } else if (lowLimit < 0.5 * vl.getNominalV()) {
                LOGGER.info("Voltage level {} has maybe an inconsistent low voltage limit ({} kV)", vl.getId(), lowLimit);
            }
        }
        return integrityVoltageLevelLimits;
    }

    /**
     * @param network the network on which voltage levels are verified.
     * @return true if the high voltage level limits are correct taking into account high voltage limit overrides,
     * false otherwise.
     */
    boolean checkHighVoltageLevelLimits(Network network) {
        boolean integrityVoltageLevelLimits = true;

        for (VoltageLevel vl : network.getVoltageLevels()) {
            double highLimit = vl.getHighVoltageLimit();

            if (Double.isNaN(highLimit)) {
                List<VoltageLimitOverride> overrides = getSpecificVoltageLimits(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT);
                if (overrides.size() != 1) {
                    LOGGER.warn("Voltage level {} has no high voltage limit defined. Please add one or use a voltage limit override.", vl.getId());
                    integrityVoltageLevelLimits = false;
                } else if (overrides.get(0).isRelative()) {
                    LOGGER.warn("Relative voltage override impossible on undefined high voltage limit for voltage level {}.", vl.getId());
                    integrityVoltageLevelLimits = false;
                }
            } else if (highLimit > 1.5 * vl.getNominalV()) {
                LOGGER.info("Voltage level {} has maybe an inconsistent high voltage limit ({} kV)", vl.getId(), highLimit);
            }
        }
        return integrityVoltageLevelLimits;
    }

    /**
     * @param network the network on which are applied voltage limit overrides.
     * @return true if the integrity of voltage limit overrides is verified, false otherwise.
     */
    boolean checkVoltageLimitOverrides(Network network) {
        // Check integrity of voltage overrides
        boolean integrityVoltageLimitOverrides = true;
        for (VoltageLimitOverride voltageLimitOverride : getSpecificVoltageLimits()) {
            String voltageLevelId = voltageLimitOverride.getVoltageLevelId();
            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);

            // Check existence of voltage level on which is applied voltage limit override
            if (voltageLevel == null) {
                LOGGER.warn("Voltage level {} not found in the network.", voltageLevelId);
                integrityVoltageLimitOverrides = false;
                continue;
            }

            if (voltageLimitOverride.isRelative()) {
                double value = voltageLimitOverride.getVoltageLimitType() == VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT ?
                        voltageLevel.getLowVoltageLimit() : voltageLevel.getHighVoltageLimit();
                if (Double.isNaN(value)) {
                    LOGGER.warn("Voltage level {} has undefined {}, relative voltage limit override is impossible.",
                            voltageLevelId, voltageLimitOverride.getVoltageLimitType());
                    integrityVoltageLimitOverrides = false;
                }
                // verify voltage limit override does not lead to negative limit value
                if (value + voltageLimitOverride.getLimit() < 0) {
                    LOGGER.warn("Voltage level {} relative override leads to a negative {}.",
                            voltageLevelId, voltageLimitOverride.getVoltageLimitType());
                    integrityVoltageLimitOverrides = false;
                }
            }
        }
        return integrityVoltageLimitOverrides;
    }

    private List<VoltageLimitOverride> getSpecificVoltageLimits(String voltageLevelId, VoltageLimitOverride.VoltageLimitType type) {
        return specificVoltageLimits.stream()
                .filter(limit -> limit.getVoltageLevelId().equals(voltageLevelId) && limit.getVoltageLimitType() == type).collect(Collectors.toList());
    }
}
