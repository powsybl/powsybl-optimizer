/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 *
 * List of transformers which tap position can be modified by OpenReac.
 * timestep num bus id
 */
public class VariableTwoWindingsTransformers implements AmplInputFile {
    public static final String PARAM_TRANSFORMER_FILE_NAME = "param_transformers.txt";

    private final List<String> transformers;
    private static final String QUOTE = "'";

    public String addQuotes(String str) {
        return QUOTE + str + QUOTE;
    }

    public VariableTwoWindingsTransformers(List<String> elementIds) {
        this.transformers = elementIds;
    }

    @Override
    public String getFileName() {
        return PARAM_TRANSFORMER_FILE_NAME;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#amplId powsyblId");
        for (String transformerId : transformers) {
            int amplId = stringToIntMapper.getInt(AmplSubset.BRANCH, transformerId);
            String[] tokens = {Integer.toString(amplId), addQuotes(transformerId)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

}
