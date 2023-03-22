/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplException;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public class IndicatorOutput implements AmplOutputFile {
    private static final String NULL_INDICATOR = "NULL_INDICATOR";
    private static final String INDICATOR_FILE_NAME = "reactiveopf_results_indic.txt";
    private static final Pattern STRING_MAYBE_IN_QUOTES = Pattern.compile("([^']\\S*|'.+?')\\s*");
    private Map<String, String> indicatorMap;

    public IndicatorOutput() {
        this.indicatorMap = new HashMap<>();
    }

    @Override
    public String getFileName() {
        return INDICATOR_FILE_NAME;
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        List<String> indicatorsLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : indicatorsLines) {
            List<String> lineTokens = parseStringOptionalQuotes(line);
            if (lineTokens.size() > 2) {
                throw new AmplException(
                        "Error reading indicators : Expected 2 or less tokens on a line, got " + lineTokens.size());
            } else if (lineTokens.size() == 1) {
                readLine(lineTokens.get(0), NULL_INDICATOR);
            } else if (lineTokens.size() == 2) {
                readLine(lineTokens.get(0), lineTokens.get(1));
            }
        }
    }

    /**
     * Parses token on a line : Removes optional single quotes surounding strings, and trims trailing whitespaces
     *
     * @param line line of the indicator file
     * @return the list of tokens found in the line
     */
    private static List<String> parseStringOptionalQuotes(String line) {
        List<String> tokens = new ArrayList<>();
        Matcher m = STRING_MAYBE_IN_QUOTES.matcher(line);
        while (m.find()) {
            String tok = m.group(1).replace("'", "");
            tokens.add(tok);
        }
        return tokens;
    }

    private void readLine(String key, String value) {
        indicatorMap.put(key, value);
    }

    public Map<String, String> getIndicators() {
        return indicatorMap;
    }
}
