package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkIndicatorsOutput implements AmplOutputFile {

    Map<String, String> networkIndicators = new HashMap<>();

    @Override
    public String getFileName() {
        return "da_network_indic.txt";
    }

    @Override
    public void read(Path outputPath, StringToIntMapper<AmplSubset> networkAmplMapper) throws IOException {
        List<String> outputLines;

        if (Files.isRegularFile(outputPath)) {
            try {
                outputLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // File reading went wrong
                return;
            }

            // We must skip the first line of the file
            for (String line : outputLines.subList(1, outputLines.size())) {
                String[] tokens = line.split(" ");

                if (tokens.length != 2) {
                    throw new IOException();
                }

                networkIndicators.put(tokens[0], tokens[1]);
            }
        }
    }

    public Map<String, String> getNetworkIndicators() {
        return networkIndicators;
    }
}
