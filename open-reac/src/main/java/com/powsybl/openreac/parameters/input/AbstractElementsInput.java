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
import com.powsybl.openreac.parameters.AmplIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
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
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num id\n");
        for (String elementID : elementIds) {
            int amplId = stringToIntMapper.getInt(getElementAmplSubset(), elementID);
            String[] tokens = {Integer.toString(amplId), AmplIOUtils.addQuotes(elementID)};
            writer.write(String.join(" ", tokens));
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }

    abstract AmplSubset getElementAmplSubset();
}
