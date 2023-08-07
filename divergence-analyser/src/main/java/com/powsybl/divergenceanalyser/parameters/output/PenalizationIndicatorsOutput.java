package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PenalizationIndicatorsOutput implements AmplOutputFile {

    List<Pair<String, String>> penalizationIndicators = new ArrayList<>();

    @Override
    public String getFileName() {
        return "da_penal_indic.txt";
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

                penalizationIndicators.add(Pair.of(tokens[0], tokens[1]));
            }
        }
    }

    public List<Pair<String, String>> getPenalizationIndicators() {
        return penalizationIndicators;
    }

}
