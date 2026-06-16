/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads back the transformers that the AMPL model fixed within a parallel bundle, i.e. the
 * members of a bundle whose effective-rho intersection turned out to be a single point or
 * empty (POINT/EMPTY). The bundle qualification lives entirely in the AMPL model, so this
 * file is how the Java side learns which transformers were fixed and at which effective ratio.
 *
 * <p>The file is absent or header-only when no transformer was fixed; a missing file is not
 * an error.
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class FixedParallelTransformersOutput extends AbstractNoThrowOutput {

    private static final String ELEMENT = "fixed_parallel_transformers";
    public static final int EXPECTED_COLS = 5;
    private static final int BUNDLE_COLUMN_INDEX = 1;
    private static final int FIXED_RHO_COLUMN_INDEX = 3;
    private static final int ID_COLUMN_INDEX = 4;

    public static class FixedParallelTransformer {
        private final int bundle;
        private final String transformerId;
        private final double fixedEffectiveRho;

        public FixedParallelTransformer(int bundle, String transformerId, double fixedEffectiveRho) {
            this.bundle = bundle;
            this.transformerId = Objects.requireNonNull(transformerId);
            this.fixedEffectiveRho = fixedEffectiveRho;
        }

        public int getBundle() {
            return bundle;
        }

        public String getTransformerId() {
            return transformerId;
        }

        public double getFixedEffectiveRho() {
            return fixedEffectiveRho;
        }
    }

    private final List<FixedParallelTransformer> fixedTransformers = new ArrayList<>();

    public List<FixedParallelTransformer> getFixedTransformers() {
        return fixedTransformers;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    public int getExpectedColumns() {
        return EXPECTED_COLS;
    }

    @Override
    public boolean throwOnMissingFile() {
        // No transformer was fixed within a bundle in this run.
        return false;
    }

    @Override
    protected void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        int bundle = Integer.parseInt(tokens[BUNDLE_COLUMN_INDEX]);
        double fixedEffectiveRho = readDouble(tokens[FIXED_RHO_COLUMN_INDEX]);
        String transformerId = AmplIOUtils.removeQuotes(tokens[ID_COLUMN_INDEX]);
        fixedTransformers.add(new FixedParallelTransformer(bundle, transformerId, fixedEffectiveRho));
    }
}
