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
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.IntersectionStatus;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelGroup;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes parallel transformer groups whose rho intersection is LARGE — those
 * that can be tied under a single shared ratio variable in the optimization.
 *
 * <p>POINT and EMPTY groups are written by
 * {@link FixedRatioTwoWindingsTransformers} instead, since their transformers
 * must be fixed rather than tied.
 *
 * <p>Each row carries the group rho intersection bounds, which AMPL uses as
 * the bounds of the single shared rho variable for the group. The bounds are
 * repeated on every row of the same group for read simplicity.
 *
 * <p>Format:
 * <pre>
 * #num_group num_branch group_rho_min group_rho_max
 * 1 142 0.970000 1.030000
 * 1 287 0.970000 1.030000
 * 2 95 0.950000 1.020000
 * 2 96 0.950000 1.020000
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class ParallelTwoWindingsTransformersGroups implements AmplInputFile {

    public static final String PARAM_PARALLEL_TRANSFORMERS_FILE_NAME = "param_parallel_transformers.txt";

    private static final DecimalFormat RHO_FORMAT = new DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final List<TransformerInGroup> rows;

    private record TransformerInGroup(int groupIndex, String transformerId, double groupRhoLow, double groupRhoHigh) { }

    public ParallelTwoWindingsTransformersGroups(List<ParallelGroup> allGroups) {
        this.rows = new ArrayList<>();
        int groupIndex = 1;
        for (ParallelGroup group : allGroups) {
            if (group.status() != IntersectionStatus.LARGE) {
                continue;
            }
            for (String transformerId : group.transformerIds().stream().sorted().toList()) {
                rows.add(new TransformerInGroup(groupIndex, transformerId, group.low(), group.high()));
            }
            groupIndex++;
        }
    }

    @Override
    public String getFileName() {
        return PARAM_PARALLEL_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_group num_branch group_rho_min group_rho_max id");
        writer.newLine();
        for (TransformerInGroup row : rows) {
            int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, row.transformerId());
            writer.write(row.groupIndex() + " " + amplBranchId + " "
                + RHO_FORMAT.format(row.groupRhoLow()) + " " + RHO_FORMAT.format(row.groupRhoHigh())
                + " " + AmplIOUtils.addQuotes(row.transformerId()));
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
    }
}
