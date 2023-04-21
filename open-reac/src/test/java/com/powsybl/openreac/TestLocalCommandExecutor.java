/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.powsybl.computation.local.LocalCommandExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TestLocalCommandExecutor implements LocalCommandExecutor {

    private final List<String> outputFileNames;

    public TestLocalCommandExecutor(List<String> outputFileNames) {
        this.outputFileNames = Objects.requireNonNull(outputFileNames);
    }

    @Override
    public int execute(String program, List<String> args, Path outFile, Path errFile, Path workingDir, Map<String, String> env) throws IOException {
        for (String outputFileName : outputFileNames) {
            Files.copy(getClass().getResourceAsStream("/" + outputFileName), workingDir.resolve(outputFileName));
        }
        return 0;
    }

    @Override
    public void stop(Path workingDir) {
        // not supported
    }

    @Override
    public void stopForcibly(Path workingDir) {
        // not supported
    }
}
