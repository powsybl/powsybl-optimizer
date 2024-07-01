/**
 * Copyright (c) 2022,2023,2024 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public abstract class AbstractStateEstimatorIndicatorsOutput extends AbstractStateEstimatorEstimateOutput {

    List<Pair<String, String>> indicators = new ArrayList<>();

    @Override
    public void read(BufferedReader bufferedReader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        bufferedReader.readLine(); // consume header
        bufferedReader.lines().forEach(line -> {
            if (!COMMENTED_LINE_TEST.test(line)) {
                String[] tokens = line.split(" ");
                readLine(tokens, stringToIntMapper);
            }
        });
    }

    public List<Pair<String, String>> getIndicators() {
        return indicators;
    }

    @Override
    void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper) {
        if (tokens.length == 2) {
            indicators.add(Pair.of(tokens[0], tokens[1]));
        }
    }
}
