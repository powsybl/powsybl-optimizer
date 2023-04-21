/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.openreac.OpenReacModel;
import com.powsybl.openreac.parameters.output.AbstractNoThrowOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Abstract class that reads output from ampl and generates network modifications
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
public abstract class AbstractNetworkOutput extends AbstractNoThrowOutput {

    private final Predicate<String> COMMENTED_LINE_TEST = Pattern.compile("\\s*#.*").asMatchPredicate();

    private final List<NetworkModification> modifications = new ArrayList<>();

    /**
     * @return the name of the element that will be read.
     */
    public abstract String getElement();

    @Override
    public String getFileName() {
        return OpenReacModel.OUTPUT_FILE_PREFIX + "_" + getElement() + "." + OpenReacModel.OUTPUT_FILE_FORMAT.getFileExtension();
    }

    @Override
    public void read(Path path, StringToIntMapper<AmplSubset> stringToIntMapper) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            triggerErrorState();
        }
        if(lines != null){
            for (String line : lines) {
                if(!COMMENTED_LINE_TEST.test(line)){
                    String[] tokens = line.split(OpenReacModel.OUTPUT_FILE_FORMAT.getTokenSeparator());
                    modifications.add(doReadLine(tokens, stringToIntMapper));
                }
            }
        }
    }

    protected abstract NetworkModification doReadLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper);

    public List<NetworkModification> getModifications() {
        return modifications;
    }
}
