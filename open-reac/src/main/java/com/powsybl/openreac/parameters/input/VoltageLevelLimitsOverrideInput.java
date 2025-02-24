/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.AmplIOUtils;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.openreac.Reports.reportVoltageLevelsWithLimitsOutOfNominalVRange;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public class VoltageLevelLimitsOverrideInput implements AmplInputFile {

    private static final String FILENAME = "ampl_network_substations_override.txt";

    private final Map<String, Pair<Double, Double>> normalizedVoltageLimitsOverride;

    private static final String OVERRIDE_ON_VOLTAGE_LEVEL = "Override on voltage level ";
    private static final double VOLTAGE_LIMIT_LOW_THRESHOLD = 0.85;
    private static final double VOLTAGE_LIMIT_HIGH_THRESHOLD = 1.15;
    private static final double VOLTAGE_LIMIT_TOLERANCE = 5;

    public VoltageLevelLimitsOverrideInput(List<VoltageLimitOverride> voltageLimitsOverrides, Network network, ReportNode reportNode) {
        Objects.requireNonNull(voltageLimitsOverrides);
        Objects.requireNonNull(network);
        this.normalizedVoltageLimitsOverride = new HashMap<>();
        transformToNormalizedVoltage(voltageLimitsOverrides, network, reportNode);
    }

    private void checkLimitsInNominalVoltageRange(Network network, ReportNode reportNode) {
        // check that the limits are in the nominal voltage range [0.85 * nominal_V - 5, 1.15 * nominal_V + 5]
        Map<String, VoltageLevelLimitInfo> voltageLevelsWithLimitsOutOfNominalVRange = new HashMap<>();
        for (Map.Entry<String, Pair<Double, Double>> entry : normalizedVoltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
            double nominalV = voltageLevel.getNominalV();
            double lowLimit = entry.getValue().getFirst() * nominalV;
            double highLimit = entry.getValue().getSecond() * nominalV;

            if (lowLimit < VOLTAGE_LIMIT_LOW_THRESHOLD * nominalV - VOLTAGE_LIMIT_TOLERANCE ||
                lowLimit > VOLTAGE_LIMIT_HIGH_THRESHOLD * nominalV + VOLTAGE_LIMIT_TOLERANCE ||
                highLimit < VOLTAGE_LIMIT_LOW_THRESHOLD * nominalV - VOLTAGE_LIMIT_TOLERANCE ||
                highLimit > VOLTAGE_LIMIT_HIGH_THRESHOLD * nominalV + VOLTAGE_LIMIT_TOLERANCE) {
                voltageLevelsWithLimitsOutOfNominalVRange.put(voltageLevelId, new VoltageLevelLimitInfo(voltageLevelId, lowLimit, highLimit, nominalV));
            }
        }

        reportVoltageLevelsWithLimitsOutOfNominalVRange(reportNode, voltageLevelsWithLimitsOutOfNominalVRange);
    }

    /**
     * voltageLimitsOverride contains absolute voltage limits.
     * This function compute limits in pair-unit quantities.
     */
    private void transformToNormalizedVoltage(List<VoltageLimitOverride> voltageLimitsOverrides, Network network, ReportNode reportNode) {
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

            if (newLimits.getFirst() >= newLimits.getSecond()) {
                throw new InvalidParametersException(OVERRIDE_ON_VOLTAGE_LEVEL + voltageLevelId + " leads to low voltage limit >= high voltage limit.");
            }
            normalizedVoltageLimitsOverride.put(voltageLevelId, newLimits);
        }

        checkLimitsInNominalVoltageRange(network, reportNode);
    }

    @Override
    public String getFileName() {
        return FILENAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num minV (pu) maxV (pu) id");
        writer.newLine();

        for (Map.Entry<String, Pair<Double, Double>> entry : normalizedVoltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            Pair<Double, Double> limits = entry.getValue();

            if (!Double.isNaN(limits.getFirst()) || !Double.isNaN(limits.getSecond())) {
                int amplId = stringToIntMapper.getInt(AmplSubset.VOLTAGE_LEVEL, voltageLevelId);
                String[] tokens = {Integer.toString(amplId), Double.toString(limits.getFirst()), Double.toString(limits.getSecond()), AmplIOUtils.addQuotes(voltageLevelId)};
                writer.write(String.join(" ", tokens));
                writer.newLine();
            }
        }

        //add new line at the end of the file
        writer.newLine();
        writer.flush();
    }
}
