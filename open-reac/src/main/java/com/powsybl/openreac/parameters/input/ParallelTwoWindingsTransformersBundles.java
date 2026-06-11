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
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelBundle;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes parallel transformer bundles whose rho intersection is LARGE — those
 * that can be tied under a single shared ratio variable in the optimization.
 *
 * <p>POINT and EMPTY bundles are written by
 * {@link FixedRatioTwoWindingsTransformers} instead, since their transformers
 * must be fixed rather than tied.
 *
 * <p>Each row carries the bundle rho intersection bounds, expressed in
 * <em>effective-ratio</em> space (tap rho scaled by the constant per-unit
 * ratio of each member, see {@code ParallelTwoWindingsTransformersDetector#cstRatio}).
 * AMPL uses them as the bounds of the single shared effective-rho variable for
 * the bundle, and ties each member through
 * {@code branch_Ror_var * branch_cstratio = bundle_Ror_var}. The bounds are
 * repeated on every row of the same bundle for read simplicity.
 *
 * <p>Format:
 * <pre>
 * #num_bundle num_branch bundle_rho_min bundle_rho_max
 * 1 142 0.970000 1.030000
 * 1 287 0.970000 1.030000
 * 2 95 0.950000 1.020000
 * 2 96 0.950000 1.020000
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class ParallelTwoWindingsTransformersBundles implements AmplInputFile {

    public static final String PARAM_PARALLEL_TRANSFORMERS_FILE_NAME = "param_parallel_transformers.txt";

    private static final DecimalFormat RHO_FORMAT = new DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final List<TransformerInBundle> rows;

    private record TransformerInBundle(int bundleIndex, String transformerId, double bundleRhoLow, double bundleRhoHigh) { }

    public ParallelTwoWindingsTransformersBundles(List<ParallelBundle> allBundles) {
        this.rows = new ArrayList<>();
        int bundleIndex = 1;
        for (ParallelBundle bundle : allBundles) {
            if (bundle.status() != IntersectionStatus.LARGE) {
                continue;
            }
            for (String transformerId : bundle.transformerIds().stream().sorted().toList()) {
                rows.add(new TransformerInBundle(bundleIndex, transformerId, bundle.low(), bundle.high()));
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
        writer.write("#num_bundle num_branch bundle_rho_min bundle_rho_max id");
        writer.newLine();
        for (TransformerInBundle row : rows) {
            int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, row.transformerId());
            writer.write(row.bundleIndex() + " " + amplBranchId + " "
                + RHO_FORMAT.format(row.bundleRhoLow()) + " " + RHO_FORMAT.format(row.bundleRhoHigh())
                + " " + AmplIOUtils.addQuotes(row.transformerId()));
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
    }
}
