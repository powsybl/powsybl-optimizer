/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.OpenReacModel;
import com.powsybl.openreac.exceptions.IncompatibleModelException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public abstract class AbstractNoThrowOutput implements NoThrowAmplOutput {

    private static final Predicate<String> COMMENTED_LINE_TEST = Pattern.compile("\\s*#.*").asMatchPredicate();

    private boolean errorState = false;

    @Override
    public boolean isErrorState() {
        return errorState;
    }

    protected void triggerErrorState() {
        errorState = true;
    }

    @Override
    public String getFileName() {
        return OpenReacModel.OUTPUT_FILE_PREFIX + "_" + getElement() + "." + OpenReacModel.OUTPUT_FILE_FORMAT.getFileExtension();
    }

    /**
     * @return the name of the element that will be read.
     */
    public abstract String getElement();

    /**
     * @return the number of columns expected in the output file.
     */
    public abstract int getExpectedColumns();

    @Override
    public void read(BufferedReader reader, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        String headers = reader.readLine();
        int readCols = headers.split(OpenReacModel.OUTPUT_FILE_FORMAT.getTokenSeparator()).length;
        if (readCols != getExpectedColumns()) {
            triggerErrorState();
            throw new IncompatibleModelException("Error reading " + getFileName() + ", wrong number of columns. Expected: " + getExpectedColumns() + ", found:" + readCols);
        }
        reader.lines().forEach(line -> {
            if (!COMMENTED_LINE_TEST.test(line)) {
                String[] tokens = line.split(OpenReacModel.OUTPUT_FILE_FORMAT.getTokenSeparator());
                readLine(tokens, stringToIntMapper);
            }
        });
    }

    protected abstract void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper);

    protected double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }
}
