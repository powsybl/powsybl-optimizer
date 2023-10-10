/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas PIERRE <nicolas.pierre at artelys.com>
 */
class ItoolsTest {
    private FileSystem fileSystem;
    private OpenReacTool tool;
    private ToolRunningContext context;

    @BeforeEach
    public void setUp() throws IOException {
        tool = new OpenReacTool();
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        context = new ToolRunningContext(System.out, System.err, fileSystem,
                new LocalComputationManager(), new LocalComputationManager());
    }

    @Test
    public void testCommandLineBasic() {
        String minimalLine = "--case-file network.iidm";
        assertDoesNotThrow(() -> new DefaultParser().parse(tool.getCommand().getOptions(), minimalLine.split(" ")), "minimal arguments should be input network");

        String specificOpenReacLine = "--case-file network.xiidm --open-reac-params params.txt";
        assertDoesNotThrow(() -> new DefaultParser().parse(tool.getCommand().getOptions(), minimalLine.split(" ")), "OpenReac Tool should handle all those args " + specificOpenReacLine);

    }

    @Test
    public void testCreateOpenReacParameters() throws ParseException, IOException {
        Files.copy(getClass().getResourceAsStream("/openreac-params.json"), fileSystem.getPath("params.json"));
        String line = "--case-file network.iidm --open-reac-params params.json";
        CommandLine cmdLine = new DefaultParser().parse(tool.getCommand().getOptions(), line.split(" "));
        OpenReacParameters loadedParams = tool.createOpenReacParameters(cmdLine, context);
        assertEquals(List.of("2-winding-transfo"), loadedParams.getVariableTwoWindingsTransformers(), "Parsing of OpenReac parameters is wrong.");
        assertEquals(List.of("constant-q-gen"), loadedParams.getConstantQGenerators(), "Parsing of OpenReac parameters is wrong.");
        assertEquals(List.of("var-shunt", "var-shunt-2"), loadedParams.getVariableShuntCompensators(), "Parsing of OpenReac parameters is wrong.");

        // List of voltage limit overrides
        List<VoltageLimitOverride> vloList = new ArrayList<>();
        vloList.add(new VoltageLimitOverride("voltageLevelId", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -5));
        vloList.add(new VoltageLimitOverride("voltageLevelId", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 5));

        for (int i = 0; i < vloList.size(); i++) {
            assertEquals(vloList.get(i), loadedParams.getSpecificVoltageLimits().get(i));
        }
        assertEquals(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE, loadedParams.getObjective(), "Parsing of OpenReac parameters is wrong.");
    }

}
