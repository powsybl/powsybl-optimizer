/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Writes the parallel two-windings transformers groups in a 2D form to be read
 * by AMPL: each line contains the group index and the AMPL branch id of one
 * transformer of that group.
 *
 * <p>Format:
 * <pre>
 * #num_group num_branch
 * 1 142
 * 1 287
 * 2 95
 * 2 96
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class ParallelTwoWindingsTransformersGroups implements AmplInputFile {

    public static final String PARAM_PARALLEL_TRANSFORMERS_FILE_NAME = "param_parallel_transformers.txt";

    private final List<Set<String>> groups;

    public ParallelTwoWindingsTransformersGroups(List<Set<String>> groups) {
        this.groups = groups;
    }

    @Override
    public String getFileName() {
        return PARAM_PARALLEL_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_group num_branch\n");
        int groupIndex = 1;
        for (Set<String> group : groups) {
            for (String transformerId : group.stream().sorted().toList()) {
                int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, transformerId);
                writer.write(groupIndex + " " + amplBranchId);
                writer.newLine();
            }
            groupIndex++;
        }
        writer.newLine();
        writer.flush();
    }
}
