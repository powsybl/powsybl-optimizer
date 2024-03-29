/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.options;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class SolvingOptions implements AmplInputFile {

    HashMap<String, Integer> options;

    public SolvingOptions(HashMap<String, Integer> options) {
        this.options = options;
    }

    @Override
    public String getFileName() {
        return "solving_options.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        for (var option : options.entrySet()) {
            String[] tokens = {option.getKey(), Integer.toString(option.getValue())};
            writer.write(String.join(" ", tokens));
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }
}
