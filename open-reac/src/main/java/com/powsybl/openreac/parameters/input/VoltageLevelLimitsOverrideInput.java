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
import com.powsybl.openreac.parameters.AmplIOUtils;
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

    public VoltageLevelLimitsOverrideInput(List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride, Network network) {
        Objects.requireNonNull(voltageLimitsOverride);
        Objects.requireNonNull(network);
        this.normalizedVoltageLimitsOverride = new HashMap<>();
        transformToNormalizedVoltage(voltageLimitsOverride, network);
    }

    /**
     * voltageLimitsOverride contains absolute voltage limits.
     * This function compute limits in pair-unit quantities.
     */
    private void transformToNormalizedVoltage(List<Pair<String, VoltageLimitOverride>> voltageLimitsOverride, Network network) {
        for (Pair<String, VoltageLimitOverride> pair : voltageLimitsOverride) {
            String voltageLevelId = pair.getFirst();
            VoltageLimitOverride override = pair.getSecond();

            // get previous voltage limit values
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            double previousNormalizedLowVoltageLimit = network.getVoltageLevel(voltageLevelId).getLowVoltageLimit() / nominalV;
            double previousNormalizedHighVoltageLimit = network.getVoltageLevel(voltageLevelId).getHighVoltageLimit() / nominalV;

            // get or create voltage limit override of the voltage level
            Pair<Double, Double> newLimits = normalizedVoltageLimitsOverride.containsKey(voltageLevelId) ? normalizedVoltageLimitsOverride.get(voltageLevelId)
                    : new Pair<>(previousNormalizedLowVoltageLimit, previousNormalizedHighVoltageLimit);

            // if override is low ...
            if (override.getSide() == VoltageLimitOverride.OverrideSide.LOW) {
                // ... and relative, add relative override
                if (override.isRelative()) {
                    newLimits.setFirst(newLimits.getFirst() + override.getLimitOverride() / nominalV);
                // ... and absolute, replace by absolute override
                } else {
                    newLimits.setFirst(override.getLimitOverride() / nominalV);
                }
            // if override is high ...
            } else {
                // ... and relative, add relative override
                if (override.isRelative()) {
                    newLimits.setSecond(newLimits.getSecond() + override.getLimitOverride() / nominalV);
                // ... and absolute, replace by absolute override
                } else {
                    newLimits.setSecond(override.getLimitOverride() / nominalV);
                }
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
                String[] tokens = {Integer.toString(amplId), Double.toString(limits.getFirst()), Double.toString(limits.getSecond()), AmplIOUtils.addQuotes(voltageLevelId)};
                dataBuilder.append(String.join(" ", tokens));
                dataBuilder.append(System.lineSeparator());
            }
        }

        //add new line at the end of the file !
        dataBuilder.append(System.lineSeparator());
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
