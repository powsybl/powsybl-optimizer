/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 *
 * This class stores all inputs parameters specific to the OpenReac optimizer, and allow them to be loaded from yaml.
 * <p>
 * TODO read from yaml shunts, transfo, generators, specific voltages ?
 */
public class OpenReacParameters extends AbstractExtendable<OpenReacParameters> {
    private static final String MODULE_CONFIG_NAME = "open-reac";

    private static final String MODIFIABLE_SHUNT_LIST = "modifiable-shunt-list";

    // VERSION 1.0 : No reading from yaml but framework is here
    public static final String VERSION = "1.0";

    /**
     * This map allows to change the bounds of voltage levels.
     * <ul>
     *     <li>
     *         Key: VoltageLevel ID in the network
     *     </li>
     *     <li>
     *         Value: Pair for new voltage level bounds
     *         <ul>
     *            <li>Left: lower bound</li>
     *            <li>Right: upper bound</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private final Map<String, Pair<Double, Double>> specificVoltageDelta;
    /**
     * List of network's shunts ID
     */
    private final List<String> variableShuntCompensators;
    private final List<String> targetQGenerators;
    private final List<String> variableTwoWindingsTransformers;
    private final Map<AlgorithmInput.OpenReacAlgoParam, String> algoParamsMap;

    public static OpenReacParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    private static OpenReacParameters load(PlatformConfig config) {
        OpenReacParameters params = new OpenReacParameters();
        return load(params, config);
    }

    private static OpenReacParameters load(OpenReacParameters params, PlatformConfig platformConfig) {
        platformConfig.getOptionalModuleConfig(MODULE_CONFIG_NAME).ifPresent(config -> {
        });
        return params;
    }

    public OpenReacParameters() {
        this.variableShuntCompensators = new LinkedList<>();
        this.targetQGenerators = new LinkedList<>();
        this.variableTwoWindingsTransformers = new LinkedList<>();
        this.specificVoltageDelta = new HashMap<>();
        this.algoParamsMap = new HashMap<>();
    }

    public OpenReacParameters addVariableShuntCompensators(String... shuntsIds) {
        this.variableShuntCompensators.addAll(Arrays.asList(shuntsIds));
        return this;
    }

    /**
     * Override voltage level bound in the network. This will modify the network when OpenReac is called.
     *
     * @param lowerVoltage factor to the nominal voltage of the voltage ID.
     * @param upperVoltage factor to the nominal voltage of the voltage ID.
     */
    public OpenReacParameters addSpecificVoltageDelta(String voltageLevelId, double lowerVoltage, double upperVoltage) {
        this.specificVoltageDelta.put(voltageLevelId, Pair.of(lowerVoltage, upperVoltage));
        return this;
    }

    /**
     * Fix the reactance of the given generators during the OpenReac solve.
     * The reactance is constant to the reactance stored in the network.
     */
    public OpenReacParameters addTargetQGenerators(String... generatorsIds) {
        this.targetQGenerators.addAll(Arrays.asList(generatorsIds));
        return this;
    }

    /**
     * Tells OpenReac that it can modify the ratio of the given transformers.
     */
    public OpenReacParameters addVariableTwoWindingsTransformers(String... transformerIds) {
        this.variableTwoWindingsTransformers.addAll(Arrays.asList(transformerIds));
        return this;
    }

    public OpenReacParameters addAlgorithmParam(AlgorithmInput.OpenReacAlgoParam param, String value) {
        this.algoParamsMap.put(param, value);
        return this;
    }

    public List<String> getVariableShuntCompensators() {
        return variableShuntCompensators;
    }

    public Map<String, Pair<Double, Double>> getSpecificVoltageDelta() {
        return specificVoltageDelta;
    }

    public List<String> getTargetQGenerators() {
        return targetQGenerators;
    }

    public List<String> getVariableTwoWindingsTransformers() {
        return variableTwoWindingsTransformers;
    }

    public Map<AlgorithmInput.OpenReacAlgoParam, String> getAlgorithmParams() {
        return algoParamsMap;
    }
}
