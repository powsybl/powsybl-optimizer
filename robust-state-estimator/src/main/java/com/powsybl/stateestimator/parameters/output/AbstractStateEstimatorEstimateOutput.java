/**
 * Copyright (c) 2022,2023,2024 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public abstract class AbstractStateEstimatorEstimateOutput implements AmplOutputFile {

    static final Predicate<String> COMMENTED_LINE_TEST = Pattern.compile("\\s*#.*").asMatchPredicate();

    @Override
    public boolean throwOnMissingFile() {
        return false;
    }

    abstract void readLine(String[] tokens, StringToIntMapper<AmplSubset> amplMapper);
}
