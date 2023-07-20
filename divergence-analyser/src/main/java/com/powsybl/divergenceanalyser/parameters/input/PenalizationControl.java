package com.powsybl.divergenceanalyser.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class PenalizationControl implements AmplInputFile {
    private final String fileName = "penal.txt";

    HashMap<String, Integer> penalization;

    public PenalizationControl(HashMap<String, Integer> penalization) {
        this.penalization = penalization;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> networkAmplMapper) {

        StringBuilder dataBuilder = new StringBuilder();
        for (String penal : penalization.keySet()) {
            dataBuilder.append(penal).append(" ").append(penalization.get(penal)).append("\n");
        }
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}

