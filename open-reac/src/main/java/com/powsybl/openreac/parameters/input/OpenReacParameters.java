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

    private final List<VoltageLimitOverride> specificVoltageLimits = new ArrayList<>();

    private final List<String> variableShuntCompensators = new ArrayList<>();

    private final List<String> constantQGenerators = new ArrayList<>();

    private final List<String> variableTwoWindingsTransformers = new ArrayList<>();

    // Algo parameters

    private OpenReacOptimisationObjective objective = OpenReacOptimisationObjective.MIN_GENERATION;

    private static final String OBJECTIVE_DISTANCE_KEY = "ratio_voltage_target";

    private Double objectiveDistance = 0.5;

    private static final String MIN_PLAUSIBLE_LOW_VOLTAGE_LIMIT_KEY = "min_plausible_low_voltage_limit";

    private Double minPlausibleLowVoltageLimit = 0.5; // in pu

    private static final String MAX_PLAUSIBLE_HIGH_VOLTAGE_LIMIT_KEY = "max_plausible_high_voltage_limit";

    private Double maxPlausibleHighVoltageLimit = 1.5; // in pu

    private static final String ALPHA_COEFFICIENT_KEY = "coeff_alpha";

    private Double alphaCoefficient = 1.; // in [0;1]

    private static final String ZERO_POWER_THRESHOLD_KEY = "Pnull";

    private Double zeroPowerThreshold = 0.01; // in MW, for detecting zero value for power

    private static final String ZERO_IMPEDANCE_THRESHOLD_KEY = "Znull";

    private Double zeroImpedanceThreshold = 1e-4; // in p.u., for detecting null impedance branches

    private static final String NOMINAL_THRESHOLD_IGNORED_BUS_KEY = "epsilon_nominal_voltage";

    private Double nominalThresholdIgnoredBuses = 1.; // in kV, to ignore buses with Vnom lower than this value

    private static final String NOMINAL_THRESHOLD_IGNORED_VOLTAGE_BOUNDS_KEY = "ignore_voltage_bounds";

    private Double nominalThresholdIgnoredVoltageBounds = 0.; // in kV, to ignore voltage bounds of buses with Vnom lower than this value

    private static final String PQ_MAX_KEY = "PQmax";

    private Double PQMax = 9000.; // MW

    private static final String DEFAULT_PMAX_KEY = "defaultPmax";

    private Double defaultPMax = 1000.; // MW

    private static final String DEFAULT_PMIN_KEY = "defaultPmin";

    private Double defaultPMin = 0.; // MW

    private static final String DEFAULT_QMAX_PMAX_RATIO_KEY = "defaultQmaxPmaxRatio";

    private Double defaultQmaxPmaxRatio = 0.3;

    private static final String DEFAULT_MINIMAL_Q_P_RANGE_KEY = "minimalQPrange";

    private Double defaultMinimalQPRange = 1.;

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

    public List<VoltageLimitOverride> getSpecificVoltageLimits() {
        return specificVoltageLimits;
    }

    /**
     * A list of shunt compensators, which susceptance will be considered as variable by the optimizer.
     * The optimizer computes a continuous value that is rounded when results are stored in {@link com.powsybl.openreac.parameters.output.OpenReacResult}.
     */
    public OpenReacParameters addVariableShuntCompensators(List<String> shuntsIds) {
        this.variableShuntCompensators.addAll(shuntsIds);
        return this;
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    /**
     * The reactive power produced by every generator in the list will be constant and equal to `targetQ`.
     */
    public OpenReacParameters addConstantQGenerators(List<String> generatorsIds) {
        this.constantQGenerators.addAll(generatorsIds);
        return this;
    }

    public List<String> getConstantQGenerators() {
        return constantQGenerators;
    }

    /**
     * A list of two windings transformers, which ratio will be considered as variable by the optimizer.
     */
    public OpenReacParameters addVariableTwoWindingsTransformers(List<String> transformerIds) {
        this.variableTwoWindingsTransformers.addAll(transformerIds);
        return this;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
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
        if (Double.isNaN(objectiveDistance) || objectiveDistance > 1 || objectiveDistance < 0) {
            throw new IllegalArgumentException("Objective distance must be defined and >= 0 and <= 1 to be consistent");
        }
        this.objectiveDistance = objectiveDistance;
        return this;
    }

    /**
     * @return the minimal plausible value for low voltage limits in p.u.
     */
    public Double getMinPlausibleLowVoltageLimit() {
        return minPlausibleLowVoltageLimit;
    }

    public OpenReacParameters setMinPlausibleLowVoltageLimit(double minPlausibleLowVoltageLimit) {
        if (Double.isNaN(minPlausibleLowVoltageLimit) || minPlausibleLowVoltageLimit < 0) {
            throw new InvalidParametersException("Min plausible low voltage limit must be defined and >= 0 to be consistent.");
        }
        this.minPlausibleLowVoltageLimit = minPlausibleLowVoltageLimit;
        return this;
    }

    /**
     * @return the maximal plausible value for high voltage limits in p.u.
     */
    public Double getMaxPlausibleHighVoltageLimit() {
        return maxPlausibleHighVoltageLimit;
    }

    public OpenReacParameters setMaxPlausibleHighVoltageLimit(double maxPlausibleHighVoltageLimit) {
        if (Double.isNaN(maxPlausibleHighVoltageLimit) || maxPlausibleHighVoltageLimit <= 0) {
            throw new InvalidParametersException("Max plausible high voltage limit must be defined and > 0 to be consistent.");
        }
        this.maxPlausibleHighVoltageLimit = maxPlausibleHighVoltageLimit;
        return this;
    }

    public Double getAlphaCoefficient() {
        return alphaCoefficient;
    }

    public OpenReacParameters setAlphaCoefficient(Double alphaCoefficient) {
        if (Double.isNaN(alphaCoefficient) || alphaCoefficient < 0 || alphaCoefficient > 1) {
            throw new InvalidParametersException("Coefficient alpha parameter must be defined and between 0 and 1 to be consistent.");
        }
        this.alphaCoefficient = alphaCoefficient;
        return this;
    }

    public Double getZeroPowerThreshold() {
        return zeroPowerThreshold;
    }

    public OpenReacParameters setZeroPowerThreshold(Double zeroPowerThreshold) {
        if (Double.isNaN(zeroPowerThreshold) || zeroPowerThreshold < 0) {
            throw new InvalidParametersException("Zero power threshold must be defined and >= 0 to be consistent.");
        }
        this.zeroPowerThreshold = zeroPowerThreshold;
        return this;
    }

    public Double getZeroImpedanceThreshold() {
        return zeroImpedanceThreshold;
    }

    public OpenReacParameters setZeroImpedanceThreshold(Double zeroImpedanceThreshold) {
        if (Double.isNaN(zeroImpedanceThreshold) || zeroImpedanceThreshold < 0) {
            throw new InvalidParametersException("Zero impedance threshold must be defined and >= 0 to be consistent.");
        }
        this.zeroImpedanceThreshold = zeroImpedanceThreshold;
        return this;
    }

    public Double getNominalThresholdIgnoredBuses() {
        return nominalThresholdIgnoredBuses;
    }

    public OpenReacParameters setNominalThresholdIgnoredBuses(Double nominalThresholdIgnoredBuses) {
        if (Double.isNaN(nominalThresholdIgnoredBuses) || nominalThresholdIgnoredBuses < 0) {
            throw new InvalidParametersException("Nominal threshold for ignored buses must be defined and >= 0 to be consistent.");
        }
        this.nominalThresholdIgnoredBuses = nominalThresholdIgnoredBuses;
        return this;
    }

    public Double getNominalThresholdIgnoredVoltageBounds() {
        return nominalThresholdIgnoredVoltageBounds;
    }

    public OpenReacParameters setNominalThresholdIgnoredVoltageBounds(Double nominalThresholdIgnoredVoltageBounds) {
        if (Double.isNaN(nominalThresholdIgnoredVoltageBounds) || nominalThresholdIgnoredVoltageBounds < 0) {
            throw new InvalidParametersException("Nominal threshold for ignored voltage bounds must be defined and >= 0 to be consistent");
        }
        this.nominalThresholdIgnoredVoltageBounds = nominalThresholdIgnoredVoltageBounds;
        return this;
    }

    public List<OpenReacAlgoParam> getAllAlgorithmParams() {
        ArrayList<OpenReacAlgoParam> allAlgoParams = new ArrayList<>();
        allAlgoParams.add(objective.toParam());
        allAlgoParams.add(new OpenReacAlgoParamImpl(OBJECTIVE_DISTANCE_KEY, Double.toString(objectiveDistance)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(MIN_PLAUSIBLE_LOW_VOLTAGE_LIMIT_KEY, Double.toString(minPlausibleLowVoltageLimit)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(MAX_PLAUSIBLE_HIGH_VOLTAGE_LIMIT_KEY, Double.toString(maxPlausibleHighVoltageLimit)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(ALPHA_COEFFICIENT_KEY, Double.toString(alphaCoefficient)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(ZERO_POWER_THRESHOLD_KEY, Double.toString(zeroPowerThreshold)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(ZERO_IMPEDANCE_THRESHOLD_KEY, Double.toString(zeroImpedanceThreshold)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(NOMINAL_THRESHOLD_IGNORED_BUS_KEY, Double.toString(nominalThresholdIgnoredBuses)));
        allAlgoParams.add(new OpenReacAlgoParamImpl(NOMINAL_THRESHOLD_IGNORED_VOLTAGE_BOUNDS_KEY, Double.toString(nominalThresholdIgnoredVoltageBounds)));
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

        // Check integrity of min/max plausible voltage limits
        if (minPlausibleLowVoltageLimit != null && maxPlausibleHighVoltageLimit != null
                && minPlausibleLowVoltageLimit > maxPlausibleHighVoltageLimit) {
            LOGGER.warn("Min plausible low voltage limit must be lower than max plausible high voltage limit.");
            integrityAlgorithmParameters = false;
        }

        if (nominalThresholdIgnoredBuses > nominalThresholdIgnoredVoltageBounds) {
            LOGGER.warn("Some buses with ignored voltage bounds will be ignored in calculations.");
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
