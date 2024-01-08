/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class PenalizationOptions implements AmplInputFile {
    private final String fileName = "penal_options.txt";

    HashMap<String, Integer> penalization;

    public PenalizationOptions(HashMap<String, Integer> penalization) {
        this.penalization = penalization;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void write(BufferedWriter bufferedWriter, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // TODO
        return;
    }

    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> networkAmplMapper) {

        StringBuilder dataBuilder = new StringBuilder();
        for (String penal : penalization.keySet()) {
            dataBuilder.append(penal).append(" ").append(penalization.get(penal)).append("\n");
        }
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}

