/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input.algo;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public class AlgorithmInput implements AmplInputFile {
    private static final String ALGORITHM_INPUT_FILE = "param_algo.txt";

    private final List<OpenReacAlgoParam> algoParameters;

    public AlgorithmInput(List<OpenReacAlgoParam> algoParameters) {
        this.algoParameters = algoParameters;
    }

    @Override
    public String getFileName() {
        return ALGORITHM_INPUT_FILE;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        for (OpenReacAlgoParam param : algoParameters) {
            writer.append(param.getName())
                    .append(" ")
                    .append(param.getValue());
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }
}
