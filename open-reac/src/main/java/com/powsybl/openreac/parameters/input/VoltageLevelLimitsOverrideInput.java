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
     * This function compute limits in per-unit quantities.
     */
    private void transformToNormalizedVoltage(Map<String, VoltageLimitOverride> voltageLimitsOverride, Network network) {
        for (Map.Entry<String, VoltageLimitOverride> entry : voltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            VoltageLimitOverride limits = entry.getValue();
            double previousLowVoltageLimit = network.getVoltageLevel(voltageLevelId).getLowVoltageLimit();
            double previousHighVoltageLimit = network.getVoltageLevel(voltageLevelId).getHighVoltageLimit();
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            normalizedVoltageLimitsOverride.put(voltageLevelId, new VoltageLimitOverride((previousLowVoltageLimit + limits.getDeltaLowVoltageLimit()) / nominalV,
                    (previousHighVoltageLimit + limits.getDeltaHighVoltageLimit()) / nominalV));
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
            int amplId = stringToIntMapper.getInt(AmplSubset.VOLTAGE_LEVEL, voltageLevelId);
            String[] tokens = {Integer.toString(amplId), Double.toString(limits.getDeltaLowVoltageLimit()),
                    Double.toString(limits.getDeltaHighVoltageLimit()), AmplIOUtils.addQuotes(voltageLevelId)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append(System.lineSeparator());
        }
        //add new line at the end of the file !
        dataBuilder.append(System.lineSeparator());
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
