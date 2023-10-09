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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class VoltageLevelLimitsOverrideInput implements AmplInputFile {

    private static final String FILENAME = "ampl_network_substations_override.txt";
    private final Map<String, VoltageLimitOverride> normalizedVoltageLimitsOverride;

    public VoltageLevelLimitsOverrideInput(Map<String, VoltageLimitOverride> voltageLimitsOverride, Network network) {
        Objects.requireNonNull(voltageLimitsOverride);
        Objects.requireNonNull(network);
        this.normalizedVoltageLimitsOverride = new HashMap<>();
        transformToNormalizedVoltage(voltageLimitsOverride, network);
    }

    /**
     * voltageLimitsOverride contains absolute voltage limits.
     * This function compute limits in pair-unit quantities.
     */
    private void transformToNormalizedVoltage(Map<String, VoltageLimitOverride> voltageLimitsOverride, Network network) {
        for (Map.Entry<String, VoltageLimitOverride> entry : voltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            VoltageLimitOverride limits = entry.getValue();

            double previousLowVoltageLimit = network.getVoltageLevel(voltageLevelId).getLowVoltageLimit();
            double previousHighVoltageLimit = network.getVoltageLevel(voltageLevelId).getHighVoltageLimit();
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();

            VoltageLimitOverrideBuilder builder = new VoltageLimitOverrideBuilder()
                    .withLowLimitKind(limits.getLowLimitKind())
                    .withHighLimitKind(limits.getHighLimitKind());

            // compute low normalized override
            if (limits.getLowLimitKind() == VoltageLimitOverride.OverrideKind.ABSOLUTE) {
                builder.withLowLimitOverride(limits.getLowLimitOverride() / nominalV);
            } else if (limits.getLowLimitKind() == VoltageLimitOverride.OverrideKind.RELATIVE) {
                // if override is relative, must check that previous limit of voltage level is defined
                if (!Double.isNaN(previousLowVoltageLimit)) {
                    builder.withLowLimitOverride((previousLowVoltageLimit + limits.getLowLimitOverride()) / nominalV);
                } else {
                    throw new IllegalArgumentException("Relative override must be done on valid low voltage limit");
                }
            // if no kind given, then no low voltage limit override
            } else {
                builder.withLowLimitOverride(previousLowVoltageLimit / nominalV);
            }

            // compute high normalized override
            if (limits.getHighLimitKind() == VoltageLimitOverride.OverrideKind.ABSOLUTE) {
                builder.withHighLimitOverride(limits.getHighLimitOverride() / nominalV);
            } else if (limits.getHighLimitKind() == VoltageLimitOverride.OverrideKind.RELATIVE) {
                // if override is relative, must check that previous limit of voltage level is defined
                if (!Double.isNaN(previousHighVoltageLimit)) {
                    builder.withHighLimitOverride((previousHighVoltageLimit + limits.getHighLimitOverride()) / nominalV);
                } else {
                    throw new IllegalArgumentException("Relative override must be done on valid high voltage limit");
                }
            // if no kind given, then no high voltage limit override
            } else {
                builder.withHighLimitOverride(previousHighVoltageLimit / nominalV);
            }

            normalizedVoltageLimitsOverride.put(voltageLevelId, builder.build());
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

        for (Map.Entry<String, VoltageLimitOverride> entry : normalizedVoltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            VoltageLimitOverride limits = entry.getValue();

            if (!Double.isNaN(limits.getLowLimitOverride()) && !Double.isNaN(limits.getHighLimitOverride())) {
                int amplId = stringToIntMapper.getInt(AmplSubset.VOLTAGE_LEVEL, voltageLevelId);
                double newLowVoltageLimit = limits.getLowLimitOverride();
                double newHighVoltageLimit = limits.getHighLimitOverride();

                String[] tokens = {Integer.toString(amplId), Double.toString(newLowVoltageLimit), Double.toString(newHighVoltageLimit), AmplIOUtils.addQuotes(voltageLevelId)};
                dataBuilder.append(String.join(" ", tokens));
                dataBuilder.append(System.lineSeparator());
            }
        }

        //add new line at the end of the file !
        dataBuilder.append(System.lineSeparator());
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
