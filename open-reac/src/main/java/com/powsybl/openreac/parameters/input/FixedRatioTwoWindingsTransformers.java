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
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelBundle;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.RhoBounds;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Writes the transformers whose rho must be fixed in the optimization,
 * because they belong to a parallel bundle whose rho intersection is
 * degenerate (POINT or EMPTY).
 *
 * <p>Both POINT and EMPTY bundles are handled the same way: each variable
 * transformer is fixed at the value of its own domain closest to the center
 * of the intersection bounds, i.e. {@code clamp(center, rhoMin_i, rhoMax_i)}.
 * For a POINT bundle the center lies inside every member's range, so all
 * members collapse to the single shared value; for an EMPTY bundle members
 * snap to the bound of their own domain facing the gap. Clamping to each
 * member's own range guarantees the fixed value is always feasible, even when
 * the intersection is degenerate within {@code RHO_INTERSECTION_EPSILON}.
 *
 * <p>Format:
 * <pre>
 * #num_branch fixed_rho
 * 142 1.0000
 * 287 0.9900
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class FixedRatioTwoWindingsTransformers implements AmplInputFile {

    public static final String PARAM_FIXED_RATIO_TRANSFORMERS_FILE_NAME = "param_fixed_ratio_transformers.txt";

    private static final DecimalFormat RHO_FORMAT = new DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final List<FixedTransformer> fixedTransformers;

    private record FixedTransformer(String transformerId, double fixedRho) { }

    FixedRatioTwoWindingsTransformers(List<ParallelBundle> allBundles, Network network) {
        this(allBundles, network, null);
    }

    public FixedRatioTwoWindingsTransformers(List<ParallelBundle> allBundles, Network network, Set<String> variableTransformerIds) {
        this.fixedTransformers = new ArrayList<>();
        for (ParallelBundle bundle : allBundles) {
            switch (bundle.status()) {
                // POINT is the boundary of EMPTY: write both with the same per-member
                // clamp, so the fixed value can never fall outside the member's own
                // [rhoMin, rhoMax] (avoids an infeasible fix when the intersection is
                // degenerate within RHO_INTERSECTION_EPSILON).
                case POINT, EMPTY -> addFixedClampedToCenter(bundle, network, variableTransformerIds);
                case LARGE -> {
                    // Tied — handled by ParallelTwoWindingsTransformersBundles.
                }
            }
        }
    }

    private static boolean isVariable(Set<String> variableTransformerIds, String twtId) {
        return variableTransformerIds == null || variableTransformerIds.contains(twtId);
    }

    private void addFixedClampedToCenter(ParallelBundle bundle, Network network, Set<String> variableTransformerIds) {
        // Degenerate intersection (POINT or EMPTY): fix each variable member as close
        // to the gap center as its own [rhoMin, rhoMax] allows. For a true point the
        // center lies in every member's range, so all members collapse to it.
        // Concretely, clamp(center, rhoMin_i, rhoMax_i):
        //   - center inside the member's domain -> fix at center;
        //   - domain entirely below the center  -> fix at rhoMax_i;
        //   - domain entirely above the center  -> fix at rhoMin_i.
        // (An EMPTY status only needs one pair of disjoint domains; other members
        // may well contain the center.)
        double center = (bundle.low() + bundle.high()) / 2.0;
        for (String twtId : bundle.transformerIds()) {
            if (!isVariable(variableTransformerIds, twtId)) {
                continue; // non-variable: already frozen at its current tap, nothing to write
            }
            RhoBounds bounds = ParallelTwoWindingsTransformersDetector.rhoBounds(network.getTwoWindingsTransformer(twtId));
            if (!bounds.isPresent()) {
                continue;
            }
            double fixedRho = Math.max(bounds.min(), Math.min(bounds.max(), center));
            fixedTransformers.add(new FixedTransformer(twtId, fixedRho));
        }
    }

    @Override
    public String getFileName() {
        return PARAM_FIXED_RATIO_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_branch fixed_rho id");
        writer.newLine();
        for (FixedTransformer ft : fixedTransformers) {
            int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, ft.transformerId());
            writer.write(amplBranchId + " " + RHO_FORMAT.format(ft.fixedRho())
                + " " + AmplIOUtils.addQuotes(ft.transformerId()));
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
    }
}
