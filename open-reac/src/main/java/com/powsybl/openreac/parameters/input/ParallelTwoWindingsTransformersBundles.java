/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Writes the topological membership of the detected parallel transformer bundles:
 * every branch sharing a {@code num_bundle} is parallel to the others of that bundle.
 *
 * <p>This file carries the membership only. The numeric qualification of each bundle —
 * the effective-rho intersection, the LARGE / POINT / EMPTY classification, the shared-ratio
 * bounds and the fixed targets — is derived entirely in the AMPL model from its own data
 * (see {@code commons.mod}), so that the classification and the constraints can never
 * disagree. Bundles are written in the order produced by the detector, and the 1-based
 * {@code num_bundle} index is reused as-is by AMPL.
 *
 * <p>Format:
 * <pre>
 * #num_bundle num_branch id
 * 1 142 "T1"
 * 1 287 "T2"
 * 2 95 "T3"
 * 2 96 "T4"
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class ParallelTwoWindingsTransformersBundles implements AmplInputFile {

    public static final String PARAM_PARALLEL_TRANSFORMERS_FILE_NAME = "param_parallel_transformers.txt";

    private final List<TransformerInBundle> rows;

    private record TransformerInBundle(int bundleIndex, String transformerId) { }

    public ParallelTwoWindingsTransformersBundles(List<Set<String>> bundles) {
        this.rows = new ArrayList<>();
        int bundleIndex = 1;
        for (Set<String> bundle : bundles) {
            for (String transformerId : bundle.stream().sorted().toList()) {
                rows.add(new TransformerInBundle(bundleIndex, transformerId));
            }
            bundleIndex++;
        }
    }

    @Override
    public String getFileName() {
        return PARAM_PARALLEL_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_bundle num_branch id");
        writer.newLine();
        for (TransformerInBundle row : rows) {
            int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, row.transformerId());
            writer.write(row.bundleIndex() + " " + amplBranchId + " " + AmplIOUtils.addQuotes(row.transformerId()));
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
    }
}
