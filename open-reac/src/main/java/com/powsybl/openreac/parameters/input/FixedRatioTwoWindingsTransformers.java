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
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.RatioTapChangerStep;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the transformers whose rho must be fixed in the optimization,
 * because they belong to a parallel group whose rho intersection is
 * degenerate (POINT or EMPTY).
 *
 * <p>For POINT groups, all transformers are fixed at the unique intersection
 * value (which equals max(rhoMin_i) = min(rhoMax_i) up to epsilon).
 *
 * <p>For EMPTY groups, each transformer is fixed at the bound of its own
 * domain closest to the center of the disjoint gap: at rhoMax_i if the
 * transformer's domain lies below the center, at rhoMin_i if it lies above.
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

    public FixedRatioTwoWindingsTransformers(List<ParallelGroup> allGroups, Network network) {
        this.fixedTransformers = new ArrayList<>();
        for (ParallelGroup group : allGroups) {
            switch (group.status()) {
                case POINT -> addPointGroup(group);
                case EMPTY -> addEmptyGroup(group, network);
                case LARGE -> {
                    // Not fixed — handled by ParallelTwoWindingsTransformersGroups.
                }
            }
        }
    }

    private void addPointGroup(ParallelGroup group) {
        // For POINT: low ≈ high, pick either as the fixed value.
        double fixedRho = group.low();
        for (String twtId : group.transformerIds()) {
            fixedTransformers.add(new FixedTransformer(twtId, fixedRho));
        }
    }

    private void addEmptyGroup(ParallelGroup group, Network network) {
        // For EMPTY: low > high. Center of the gap is (low + high) / 2.
        // Each transformer goes to the bound of its own domain closest to
        // the gap center.
        double center = (group.low() + group.high()) / 2.0;
        for (String twtId : group.transformerIds()) {
            TwoWindingsTransformer twt = network.getTwoWindingsTransformer(twtId);
            if (twt == null || twt.getRatioTapChanger() == null) {
                continue;
            }
            RatioTapChanger rtc = twt.getRatioTapChanger();
            double rhoMin = rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).min().orElse(Double.NaN);
            double rhoMax = rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).max().orElse(Double.NaN);
            // If the whole domain is below the center, snap up to rhoMax.
            // If it's above, snap down to rhoMin.
            // (One of these holds by construction, since the domains are disjoint.)
            double fixedRho = (rhoMax <= center) ? rhoMax : rhoMin;
            fixedTransformers.add(new FixedTransformer(twtId, fixedRho));
        }
    }

    @Override
    public String getFileName() {
        return PARAM_FIXED_RATIO_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_branch fixed_rho\n");
        for (FixedTransformer ft : fixedTransformers) {
            int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, ft.transformerId());
            writer.write(amplBranchId + " " + RHO_FORMAT.format(ft.fixedRho()));
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
    }
}
