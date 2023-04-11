/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public abstract class AbstractElementsInput implements AmplInputFile {

    private final List<String> elementIds;

    protected AbstractElementsInput(List<String> elementIds) {
        this.elementIds = elementIds;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#amplId powsyblId\n");
        for (String elementID : elementIds) {
            int amplId = stringToIntMapper.getInt(getElementAmplSubset(), elementID);
            String[] tokens = {Integer.toString(amplId), AmplWriterUtils.addQuotes(elementID)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append(System.lineSeparator());
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    abstract AmplSubset getElementAmplSubset();
}
