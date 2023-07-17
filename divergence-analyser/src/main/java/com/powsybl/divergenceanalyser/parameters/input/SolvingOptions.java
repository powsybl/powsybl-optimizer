package com.powsybl.divergenceanalyser.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class SolvingOptions implements AmplInputFile {
    private static final String fileName = "solving_options.txt";

    HashMap<String, Integer> options;

    public SolvingOptions(HashMap<String, Integer> options){
        this.options = options;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> networkAmplMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        for(String option : options.keySet()){
            dataBuilder.append(option).append(" ").append(options.get(option)).append("\n");
        }
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
