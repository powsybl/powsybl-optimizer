/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class SuspectBranches implements AmplInputFile {

    Map<Integer, String> suspectBranches;

    public SuspectBranches(Map<Integer, String> suspectBranches) {
        this.suspectBranches = suspectBranches;
    }

    @Override
    public String getFileName() {
        return "ampl_suspect_branches.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Format attendu : "num" "branch_susp_id"
        for (var suspectBranch : suspectBranches.entrySet()) {
            String line = suspectBranch.getKey().toString() + " " + suspectBranch.getValue();
            writer.write(line);
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }
}
