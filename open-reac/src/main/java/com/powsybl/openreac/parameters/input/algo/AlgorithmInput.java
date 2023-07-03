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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
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
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        for (OpenReacAlgoParam param : algoParameters) {
            dataBuilder.append(param.getName()).append(" ").append(param.getValue()).append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
