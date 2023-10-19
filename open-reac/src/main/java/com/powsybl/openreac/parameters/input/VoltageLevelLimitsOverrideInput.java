/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import org.jgrapht.alg.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class VoltageLevelLimitsOverrideInput implements AmplInputFile {

    private static final String FILENAME = "ampl_network_substations_override.txt";

    private final Map<String, Pair<Double, Double>> normalizedVoltageLimitsOverride;

    public VoltageLevelLimitsOverrideInput(List<VoltageLimitOverride> voltageLimitsOverrides, Network network) {
        Objects.requireNonNull(voltageLimitsOverrides);
        Objects.requireNonNull(network);
        this.normalizedVoltageLimitsOverride = new HashMap<>();
        transformToNormalizedVoltage(voltageLimitsOverrides, network);
    }

    /**
     * voltageLimitsOverride contains absolute voltage limits.
     * This function compute limits in pair-unit quantities.
     */
    private void transformToNormalizedVoltage(List<VoltageLimitOverride> voltageLimitsOverrides, Network network) {
        for (VoltageLimitOverride voltageLimitOverride : voltageLimitsOverrides) {
            // get previous voltage limit values
            String voltageLevelId = voltageLimitOverride.getVoltageLevelId();
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            double previousNormalizedLowVoltageLimit = network.getVoltageLevel(voltageLevelId).getLowVoltageLimit() / nominalV;
            double previousNormalizedHighVoltageLimit = network.getVoltageLevel(voltageLevelId).getHighVoltageLimit() / nominalV;

            // get or create voltage limit override of the voltage level
            Pair<Double, Double> newLimits = normalizedVoltageLimitsOverride.containsKey(voltageLevelId) ? normalizedVoltageLimitsOverride.get(voltageLevelId)
                    : new Pair<>(previousNormalizedLowVoltageLimit, previousNormalizedHighVoltageLimit);
            if (voltageLimitOverride.getVoltageLimitType() == VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT) {
                double value = (voltageLimitOverride.isRelative() ? newLimits.getFirst() : 0.0) + voltageLimitOverride.getLimit() / nominalV;
                newLimits.setFirst(value);
            } else if (voltageLimitOverride.getVoltageLimitType() == VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT) {
                double value = (voltageLimitOverride.isRelative() ? newLimits.getSecond() : 0.0) + voltageLimitOverride.getLimit() / nominalV;
                newLimits.setSecond(value);
            } else {
                throw new UnsupportedOperationException("Unsupported voltage limit type: " + voltageLimitOverride.getVoltageLimitType());
            }

            if (newLimits.getFirst() > newLimits.getSecond()) {
                throw new InvalidParametersException("Override on voltage level " + voltageLevelId + " leads to low voltage limit > high voltage limit.");
            }
            normalizedVoltageLimitsOverride.put(voltageLevelId, newLimits);
        }
    }

    @Override
    public String getFileName() {
        return FILENAME;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#num minV (pu) maxV (pu) id");
        dataBuilder.append(System.lineSeparator());

        for (Map.Entry<String, Pair<Double, Double>> entry : normalizedVoltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            Pair<Double, Double> limits = entry.getValue();

            if (!Double.isNaN(limits.getFirst()) || !Double.isNaN(limits.getSecond())) {
                int amplId = stringToIntMapper.getInt(AmplSubset.VOLTAGE_LEVEL, voltageLevelId);
                String[] tokens = {Integer.toString(amplId), Double.toString(limits.getFirst()), Double.toString(limits.getSecond()), "\"" + voltageLevelId + "\""};
                dataBuilder.append(String.join(" ", tokens));
                dataBuilder.append(System.lineSeparator());
            }
        }

        //add new line at the end of the file
        dataBuilder.append(System.lineSeparator());
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
