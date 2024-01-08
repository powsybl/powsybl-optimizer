/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class PenalizationIndicatorsOutput implements AmplOutputFile {

    List<Pair<String, String>> penalizationIndicators = new ArrayList<>();

    @Override
    public String getFileName() {
        return "da_penal_indic.txt";
    }

    @Override
    public boolean throwOnMissingFile() {
        return false;
    }

    @Override
    public void read(BufferedReader bufferedReader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // TODO
        return;
    }

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
