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
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class PenalizationOptions implements AmplInputFile {

    HashMap<String, Integer> penalization;

    public PenalizationOptions(HashMap<String, Integer> penalization) {
        this.penalization = penalization;
    }

    @Override
    public String getFileName() {
        return "penal_options.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        for (var penal : penalization.entrySet()) {
            String[] tokens = {penal.getKey(), Integer.toString(penal.getValue())};
            writer.write(String.join(" ", tokens));
            writer.newLine();
        }

        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }
}

