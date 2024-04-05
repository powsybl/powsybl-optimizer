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
import com.powsybl.openreac.parameters.input.algo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class stores all inputs parameters specific to the OpenReac optimizer.
 *
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 * @author Pierre Arvy <pierre.arvy at artelys.com>
 */
public class OpenReacParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenReacParameters.class);

    private final List<VoltageLimitOverride> specificVoltageLimits = new ArrayList<>();

    private final List<String> variableShuntCompensators = new ArrayList<>();

    private final List<String> constantQGenerators = new ArrayList<>();

    private final List<String> variableTwoWindingsTransformers = new ArrayList<>();

    private final List<String> configuredReactiveSlackBuses = new ArrayList<>();

    // Algo parameters

    private OpenReacOptimisationObjective objective = OpenReacOptimisationObjective.MIN_GENERATION;

    private OpenReacAmplLogLevel logLevelAmpl = OpenReacAmplLogLevel.INFO;

    private OpenReacSolverLogLevel logLevelSolver = OpenReacSolverLogLevel.EVERYTHING;

    private static final String OBJECTIVE_DISTANCE_KEY = "ratio_voltage_target";

    private Double objectiveDistance; // between 0 and 100

    private static final String MIN_PLAUSIBLE_LOW_VOLTAGE_LIMIT_KEY = "min_plausible_low_voltage_limit";

    private double minPlausibleLowVoltageLimit = 0.5; // in pu

    private static final String MAX_PLAUSIBLE_HIGH_VOLTAGE_LIMIT_KEY = "max_plausible_high_voltage_limit";

    private double maxPlausibleHighVoltageLimit = 1.5; // in pu

    private ReactiveSlackBusesMode reactiveSlackBusesMode = ReactiveSlackBusesMode.NO_GENERATION;

    private static final String DEFAULT_VARIABLE_SCALING_FACTOR = "default_variable_scaling_factor";

    private double defaultVariableScalingFactor = 1;

    private static final String DEFAULT_CONSTRAINT_SCALING_FACTOR = "default_constraint_scaling_factor";

    private double defaultConstraintScalingFactor = 1;

    private static final String REACTIVE_SLACK_VARIABLE_SCALING_FACTOR = "reactive_slack_variable_scaling_factor";

    private double reactiveSlackVariableScalingFactor = 1e-1;

    private static final String TWO_WINDING_TRANSFORMER_RATIO_VARIABLE_SCALING_FACTOR = "transformer_ratio_variable_scaling_factor";

    private double twoWindingTransformerRatioVariableScalingFactor = 1e-3;

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

    /**
     * A list of buses, to which reactive slacks variable will be attached by the optimizer.
     */
    public OpenReacParameters addConfiguredReactiveSlackBuses(List<String> busesIds) {
        this.configuredReactiveSlackBuses.addAll(busesIds);
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
        if (Double.isNaN(objectiveDistance) || objectiveDistance > 100 || objectiveDistance < 0) {
            throw new IllegalArgumentException("Objective distance must be defined and >= 0 and <= 100 to be consistent");
        }
        this.objectiveDistance = objectiveDistance;
        return this;
    }

    /**
     * @return log level of ampl printings.
     */
    public OpenReacAmplLogLevel getLogLevelAmpl() {
        return this.logLevelAmpl;
    }

    /**
     * @param logLevelAmpl the log level of ampl printings.
     */
    public OpenReacParameters setLogLevelAmpl(OpenReacAmplLogLevel logLevelAmpl) {
        this.logLevelAmpl = Objects.requireNonNull(logLevelAmpl);
        return this;
    }

    /**
     * @return log level of solver printings.
     */
    public OpenReacSolverLogLevel getLogLevelSolver() {
        return this.logLevelSolver;
    }

    /**
     * @param logLevelSolver the log level of solver printings.
     */
    public OpenReacParameters setLogLevelSolver(OpenReacSolverLogLevel logLevelSolver) {
        this.logLevelSolver = Objects.requireNonNull(logLevelSolver);
        return this;
    }

    /**
     * @return the minimal plausible value for low voltage limits in p.u.
     */
    public double getMinPlausibleLowVoltageLimit() {
        return minPlausibleLowVoltageLimit;
    }

    public OpenReacParameters setMinPlausibleLowVoltageLimit(double minPlausibleLowVoltageLimit) {
        if (minPlausibleLowVoltageLimit < 0 || Double.isNaN(minPlausibleLowVoltageLimit)) {
            throw new IllegalArgumentException("Min plausible low voltage limit must be >= 0 and defined to be consistent.");
        }
        this.minPlausibleLowVoltageLimit = minPlausibleLowVoltageLimit;
        return this;
    }

    /**
     * @return the maximal plausible value for high voltage limits in p.u.
     */
    public double getMaxPlausibleHighVoltageLimit() {
        return maxPlausibleHighVoltageLimit;
    }

    public OpenReacParameters setMaxPlausibleHighVoltageLimit(double maxPlausibleHighVoltageLimit) {
        if (maxPlausibleHighVoltageLimit <= 0 || Double.isNaN(maxPlausibleHighVoltageLimit)) {
            throw new IllegalArgumentException("Max plausible high voltage limit must be > 0 and defined to be consistent.");
        }
        this.maxPlausibleHighVoltageLimit = maxPlausibleHighVoltageLimit;
        return this;
    }

    /**
     * @return the mode used to select which buses will have reactive slack variables attached in the optimization.
     * If mode is CONFIGURED, the buses in configuredReactiveSlackBuses are used.
     */
    public ReactiveSlackBusesMode getReactiveSlackBusesMode() {
        return reactiveSlackBusesMode;
    }

    public OpenReacParameters setReactiveSlackBusesMode(ReactiveSlackBusesMode reactiveSlackBusesMode) {
        this.reactiveSlackBusesMode = Objects.requireNonNull(reactiveSlackBusesMode);
        return this;
    }

    /**
     * @return the default scaling value of all the variables in ACOPF solving.
     */
    public double getDefaultVariableScalingFactor() {
        return defaultVariableScalingFactor;
    }

    public OpenReacParameters setDefaultVariableScalingFactor(double defaultVariableScalingFactor) {
        if (defaultVariableScalingFactor <= 0 || Double.isNaN(defaultVariableScalingFactor)) {
            throw new IllegalArgumentException("Default scaling factor for variables must be > 0 and defined to be consistent.");
        }
        this.defaultVariableScalingFactor = defaultVariableScalingFactor;
        return this;
    }

    /**
     * @return the default scaling value of all the constraints in ACOPF solving.
     */
    public double getDefaultConstraintScalingFactor() {
        return defaultConstraintScalingFactor;
    }

    public OpenReacParameters setDefaultConstraintScalingFactor(double defaultConstraintScalingFactor) {
        if (defaultConstraintScalingFactor < 0 || Double.isNaN(defaultConstraintScalingFactor)) {
            throw new IllegalArgumentException("Default scaling factor for constraints must be >= 0 and defined to be consistent.");
        }
        this.defaultConstraintScalingFactor = defaultConstraintScalingFactor;
        return this;
    }

    /**
     * @return the scaling value of reactive slack variables in ACOPF solving.
     */
    public double getReactiveSlackVariableScalingFactor() {
        return reactiveSlackVariableScalingFactor;
    }

    public OpenReacParameters setReactiveSlackVariableScalingFactor(double reactiveSlackVariableScalingFactor) {
        if (reactiveSlackVariableScalingFactor <= 0 || Double.isNaN(reactiveSlackVariableScalingFactor)) {
            throw new IllegalArgumentException("Scaling factor for reactive slack variables must be > 0 and defined to be consistent.");
        }
        this.reactiveSlackVariableScalingFactor = reactiveSlackVariableScalingFactor;
        return this;
    }

    /**
     * @return the scaling value of transformer ratios in ACOPF solving.
     */
    public double getTwoWindingTransformerRatioVariableScalingFactor() {
        return twoWindingTransformerRatioVariableScalingFactor;
    }

    public OpenReacParameters setTwoWindingTransformerRatioVariableScalingFactor(double twoWindingTransformerRatioVariableScalingFactor) {
        if (twoWindingTransformerRatioVariableScalingFactor <= 0 || Double.isNaN(twoWindingTransformerRatioVariableScalingFactor)) {
            throw new IllegalArgumentException("Scaling factor for transformer ratio variables must be > 0 and defined to be consistent.");
        }
        this.twoWindingTransformerRatioVariableScalingFactor = twoWindingTransformerRatioVariableScalingFactor;
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

    public List<String> getConfiguredReactiveSlackBuses() {
        return configuredReactiveSlackBuses;
    }

    public List<OpenReacAlgoParam> getAllAlgorithmParams() {
        ArrayList<OpenReacAlgoParam> allAlgoParams = new ArrayList<>();
        allAlgoParams.add(objective.toParam());
        if (objectiveDistance != null) {
            allAlgoParams.add(new OpenReacAlgoParamImpl(OBJECTIVE_DISTANCE_KEY, Double.toString(objectiveDistance / 100)));
        }
        allAlgoParams.add(this.logLevelAmpl.toParam());
        allAlgoParams.add(this.logLevelSolver.toParam());
        allAlgoParams.add(new OpenReacAlgoParamImpl(MIN_PLAUSIBLE_LOW_VOLTAGE_LIMIT_KEY, Double.toString(minPlausibleLowVoltageLimit)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(MAX_PLAUSIBLE_HIGH_VOLTAGE_LIMIT_KEY, Double.toString(maxPlausibleHighVoltageLimit)));
        allAlgoParams.add(reactiveSlackBusesMode.toParam());
        allAlgoParams.add(new OpenReacAlgoParamImpl(DEFAULT_VARIABLE_SCALING_FACTOR, Double.toString(defaultVariableScalingFactor)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(DEFAULT_CONSTRAINT_SCALING_FACTOR, Double.toString(defaultConstraintScalingFactor)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(REACTIVE_SLACK_VARIABLE_SCALING_FACTOR, Double.toString(reactiveSlackVariableScalingFactor)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(TWO_WINDING_TRANSFORMER_RATIO_VARIABLE_SCALING_FACTOR, Double.toString(twoWindingTransformerRatioVariableScalingFactor)));
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
        for (String busId : getConfiguredReactiveSlackBuses()) {
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

        boolean integrityAlgorithmParameters = checkAlgorithmParametersIntegrity();
        if (!integrityAlgorithmParameters) {
            throw new InvalidParametersException("At least one algorithm parameter is inconsistent.");
        }
    }

    /**
     * @return true if the algorithm parameters are consistent, false otherwise.
     */
    public boolean checkAlgorithmParametersIntegrity() {
        boolean integrityAlgorithmParameters = true;

        // Check integrity of objective function
        if (objective.equals(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT) && objectiveDistance == null) {
            LOGGER.warn("In using {} as objective, a distance in percent between low and high voltage limits is expected.", OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT);
            integrityAlgorithmParameters = false;
        }

        // Check integrity of min/max plausible voltage limits
        if (minPlausibleLowVoltageLimit > maxPlausibleHighVoltageLimit) {
            LOGGER.warn("Min plausible low voltage limit must be lower than max plausible high voltage limit.");
            integrityAlgorithmParameters = false;
        }

        return integrityAlgorithmParameters;
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
