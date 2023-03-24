package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VoltageLevelLimitsOverrideInput implements AmplInputFile {

    private static final String FILENAME = "ampl_network_substations_override.txt";
    private final Map<String, Pair<Double, Double>> normalizedVoltageLimitsOverride;

    public VoltageLevelLimitsOverrideInput(Map<String, Pair<Double, Double>> voltageLimitsOverride, Network network) {
        Objects.requireNonNull(voltageLimitsOverride);
        Objects.requireNonNull(network);
        this.normalizedVoltageLimitsOverride = new HashMap<>();
        transformToNormalizedVoltage(voltageLimitsOverride, network);
    }

    /**
     * voltageLimitsOverride contains absolute voltage values.
     * This function compute limits normalized to nominal voltage.
     */
    private void transformToNormalizedVoltage(Map<String, Pair<Double, Double>> voltageLimitsOverride, Network network) {
        for (Map.Entry<String, Pair<Double, Double>> entry : voltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            Pair<Double, Double> limits = entry.getValue();
            double previousLowVoltageLimit = network.getVoltageLevel(voltageLevelId).getLowVoltageLimit();
            double previousHighVoltageLimit = network.getVoltageLevel(voltageLevelId).getHighVoltageLimit();
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            normalizedVoltageLimitsOverride.put(voltageLevelId, Pair.of((previousLowVoltageLimit + limits.getLeft()) / nominalV,
                    (previousHighVoltageLimit + limits.getRight()) / nominalV));
        }
    }

    @Override
    public String getFileName() {
        return FILENAME;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#num minV (pu) maxV (pu) id\n");
        for (Map.Entry<String, Pair<Double, Double>> entry : normalizedVoltageLimitsOverride.entrySet()) {
            String voltageLevelId = entry.getKey();
            Pair<Double, Double> limits = entry.getValue();
            int amplId = stringToIntMapper.getInt(AmplSubset.VOLTAGE_LEVEL, voltageLevelId);
            String[] tokens = {Integer.toString(amplId), Double.toString(limits.getLeft()),
                    Double.toString(limits.getRight()), AmplWriterUtils.addQuotes(voltageLevelId)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
